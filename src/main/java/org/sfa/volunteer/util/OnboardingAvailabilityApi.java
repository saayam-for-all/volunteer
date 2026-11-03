package org.sfa.volunteer.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.response.OnboardingAvailabilityResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.service.OnboardingAvailabilityService;
import org.sfa.volunteer.util.OnboardingAvailabilityAuthorization.Caller;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

/** Shared request handling for the two onboarding availability transports. */
@Component
@Slf4j
public class OnboardingAvailabilityApi {
    private final OnboardingAvailabilityService service;
    private final OnboardingAvailabilityAuthorization authorization;
    private final ResponseBuilder responses;

    public OnboardingAvailabilityApi(OnboardingAvailabilityService service,
            OnboardingAvailabilityAuthorization authorization, ResponseBuilder responses) {
        this.service = service;
        this.authorization = authorization;
        this.responses = responses;
    }

    public SaayamResponse<OnboardingAvailabilityResponse> read(String rawBody, Caller caller) {
        return execute(() -> {
            var body = OnboardingAvailabilityPayload.read(OnboardingAvailabilityPayload.object(rawBody));
            authorization.requireOwner(body.userId(), caller);
            return responses.buildSuccessResponse(SaayamStatusCode.SUCCESS, service.read(body.userId()));
        });
    }

    public SaayamResponse<OnboardingAvailabilityResponse> save(String rawBody, Caller caller) {
        return execute(() -> saveRequest(OnboardingAvailabilityPayload.object(rawBody), caller));
    }

    public SaayamResponse<OnboardingAvailabilityResponse> save(JsonNode body, Caller caller) {
        return execute(() -> saveRequest(body, caller));
    }

    private SaayamResponse<OnboardingAvailabilityResponse> saveRequest(JsonNode payload, Caller caller) {
        OnboardingAvailabilitySaveRequest body = OnboardingAvailabilityPayload.save(payload);
        authorization.requireOwner(body.userId(), caller);
        return responses.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, service.save(body));
    }

    public static <T> SaayamResponse<T> execute(Supplier<SaayamResponse<T>> action) {
        try {
            return action.get();
        } catch (OnboardingAvailabilityException failure) {
            return error(failure.getHttpStatus(), failure.getErrorCode(), failure.getMessage());
        } catch (RuntimeException failure) {
            log.error("Onboarding availability request failed ({})", failure.getClass().getSimpleName());
            return error(500, "INTERNAL_SERVER_ERROR", "Unable to process onboarding availability");
        }
    }

    public static <T> SaayamResponse<T> error(int status, String code, String message) {
        return SaayamResponse.<T>builder()
                .statusCode(status)
                .saayamCode(code)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }
}
