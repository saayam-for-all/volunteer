package org.sfa.volunteer.util;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
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
}
