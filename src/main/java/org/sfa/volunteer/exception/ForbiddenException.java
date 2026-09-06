package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ForbiddenException extends RuntimeException {
    private final String reason;
}