package org.sfa.volunteer.util;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;

public class ResponseBuilder {

    public static <T> SaayamResponse<T> buildSuccessResponse(SaayamStatusCode statusCode, String message, T data) {
        return SaayamResponse.success(statusCode, message, data);
    }

    public static <T> SaayamResponse<T> buildErrorResponse(int httpStatusCode, SaayamStatusCode statusCode, String message) {
        return SaayamResponse.error(httpStatusCode, statusCode, message);
    }
}
