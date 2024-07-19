package org.sfa.volunteer.exception;

import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserStatusNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserStatusNotFoundException(UserStatusNotFoundException exception, WebRequest request) {
        String errorMessage = MessageSourceUtil.getMessage(SaayamStatusCode.INVALID_USER_STATUS.getCode(), new Object[]{exception.getUserStatusId()});
        log.error("UserStatusNotFoundException: {}", errorMessage);
        return ResponseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.INVALID_USER_STATUS, errorMessage);
    }

    @ExceptionHandler(UserCategoryNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserCategoryNotFoundException(UserCategoryNotFoundException exception, WebRequest request) {
        String errorMessage = MessageSourceUtil.getMessage(SaayamStatusCode.INVALID_USER_CATEGORY.getCode(), new Object[]{exception.getUserCategoryId()});
        log.error("UserCategoryNotFoundException: {}", errorMessage);
        return ResponseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.INVALID_USER_CATEGORY, errorMessage);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserNotFoundException(UserNotFoundException exception, WebRequest request) {
        String errorMessage = MessageSourceUtil.getMessage(SaayamStatusCode.USER_NOT_FOUND.getCode(), new Object[]{exception.getUserId()});
        log.error("UserNotFoundException: {}", errorMessage);
        return ResponseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.USER_NOT_FOUND, errorMessage);
    }

//    @ExceptionHandler(Exception.class)
//    @ResponseBody
//    public <T> SaayamResponse<T> handleGeneralException(Exception exception, WebRequest request) {
//        String errorMessage = MessageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
//        log.error("Exception: {}", errorMessage, exception);
//        return ResponseBuilder.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), SaayamStatusCode.INTERNAL_SERVER_ERROR, errorMessage);
//    }
}
