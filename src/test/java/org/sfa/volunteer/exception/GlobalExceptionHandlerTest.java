package org.sfa.volunteer.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private final FailureController controller = new FailureController();
    private GlobalExceptionHandler handler;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("lang/messages");
        var messages = new MessageSourceUtil(source);
        handler = new GlobalExceptionHandler(messages, new ResponseBuilder(messages));
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(handler).build();
    }

    static Stream<Arguments> notificationErrors() {
        return Stream.of(
                Arguments.of(NotificationException.userNotFound("user-1"), SaayamStatusCode.USER_NOT_FOUND, 404),
                Arguments.of(NotificationException.invalidPagination("user-1"), SaayamStatusCode.INVALID_PARAMETER, 400),
                Arguments.of(new NotificationException("DATABASE_ERROR", "user-1"), SaayamStatusCode.DATABASE_ERROR, 500),
                Arguments.of(new NotificationException("BAD_REQUEST", "user-1"), SaayamStatusCode.BAD_REQUEST, 400),
                Arguments.of(new NotificationException("NOTIFICATION_NOT_FOUND", "user-1"), SaayamStatusCode.NOTIFICATION_NOT_FOUND, 404),
                Arguments.of(new NotificationException("NOTIFICATION_ALREADY_ACTIONED", "user-1"), SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED, 409),
                Arguments.of(new NotificationException("unrecognized failure", "user-1"), SaayamStatusCode.UNEXPECTED_ERROR, 500));
    }

    @ParameterizedTest
    @MethodSource("notificationErrors")
    void notificationErrorsReturnMatchingHttpAndApplicationStatuses(
            NotificationException error, SaayamStatusCode code, int httpStatus) throws Exception {
        controller.error = error;
        mvc.perform(get("/test/failure"))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(httpStatus))
                .andExpect(jsonPath("$.saayamCode").value(code.getCode()));
    }

    static Stream<Arguments> existingDomainErrors() {
        return Stream.of(
                Arguments.of(new UserStatusNotFoundException(1), SaayamStatusCode.INVALID_USER_STATUS),
                Arguments.of(new UserCategoryNotFoundException(1), SaayamStatusCode.INVALID_USER_CATEGORY),
                Arguments.of(new UserNotFoundException("user-1"), SaayamStatusCode.USER_NOT_FOUND),
                Arguments.of(new UserOrganizationNotFoundException("user-1"), SaayamStatusCode.ORGANIZATION_NOT_FOUND),
                Arguments.of(new CountryNotFoundException("unknown-country"), SaayamStatusCode.COUNTRY_NOT_FOUND),
                Arguments.of(new VolunteerException("VOLUNTEER_NOT_FOUND", "user-1"), SaayamStatusCode.VOLUNTEER_NOT_FOUND),
                Arguments.of(new VolunteerException("VOLUNTEER_EXISTS", "user-1"), SaayamStatusCode.VOLUNTEER_EXISTS),
                Arguments.of(new VolunteerException("INVALID_VOLUNTEER_STEP", "user-1"), SaayamStatusCode.INVALID_VOLUNTEER_STEP),
                Arguments.of(new VolunteerException("unrecognized failure", "user-1"), SaayamStatusCode.UNKNOWN_ERROR));
    }

    @ParameterizedTest
    @MethodSource("existingDomainErrors")
    void sharedHandlerPreservesExistingDomainErrorBodies(RuntimeException error, SaayamStatusCode code)
            throws Exception {
        controller.error = error;
        mvc.perform(get("/test/failure"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.saayamCode").value(code.getCode()));
    }

    @Test
    void acceptExceptionUsesItsHttpStatusAndMessageDetail() throws Exception {
        controller.error = new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_NOT_FOUND,
                HttpStatus.NOT_FOUND, "user-1", 42);
        mvc.perform(get("/test/failure"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found with ID 42"));
    }

    @Test
    void validationErrorsIncludeAllFieldMessages() throws Exception {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "userId", "User ID is required"));
        binding.addError(new FieldError("request", "name", "Name is required"));
        var parameter = new MethodParameter(FailureController.class.getDeclaredMethod("fail"), -1);
        var error = new MethodArgumentNotValidException(parameter, binding);
        var response = handler.handleValidationException(error, null);
        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("User ID is required; Name is required");
    }

    @RestController
    static class FailureController {
        RuntimeException error;

        @GetMapping("/test/failure")
        public void fail() { throw error; }
    }
}
