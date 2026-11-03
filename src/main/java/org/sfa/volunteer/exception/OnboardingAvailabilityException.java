package org.sfa.volunteer.exception;

/** A sanitized onboarding failure that adapters map to the same status and error code. */
public class OnboardingAvailabilityException extends RuntimeException {
    private final int httpStatus;
    private final String errorCode;

    public OnboardingAvailabilityException(int httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
