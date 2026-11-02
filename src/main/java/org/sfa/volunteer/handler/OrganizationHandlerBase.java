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
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.VolunteerApplication;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.exception.OrganizationNotFoundException;
import org.sfa.volunteer.exception.UserOrganizationNotFoundException;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.Cors;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
public abstract class OrganizationHandlerBase
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String ORG_ID_PARAM = "orgId";
    protected static final String USER_ID_PARAM = "userId";
    protected static final Object[] NO_ARGS = new Object[0];

    private static final String OPTIONS_METHOD = "OPTIONS";
    private static final String OPTIONS_SUFFIX = ",OPTIONS";
    private static final String BODY_REQUIRED_MESSAGE = "Request body is required";
    private static final String INVALID_BODY_MESSAGE = "Invalid request body";
    private static final String REQUIRED_SUFFIX = " is required";
    private static final String VIOLATION_SEPARATOR = "; ";
    private static final String PROPERTY_SEPARATOR = " ";
    private static final String SERIALIZATION_FAILURE_BODY = "{\"message\":\"Failed to serialize error response\"}";

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static volatile ApplicationContext springContext;

    protected final OrganizationService organizationService;
    protected final ResponseBuilder responseBuilder;
    protected final MessageSourceUtil messageSourceUtil;
    private final String allowedMethods;

    protected record Result(int status, Object body) { }

    protected OrganizationHandlerBase(String httpMethod) {
        this(httpMethod, bean(OrganizationService.class), bean(ResponseBuilder.class),
                bean(MessageSourceUtil.class));
    }

    protected OrganizationHandlerBase(String httpMethod, OrganizationService organizationService,
                                      ResponseBuilder responseBuilder, MessageSourceUtil messageSourceUtil) {
        this.allowedMethods = httpMethod + OPTIONS_SUFFIX;
        this.organizationService = organizationService;
        this.responseBuilder = responseBuilder;
        this.messageSourceUtil = messageSourceUtil;
    }

    protected abstract Result handle(APIGatewayProxyRequestEvent event) throws JsonProcessingException;

    @Override
    public final APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(Cors.headers(allowedMethods));

        if (OPTIONS_METHOD.equalsIgnoreCase(event.getHttpMethod())) {
            return response.withStatusCode(HttpStatus.NO_CONTENT.value());
        }

        try {
            Result result = handle(event);
            return response.withStatusCode(result.status()).withBody(serialize(result.body()));
        } catch (OrganizationNotFoundException e) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.ORGANIZATION_NOT_FOUND.getCode(), new Object[]{ e.getOrgId() });
            return error(response, HttpStatus.NOT_FOUND, SaayamStatusCode.ORGANIZATION_NOT_FOUND, msg);
        } catch (UserOrganizationNotFoundException e) {
            String msg = messageSourceUtil.getMessage(
                    SaayamStatusCode.ORGANIZATION_NOT_FOUND.getCode(), new Object[]{ e.getUserId() });
            return error(response, HttpStatus.NOT_FOUND, SaayamStatusCode.ORGANIZATION_NOT_FOUND, msg);
        } catch (IllegalArgumentException e) {
            return error(response, HttpStatus.BAD_REQUEST, SaayamStatusCode.BAD_REQUEST, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("Invalid organization request body: {}", e.getOriginalMessage());
            return error(response, HttpStatus.BAD_REQUEST, SaayamStatusCode.BAD_REQUEST, INVALID_BODY_MESSAGE);
        } catch (Exception e) {
            log.error("Organization request failed", e);
            String msg = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
            return error(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    SaayamStatusCode.INTERNAL_SERVER_ERROR, msg);
        }
    }

    protected <T> Result ok(SaayamStatusCode code, Object[] args, T data) {
        return new Result(HttpStatus.OK.value(), responseBuilder.buildSuccessResponse(code, args, data));
    }

    protected <T> Result created(SaayamStatusCode code, Object[] args, T data) {
        return new Result(HttpStatus.CREATED.value(), responseBuilder.buildSuccessResponse(code, args, data));
    }

    protected static String requirePathParam(APIGatewayProxyRequestEvent event, String name) {
        return requireValue(event.getPathParameters(), name);
    }

    protected static String requireQueryParam(APIGatewayProxyRequestEvent event, String name) {
        return requireValue(event.getQueryStringParameters(), name);
    }

    protected static <T> T readBody(APIGatewayProxyRequestEvent event, Class<T> type)
            throws JsonProcessingException {
        String body = event.getBody();
        if (body != null && Boolean.TRUE.equals(event.getIsBase64Encoded())) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(BODY_REQUIRED_MESSAGE);
        }
        T request = OBJECT_MAPPER.readValue(body, type);
        validate(request);
        return request;
    }

    private static String requireValue(Map<String, String> params, String name) {
        String value = params == null ? null : params.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + REQUIRED_SUFFIX);
        }
        return value;
    }

    private static <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(v -> v.getPropertyPath() + PROPERTY_SEPARATOR + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(VIOLATION_SEPARATOR)));
        }
    }

    private static String serialize(Object body) {
        try {
            return OBJECT_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private APIGatewayProxyResponseEvent error(APIGatewayProxyResponseEvent response, HttpStatus status,
                                               SaayamStatusCode code, String message) {
        SaayamResponse<Void> errorResponse = responseBuilder.buildErrorResponse(status.value(), code, message);
        try {
            return response.withStatusCode(status.value()).withBody(OBJECT_MAPPER.writeValueAsString(errorResponse));
        } catch (JsonProcessingException e) {
            return response.withStatusCode(status.value()).withBody(SERIALIZATION_FAILURE_BODY);
        }
    }

    private static <T> T bean(Class<T> type) {
        ApplicationContext ctx = springContext;
        if (ctx == null) {
            synchronized (OrganizationHandlerBase.class) {
                if (springContext == null) {
                    springContext = SpringApplication.run(VolunteerApplication.class);
                }
                ctx = springContext;
            }
        }
        return ctx.getBean(type);
    }
}