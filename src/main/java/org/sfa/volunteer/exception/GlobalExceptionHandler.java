package org.sfa.volunteer.exception;

import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
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
        public <T> SaayamResponse<T> handleUserStatusNotFoundException(UserStatusNotFoundException exception,
                        WebRequest request) {
                String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INVALID_USER_STATUS.getCode(),
                                new Object[] { exception.getUserStatusId() });
                log.error("UserStatusNotFoundException: {}", errorMessage);
                return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(),
                                SaayamStatusCode.INVALID_USER_STATUS,
                                errorMessage);
        }

        @ExceptionHandler(UserCategoryNotFoundException.class)
        @ResponseBody
        public <T> SaayamResponse<T> handleUserCategoryNotFoundException(UserCategoryNotFoundException exception,
                        WebRequest request) {
                String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INVALID_USER_CATEGORY.getCode(),
                                new Object[] { exception.getUserCategoryId() });
                log.error("UserCategoryNotFoundException: {}", errorMessage);
                return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(),
                                SaayamStatusCode.INVALID_USER_CATEGORY,
                                errorMessage);
        }

        @ExceptionHandler(UserNotFoundException.class)
        @ResponseBody
        public <T> SaayamResponse<T> handleUserNotFoundException(UserNotFoundException exception, WebRequest request) {
                String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.USER_NOT_FOUND.getCode(),
                                new Object[] { exception.getUserId() });
                log.error("UserNotFoundException: {}", errorMessage);
                return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), SaayamStatusCode.USER_NOT_FOUND,
                                errorMessage);
        }

        @ExceptionHandler(VolunteerException.class)
        @ResponseBody
        public <T> SaayamResponse<T> handleVolunteerException(VolunteerException exception, WebRequest request) {
                String errorMessage = null;
                SaayamStatusCode status = null;
                if (exception.getMessage().contains(SaayamStatusCode.VOLUNTEER_NOT_FOUND.toString())) {
                        errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.VOLUNTEER_NOT_FOUND.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("VolunteerNotFoundException: {}", errorMessage);
                        status = SaayamStatusCode.VOLUNTEER_NOT_FOUND;
                } else if (exception.getMessage().contains(SaayamStatusCode.VOLUNTEER_EXISTS.toString())) {
                        errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.VOLUNTEER_EXISTS.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("VolunteerExistsException: {}", errorMessage);
                        status = SaayamStatusCode.VOLUNTEER_EXISTS;
                } else if (exception.getMessage().contains(SaayamStatusCode.INVALID_VOLUNTEER_STEP.toString())) {
                        errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.INVALID_VOLUNTEER_STEP.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("VolunteerInvalidStepException: {}", errorMessage);
                        status = SaayamStatusCode.INVALID_VOLUNTEER_STEP;
                } else {
                        errorMessage = "Unknown error";
                        status = SaayamStatusCode.UNKNOWN_ERROR; // Adjust as needed
                }
                return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(), status, errorMessage);
        }

        @ExceptionHandler(UserOrganizationNotFoundException.class)
        @ResponseBody
        public <T> SaayamResponse<T> handleUserOrganizationNotFoundException(
                        UserOrganizationNotFoundException exception,
                        WebRequest request) {
                String errorMessage = messageSourceUtil.getMessage(SaayamStatusCode.ORGANIZATION_NOT_FOUND.getCode(),
                                new Object[] { exception.getUserId() });
                log.error("UserOrganizationNotFoundException: {}", errorMessage);
                return responseBuilder.buildErrorResponse(HttpStatus.NOT_FOUND.value(),
                                SaayamStatusCode.ORGANIZATION_NOT_FOUND,
                                errorMessage);
        }

        @ExceptionHandler(CountryNotFoundException.class)
        @ResponseBody
        public <T> SaayamResponse<T> handleCountryNotFoundException(
                        CountryNotFoundException exception,
                        WebRequest request) {

                String errorMessage = messageSourceUtil.getMessage(
                                SaayamStatusCode.COUNTRY_NOT_FOUND.getCode(),
                                new Object[] { exception.getCountryName() });

                log.error("CountryNotFoundException: {}", errorMessage);

                return responseBuilder.buildErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                SaayamStatusCode.COUNTRY_NOT_FOUND,
                                errorMessage);
        }

	@ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public <T> SaayamResponse<T> handleValidationException(MethodArgumentNotValidException exception, WebRequest request) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("ValidationException: {}", errorMessage);
        return responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST.value(), SaayamStatusCode.BAD_REQUEST, errorMessage);
    }

        @ExceptionHandler(NotificationException.class)
        @ResponseBody
        public <T> SaayamResponse<T> handleNotificationException(NotificationException exception, WebRequest request) {

                String errorMessage;
                SaayamStatusCode status;

                // 1. User not found
                if (exception.getMessage().contains(SaayamStatusCode.USER_NOT_FOUND.toString())) {
                        errorMessage = messageSourceUtil.getMessage(
                                        SaayamStatusCode.USER_NOT_FOUND.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("NotificationUserNotFoundException: {}", errorMessage);
                        status = SaayamStatusCode.USER_NOT_FOUND;
                }
                // 2. Invalid Request Parameters (negative values, rowStart > rowEnd)
                else if (exception.getMessage().contains(SaayamStatusCode.INVALID_PARAMETER.toString())) {
                        errorMessage = messageSourceUtil.getMessage(
                                        SaayamStatusCode.INVALID_PARAMETER.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("NotificationInvalidPaginationException: {}", errorMessage);
                        status = SaayamStatusCode.INVALID_PARAMETER;
                }
                // 3. Repository / DB errors (DataAccessException wrapped inside
                // NotificationException)
                else if (exception.getMessage().contains(SaayamStatusCode.DATABASE_ERROR.toString())) {
                        errorMessage = messageSourceUtil.getMessage(
                                        SaayamStatusCode.DATABASE_ERROR.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("NotificationDatabaseException: {}", errorMessage);
                        status = SaayamStatusCode.DATABASE_ERROR;
                }

                else if (exception.getMessage().contains(SaayamStatusCode.BAD_REQUEST.toString())) {
                        errorMessage = messageSourceUtil.getMessage(
                                        SaayamStatusCode.BAD_REQUEST.getCode(),
                                        new Object[] { exception.getUserId() });
                        log.error("NotificationBadRequestException: {}", errorMessage);
                        status = SaayamStatusCode.BAD_REQUEST;
                }
                // 4. Unknown Notification error
                else {
                        errorMessage = "Notification error";
                        log.error("NotificationException: {}", errorMessage);
                        status = SaayamStatusCode.UNEXPECTED_ERROR;
                }

                return responseBuilder.buildErrorResponse(
                                HttpStatus.BAD_REQUEST.value(), // All domain errors → 400
                                status,
                                errorMessage);

        }

}
