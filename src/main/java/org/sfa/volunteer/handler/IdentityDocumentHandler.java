
package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetIdentityDocumentRequest;
import org.sfa.volunteer.dto.request.UploadIdentityDocumentRequest;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.exception.IdentityDocumentException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.IdentityDocumentStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.Cors;
import org.sfa.volunteer.util.IdentityDocumentAuthorizer;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class IdentityDocumentHandler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String ALLOWED_METHODS = "POST,OPTIONS";

    private static final String BASE_PATH =
            "/0.0.1/volunteers/identity-document";

    // Conservative JSON body limit, not a guarantee that
    // Lambda will accept the full API Gateway event payload.
    private static final int MAX_BODY_BYTES = 5_000_000;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
            );

    private static volatile ApplicationContext springContext;

    private final UserService userService;
    private final VolunteerService volunteerService;
    private final IdentityDocumentStorageService storage;
    private final ResponseBuilder responseBuilder;
    private final MessageSourceUtil messages;
    private final Validator validator;

    public IdentityDocumentHandler() {
        this(
                bean(UserService.class),
                bean(VolunteerService.class),
                bean(IdentityDocumentStorageService.class),
                bean(ResponseBuilder.class),
                bean(MessageSourceUtil.class),
                bean(Validator.class)
        );
    }

    IdentityDocumentHandler(
            UserService userService,
            VolunteerService volunteerService,
            IdentityDocumentStorageService storage,
            ResponseBuilder responseBuilder,
            MessageSourceUtil messages,
            Validator validator) {

        this.userService = userService;
        this.volunteerService = volunteerService;
        this.storage = storage;
        this.responseBuilder = responseBuilder;
        this.messages = messages;
        this.validator = validator;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        APIGatewayProxyResponseEvent response =
                new APIGatewayProxyResponseEvent()
                        .withHeaders(Cors.headers(ALLOWED_METHODS));

        if (event == null) {
            return error(response, 400,
                    SaayamStatusCode.BAD_REQUEST,
                    "Request is required");
        }

        String method = event.getHttpMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return response.withStatusCode(204);
        }

        if (!"POST".equalsIgnoreCase(method)) {
            return error(response, 405,
                    SaayamStatusCode.BAD_REQUEST,
                    "Method not allowed");
        }

        try {
            String path = event.getPath();

            if (path == null || !Set.of(
                    BASE_PATH + "/metadata",
                    BASE_PATH + "/upload",
                    BASE_PATH + "/file"
            ).contains(path)) {
                return error(response, 404,
                        SaayamStatusCode.BAD_REQUEST,
                        "Unknown route");
            }

            // Require API Gateway authorizer context.
            // Never fall back to decoding an unverified JWT.
            Caller caller = verifiedCaller(event);

            String body = readBody(event);

            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException(
                        "Request body is required");
            }

            if (body.getBytes(StandardCharsets.UTF_8).length
                    > MAX_BODY_BYTES) {
                return error(response, 413,
                        SaayamStatusCode.DOCUMENT_SIZE_EXCEEDED,
                        "Request too large");
            }

            return switch (path) {
                case BASE_PATH + "/metadata" ->
                        metadata(response, body, caller);

                case BASE_PATH + "/upload" ->
                        upload(response, body, caller);

                case BASE_PATH + "/file" ->
                        file(response, body, caller);

                default -> error(response, 404,
                        SaayamStatusCode.BAD_REQUEST,
                        "Unknown route");
            };

        } catch (ForbiddenException e) {
            return error(response, 403,
                    SaayamStatusCode.FORBIDDEN,
                    message(SaayamStatusCode.FORBIDDEN));

        } catch (IdentityDocumentException e) {
            return error(response,
                    e.getHttpStatus().value(),
                    e.getStatusCode(),
                    message(e.getStatusCode()));

        } catch (UserNotFoundException e) {
            return error(response, 404,
                    SaayamStatusCode.USER_NOT_FOUND,
                    message(SaayamStatusCode.USER_NOT_FOUND,
                            new Object[]{e.getUserId()}));

        } catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();
            SaayamStatusCode code =
                    status >= 500
                            ? SaayamStatusCode.INTERNAL_SERVER_ERROR
                            : SaayamStatusCode.BAD_REQUEST;

            return error(response, status, code,
                    status >= 500
                            ? "Internal server error"
                            : e.getReason());

        } catch (JsonProcessingException e) {
            return error(response, 400,
                    SaayamStatusCode.BAD_REQUEST,
                    "Invalid request body");

        } catch (IllegalArgumentException e) {
            return error(response, 400,
                    SaayamStatusCode.BAD_REQUEST,
                    e.getMessage());

        } catch (Exception e) {
            // Avoid logging identity document contents or JWTs.
            log.error("Identity document request failed",
                    e);

            return error(response, 500,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR,
                    message(SaayamStatusCode.INTERNAL_SERVER_ERROR));
        }
    }

    private APIGatewayProxyResponseEvent metadata(
            APIGatewayProxyResponseEvent response,
            String body,
            Caller caller) throws Exception {

        GetIdentityDocumentRequest request =
                parseAndValidate(body,
                        GetIdentityDocumentRequest.class);

        authorize(request.userId(), caller);

        IdentityDocumentMetadata result =
                volunteerService.getIdentityDocumentMetadata(
                        request.userId(),
                        request.documentSlot()
                );

        if (result == null) {
            return response.withStatusCode(204);
        }

        return success(response, 200,
                responseBuilder.buildSuccessResponse(
                        SaayamStatusCode.SUCCESS,
                        result
                ));
    }

    private APIGatewayProxyResponseEvent upload(
            APIGatewayProxyResponseEvent response,
            String body,
            Caller caller) throws Exception {

        UploadIdentityDocumentRequest request =
                parseAndValidate(body,
                        UploadIdentityDocumentRequest.class);

        authorize(request.userId(), caller);

        Map<String, Object> result =
                storage.uploadBase64(
                        request.userId(),
                        request.documentSlot(),
                        request.documentName(),
                        request.base64(),
                        request.expiresOn(),
                        request.regionHint()
                );

        return success(response, 200,
                responseBuilder.buildSuccessResponse(
                        SaayamStatusCode.IDENTITY_DOCUMENT_UPLOADED,
                        result
                ));
    }

    private APIGatewayProxyResponseEvent file(
            APIGatewayProxyResponseEvent response,
            String body,
            Caller caller) throws Exception {

        GetIdentityDocumentRequest request =
                parseAndValidate(body,
                        GetIdentityDocumentRequest.class);

        authorize(request.userId(), caller);

        Optional<String> url =
                storage.presignedUrl(
                        request.userId(),
                        request.documentSlot(),
                        null
                );

        if (url.isEmpty()) {
            return response.withStatusCode(204);
        }

        return success(response, 200,
                responseBuilder.buildSuccessResponse(
                        SaayamStatusCode.SUCCESS,
                        Map.of("url", url.get())
                ));
    }

    private void authorize(String userId, Caller caller) {
        IdentityDocumentAuthorizer.requireOwnerOrAdmin(
                userService,
                userId,
                caller.email(),
                caller.groups()
        );
    }

    /*
     * Only uses claims supplied by an API Gateway authorizer.
     * Deployment must configure a Cognito/JWT authorizer
     * that verifies signatures, issuer, audience and expiry.
     *
     * This method expects REST API proxy-style authorizer
     * claims. Other Gateway event formats require adaptation.
     */
    private static Caller verifiedCaller(
            APIGatewayProxyRequestEvent event) {

        Map<String, Object> authorizer =
                event.getRequestContext() == null
                        ? null
                        : event.getRequestContext().getAuthorizer();

        if (authorizer == null) {
            throw new ForbiddenException(
                    "Verified authorizer context missing");
        }

        Object claimsObject = authorizer.get("claims");

        if (!(claimsObject instanceof Map<?, ?> claims)) {
            throw new ForbiddenException(
                    "Verified claims missing");
        }

        Object emailObject = claims.get("email");

        if (!(emailObject instanceof String email)
                || email.isBlank()) {
            throw new ForbiddenException(
                    "Caller email missing");
        }

        Object groupsObject = claims.get("cognito:groups");
        String groups = "";

        if (groupsObject instanceof String groupString) {
            groups = groupString;
        } else if (groupsObject instanceof Collection<?> collection) {
            groups = collection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        return new Caller(email.trim(), groups);
    }

    private record Caller(String email, String groups) {}

    private <T> T parseAndValidate(
            String body,
            Class<T> type) throws JsonProcessingException {

        T request = MAPPER.readValue(body, type);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body must contain a JSON object");
        }

        Set<ConstraintViolation<T>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v ->
                            v.getPropertyPath() + ": " +
                            v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));

            throw new IllegalArgumentException(details);
        }

        return request;
    }

    private static String readBody(
            APIGatewayProxyRequestEvent event) {

        String body = event.getBody();

        if (body != null
                && Boolean.TRUE.equals(event.getIsBase64Encoded())) {

            body = new String(
                    Base64.getDecoder().decode(body),
                    StandardCharsets.UTF_8
            );
        }

        return body;
    }

    private APIGatewayProxyResponseEvent success(
            APIGatewayProxyResponseEvent response,
            int status,
            Object data) throws JsonProcessingException {

        return response.withStatusCode(status)
                .withBody(MAPPER.writeValueAsString(data));
    }

    private APIGatewayProxyResponseEvent error(
            APIGatewayProxyResponseEvent response,
            int status,
            SaayamStatusCode code,
            String message) {

        try {
            return response.withStatusCode(status)
                    .withBody(MAPPER.writeValueAsString(
                            responseBuilder.buildErrorResponse(
                                    status, code, message)
                    ));

        } catch (JsonProcessingException e) {
            return response.withStatusCode(status)
                    .withBody(
                            "{\"message\":\"Failed to serialize response\"}"
                    );
        }
    }

    private String message(SaayamStatusCode code) {
        return message(code, null);
    }

    private String message(
            SaayamStatusCode code,
            Object[] args) {

        return messages.getMessage(code.getCode(), args);
    }

    private static <T> T bean(Class<T> type) {
        ApplicationContext ctx = springContext;

        if (ctx == null) {
            synchronized (IdentityDocumentHandler.class) {
                if (springContext == null) {
                    springContext =
                            SpringApplication.run(
                                    VolunteerApplication.class);
                }
                ctx = springContext;
            }
        }

        return ctx.getBean(type);
    }
}
