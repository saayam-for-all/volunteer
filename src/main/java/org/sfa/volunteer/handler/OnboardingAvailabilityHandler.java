package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.util.Cors;
import org.sfa.volunteer.util.OnboardingAvailabilityApi;
import org.sfa.volunteer.util.OnboardingAvailabilityAuthorization.Caller;

import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OnboardingAvailabilityHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String ALLOWED_METHODS = "POST,PUT,OPTIONS";
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final OnboardingAvailabilityApi api;

    public OnboardingAvailabilityHandler() {
        api = VolunteerLambdaContext.bean(OnboardingAvailabilityApi.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = request.getHttpMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return new APIGatewayProxyResponseEvent().withStatusCode(204).withHeaders(Cors.headers(ALLOWED_METHODS));
        }
        boolean read = "POST".equalsIgnoreCase(method);
        if (!read && !"PUT".equalsIgnoreCase(method)) {
            return methodNotAllowed(ALLOWED_METHODS);
        }
        try {
            String body = readBody(request);
            Caller caller = caller(request);
            return respond(read ? api.read(body, caller) : api.save(body, caller), ALLOWED_METHODS);
        } catch (OnboardingAvailabilityException failure) {
            return respond(failure, ALLOWED_METHODS);
        }
    }

    static String readBody(APIGatewayProxyRequestEvent request) {
        String body = request.getBody();
        if (body == null || !Boolean.TRUE.equals(request.getIsBase64Encoded())) return body;
        try {
            return new String(Base64.getDecoder().decode(body), UTF_8);
        } catch (IllegalArgumentException failure) {
            throw new OnboardingAvailabilityException(400, "BAD_REQUEST", "Body must be valid base64");
        }
    }

    static Caller caller(APIGatewayProxyRequestEvent request) {
        Map<String, Object> authorizer = request.getRequestContext() == null ? null
                : request.getRequestContext().getAuthorizer();
        if (authorizer == null || !(authorizer.get("claims") instanceof Map<?, ?> claims)) return null;
        String subject = claims.get("sub") instanceof String value ? value : null;
        String email = claims.get("email") instanceof String value ? value : null;
        Object verified = claims.get("email_verified");
        return new Caller(subject, email, Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(
                verified instanceof String value ? value : null));
    }

    static APIGatewayProxyResponseEvent methodNotAllowed(String allowedMethods) {
        var response = respond(OnboardingAvailabilityApi.error(405, "METHOD_NOT_ALLOWED", "Method not allowed"),
                allowedMethods);
        response.getHeaders().put("Allow", allowedMethods);
        return response;
    }

    static APIGatewayProxyResponseEvent respond(OnboardingAvailabilityException failure, String allowedMethods) {
        return respond(OnboardingAvailabilityApi.error(failure.getHttpStatus(), failure.getErrorCode(), failure.getMessage()),
                allowedMethods);
    }

    static APIGatewayProxyResponseEvent respond(SaayamResponse<?> envelope, String allowedMethods) {
        var response = new APIGatewayProxyResponseEvent().withHeaders(Cors.headers(allowedMethods));
        try {
            return response.withStatusCode(envelope.statusCode()).withBody(JSON.writeValueAsString(envelope));
        } catch (JsonProcessingException failure) {
            return response.withStatusCode(500).withBody("""
                    {"success":false,"statusCode":500,"saayamCode":"INTERNAL_SERVER_ERROR",
                    "message":"Unable to serialize onboarding availability response"}
                    """);
        }
    }
}
