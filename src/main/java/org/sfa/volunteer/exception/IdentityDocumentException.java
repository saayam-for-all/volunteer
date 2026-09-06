package org.sfa.volunteer.exception;

import lombok.Getter;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.http.HttpStatus;

@Getter
public class IdentityDocumentException extends RuntimeException {

    private final SaayamStatusCode statusCode;
    private final HttpStatus httpStatus;
    private final String reason;

    public IdentityDocumentException(SaayamStatusCode statusCode, HttpStatus httpStatus, String reason) {
        super(reason);
        this.statusCode = statusCode;
        this.httpStatus = httpStatus;
        this.reason = reason;
    }

    public IdentityDocumentException(SaayamStatusCode statusCode, HttpStatus httpStatus,
                                     String reason, Throwable cause) {
        super(reason, cause);
        this.statusCode = statusCode;
        this.httpStatus = httpStatus;
        this.reason = reason;
    }
}