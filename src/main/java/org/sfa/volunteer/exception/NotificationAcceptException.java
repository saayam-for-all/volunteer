package org.sfa.volunteer.exception;

import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.http.HttpStatus;

public class NotificationAcceptException extends NotificationException {
    private final SaayamStatusCode statusCode;
    private final HttpStatus httpStatus;
    private final Object detail;

    public NotificationAcceptException(SaayamStatusCode statusCode, HttpStatus httpStatus,
                                       String userId, Object detail) {
        super(statusCode.toString(), userId);
        this.statusCode = statusCode;
        this.httpStatus = httpStatus;
        this.detail = detail;
    }

    public SaayamStatusCode getStatusCode() { return statusCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public Object getDetail() { return detail; }
}
