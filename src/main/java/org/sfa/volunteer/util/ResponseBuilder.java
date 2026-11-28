package org.sfa.volunteer.util;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import java.time.ZonedDateTime;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ResponseBuilder {

    private final MessageSourceUtil messageSourceUtil;

    @Autowired
    public ResponseBuilder(MessageSourceUtil messageSourceUtil) {
        this.messageSourceUtil = messageSourceUtil;
    }

    private <T> SaayamResponse<T> createSuccessResponse(SaayamStatusCode statusCode, T data, Object[] args) {
        String message = messageSourceUtil.getMessage(statusCode.getCode(), args);
        return SaayamResponse.success(statusCode, message, data);
    }

    public <T> SaayamResponse<T> buildSuccessResponse(SaayamStatusCode statusCode, T data) {
        return createSuccessResponse(statusCode, data, null);
    }

    public <T> SaayamResponse<T> buildSuccessResponse(SaayamStatusCode statusCode, Object[] args, T data) {
        return createSuccessResponse(statusCode, data, args);
    }

    public <T> SaayamResponse<T> buildErrorResponse(int httpStatusCode, SaayamStatusCode statusCode, String message) {
        return SaayamResponse.error(httpStatusCode, statusCode, message);
    }

    public <T> SaayamResponse<T> buildErrorResponse(
            int httpStatusCode, SaayamStatusCode statusCode, Object[] args, T data) {
        return SaayamResponse.<T>builder()
                .success(false).statusCode(httpStatusCode).saayamCode(statusCode.getCode())
                .message(messageSourceUtil.getMessage(statusCode.getCode(), args))
                .data(data).timestamp(java.time.ZonedDateTime.now()).build();
    }

    public static <T> SaayamResponse<T> availabilityResponse(Supplier<SaayamResponse<T>> action) {
        try {
            return action.get();
        } catch (OnboardingAvailabilityException failure) {
            return availabilityError(failure.getHttpStatus(), failure.getErrorCode(), failure.getMessage());
        } catch (RuntimeException failure) {
            log.error("Onboarding availability request failed ({})", failure.getClass().getSimpleName());
            return availabilityError(500, "INTERNAL_SERVER_ERROR", "Unable to process onboarding availability");
        }
    }

    public static <T> SaayamResponse<T> availabilityError(int status, String code, String message) {
        return SaayamResponse.<T>builder().statusCode(status).saayamCode(code).message(message)
                .timestamp(ZonedDateTime.now()).build();
    }
}
