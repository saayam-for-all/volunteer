package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.Cors;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.OnboardingAvailabilityApi;
import org.sfa.volunteer.util.OnboardingAvailabilityPayload;
import org.sfa.volunteer.util.ResponseBuilder;

public class UpdateVolunteerHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String STEP_FOUR_METHODS = "PUT,OPTIONS";
    private static final ObjectMapper LEGACY_JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final VolunteerService volunteerService;
    private final ResponseBuilder responseBuilder;
    private final MessageSourceUtil messageSourceUtil;
    private final OnboardingAvailabilityApi onboarding;

    public UpdateVolunteerHandler() {
        volunteerService = VolunteerLambdaContext.bean(VolunteerService.class);
        responseBuilder = VolunteerLambdaContext.bean(ResponseBuilder.class);
        messageSourceUtil = VolunteerLambdaContext.bean(MessageSourceUtil.class);
        onboarding = VolunteerLambdaContext.bean(OnboardingAvailabilityApi.class);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        if ("OPTIONS".equalsIgnoreCase(requestEvent.getHttpMethod())) {
            return new APIGatewayProxyResponseEvent().withStatusCode(204).withHeaders(Cors.headers(STEP_FOUR_METHODS));
        }
        JsonNode body;
        try {
            body = OnboardingAvailabilityPayload.object(OnboardingAvailabilityHandler.readBody(requestEvent));
        } catch (OnboardingAvailabilityException failure) {
            return OnboardingAvailabilityHandler.respond(failure, STEP_FOUR_METHODS);
        }
        if (OnboardingAvailabilityPayload.targetsStepFour(body)) {
            if (!"PUT".equalsIgnoreCase(requestEvent.getHttpMethod())) {
                return OnboardingAvailabilityHandler.methodNotAllowed(STEP_FOUR_METHODS);
            }
            var result = onboarding.save(body, OnboardingAvailabilityHandler.caller(requestEvent));
            return OnboardingAvailabilityHandler.respond(result, STEP_FOUR_METHODS);
        }
        return updateEarlierStep(body);
    }

    private APIGatewayProxyResponseEvent updateEarlierStep(JsonNode body) {
        try {
            var request = LEGACY_JSON.treeToValue(body, VolunteerRequest.class);
            if (Integer.valueOf(4).equals(request.step())) {
                return OnboardingAvailabilityHandler.respond(OnboardingAvailabilityApi.error(400, "BAD_REQUEST",
                        "step must be an integer for onboarding availability"), STEP_FOUR_METHODS);
            }
            var updated = volunteerService.updateVolunteer(request);
            var envelope = responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED,
                    new Object[] { updated.userId() }, updated);
            return legacyResponse(201, envelope);
        } catch (Exception failure) {
            SaayamStatusCode code = legacyErrorCode(failure.getMessage());
            String message = messageSourceUtil.getMessage(code.getCode(), null);
            return legacyResponse(500, responseBuilder.buildErrorResponse(500, code, message));
        }
    }

    private static SaayamStatusCode legacyErrorCode(String message) {
        for (SaayamStatusCode code : SaayamStatusCode.values()) {
            if (code.name().equals(message)) return code;
        }
        return SaayamStatusCode.INTERNAL_SERVER_ERROR;
    }

    private static APIGatewayProxyResponseEvent legacyResponse(int status, SaayamResponse<?> envelope) {
        try {
            return new APIGatewayProxyResponseEvent().withStatusCode(status)
                    .withBody(LEGACY_JSON.writeValueAsString(envelope));
        } catch (JsonProcessingException failure) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500)
                    .withBody("{\"message\":\"Failed to serialize error response\"}");
        }
    }
}
