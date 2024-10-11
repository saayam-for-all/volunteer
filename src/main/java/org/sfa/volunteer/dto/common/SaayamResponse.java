package org.sfa.volunteer.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.ZonedDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record SaayamResponse<T>(
        boolean success,
        int statusCode,
        String saayamCode,
        String message,
        T data,
        ZonedDateTime timestamp
) {
    public static <T> SaayamResponse<T> success(SaayamStatusCode statusCode, String message, T data) {
        return SaayamResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .saayamCode(statusCode.getCode())
                .message(message)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public static <T> SaayamResponse<T> error(int httpStatusCode, SaayamStatusCode statusCode, String message) {
        return SaayamResponse.<T>builder()
                .success(false)
                .statusCode(httpStatusCode)
                .saayamCode(statusCode.getCode())
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }
}


