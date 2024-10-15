package org.sfa.volunteer.exception;

import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSourceUtil messageSourceUtil;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public GlobalExceptionHandler(MessageSourceUtil messageSourceUtil, ResponseBuilder responseBuilder) {
        this.messageSourceUtil = messageSourceUtil;
        this.responseBuilder = responseBuilder;
    }

    @ExceptionHandler(UserStatusNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserStatusNotFoundException(UserStatusNotFoundException exception, WebRequest request) {
        String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INVALID_USER_STATUS.getCode(), new Object[]{exception.getUserStatusId()});
        log.error("UserStatusNotFoundException: {}", errorMessage);
        return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.INVALID_USER_STATUS, errorMessage);
    }

    @ExceptionHandler(UserCategoryNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserCategoryNotFoundException(UserCategoryNotFoundException exception, WebRequest request) {
        String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INVALID_USER_CATEGORY.getCode(), new Object[]{exception.getUserCategoryId()});
        log.error("UserCategoryNotFoundException: {}", errorMessage);
        return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.INVALID_USER_CATEGORY, errorMessage);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleUserNotFoundException(UserNotFoundException exception, WebRequest request) {
        String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.USER_NOT_FOUND.getCode(), new Object[]{exception.getUserId()});
        log.error("UserNotFoundException: {}", errorMessage);
        return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.USER_NOT_FOUND, errorMessage);
    }

    @ExceptionHandler(VolunteerException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleVolunteerException(VolunteerException exception, WebRequest request) {
        String errorMessage = null;
        SaayamStatusCode status = null;
        if (exception.getMessage().contains(SaayamStatusCode.VOLUNTEER_NOT_FOUND.toString())) {
            errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.VOLUNTEER_NOT_FOUND.getCode(), new Object[]{exception.getUserId()});
            log.error("VolunteerNotFoundException: {}", errorMessage);
            status = SaayamStatusCode.VOLUNTEER_NOT_FOUND;
        }
        else if (exception.getMessage().contains(SaayamStatusCode.VOLUNTEER_EXISTS.toString())) {
            errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.VOLUNTEER_EXISTS.getCode(), new Object[]{exception.getUserId()});
            log.error("VolunteerExistsException: {}", errorMessage);
            status = SaayamStatusCode.VOLUNTEER_EXISTS;
        }
        else if (exception.getMessage().contains(SaayamStatusCode.INVALID_VOLUNTEER_STEP.toString())) {
            errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INVALID_VOLUNTEER_STEP.getCode(), new Object[]{exception.getUserId()});
            log.error("VolunteerInvalidStepException: {}", errorMessage);
            status = SaayamStatusCode.INVALID_VOLUNTEER_STEP;
        }
        else {
            errorMessage = "Unknown error";
            status = SaayamStatusCode.UNKNOWN_ERROR; // Adjust as needed
        }
        return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), status , errorMessage);
    }


//    @ExceptionHandler(Exception.class)
//    @ResponseBody
//    public <T> SaayamResponse<T> handleGeneralException(Exception exception, WebRequest request) {
//        String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null);
//        log.error("Exception: {}", errorMessage, exception);
//        return responseBuilder.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), SaayamStatusCode.INTERNAL_SERVER_ERROR, errorMessage);
//    }
}
