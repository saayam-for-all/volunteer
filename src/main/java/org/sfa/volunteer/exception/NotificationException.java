package org.sfa.volunteer.exception;

import org.sfa.volunteer.dto.common.SaayamStatusCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationException extends RuntimeException {

    private final String userId;

    public NotificationException(String message, String userId) {
        super(message);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public static NotificationException userNotFound(String userId) {
        return new NotificationException(SaayamStatusCode.USER_NOT_FOUND.toString(), userId);
    }

    public static NotificationException invalidPagination(String userId) {
        return new NotificationException(SaayamStatusCode.INVALID_PARAMETER.toString(), userId);
    }
}