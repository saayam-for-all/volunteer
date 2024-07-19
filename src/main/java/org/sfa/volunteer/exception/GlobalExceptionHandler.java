package org.sfa.volunteer.exception;

import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @Autowired
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(UserStatusNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserStatusNotFoundException(UserStatusNotFoundException exception, WebRequest request) {
        String errorMessage = messageSource.getMessage(SaayamStatusCode.INVALID_USER_STATUS.getCode(), new Object[]{exception.getUserStatusId()}, request.getLocale());
        log.error("UserStatusNotFoundException: {}", errorMessage);
        return ResponseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.INVALID_USER_STATUS, errorMessage);
    }

    @ExceptionHandler(UserCategoryNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserCategoryNotFoundException(UserCategoryNotFoundException exception, WebRequest request) {
        String errorMessage = messageSource.getMessage(SaayamStatusCode.INVALID_USER_CATEGORY.getCode(), new Object[]{exception.getUserCategoryId()}, request.getLocale());
        log.error("UserCategoryNotFoundException: {}", errorMessage);
        return ResponseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.INVALID_USER_CATEGORY, errorMessage);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserNotFoundException(UserNotFoundException exception, WebRequest request) {
        String errorMessage = messageSource.getMessage(SaayamStatusCode.USER_NOT_FOUND.getCode(), new Object[]{exception.getUserId()}, request.getLocale());
        log.error("UserNotFoundException: {}", errorMessage);
        return ResponseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.USER_NOT_FOUND, errorMessage);
    }

//    @ExceptionHandler(Exception.class)
//    @ResponseBody
//    public <T> SaayamResponse<T> handleGeneralException(Exception exception, WebRequest request) {
//        String errorMessage = messageSource.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null, request.getLocale());
//        log.error("Exception: {}", errorMessage, exception);
//        return ResponseBuilder.buildErrorResponse(500, SaayamStatusCode.INTERNAL_SERVER_ERROR, errorMessage);
//    }
}
