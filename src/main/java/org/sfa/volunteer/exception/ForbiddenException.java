package org.sfa.volunteer.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

    private final String reason;

    public ForbiddenException(String reason) {
        super(reason);
        this.reason = reason;
    }
}