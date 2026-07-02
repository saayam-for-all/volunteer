package org.sfa.volunteer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.controller.UserController;
import org.sfa.volunteer.dto.request.ValidateProfileRequest;
import org.sfa.volunteer.dto.response.ProfileValidationResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class ValidateProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private MessageSourceUtil messageSourceUtil;

    @MockBean
    private ResponseBuilder responseBuilder;

    private static final String ENDPOINT = "/0.0.1/users/validateProfile";

    @Nested
    @DisplayName("Request validation tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return 400 when userId is missing")
        void shouldReturn400WhenUserIdMissing() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when userId is blank")
        void shouldReturn400WhenUserIdBlank() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "   "
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Successful request tests")
    class SuccessfulRequestTests {

        @Test
        @DisplayName("Should return OK when profile is complete")
        void shouldReturnOkWhenProfileComplete() throws Exception {
            ProfileValidationResponse mockResponse =
                    ProfileValidationResponse.builder()
                            .profileComplete(true)
                            .userId("user-123")
                            .missingFields(List.of())
                            .build();

            when(userService.validateProfile(any(ValidateProfileRequest.class)))
                    .thenReturn(mockResponse);

            when(messageSourceUtil.getMessage(any(), any()))
                    .thenReturn("Profile validation check completed");

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "user-123"
                                    }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return OK when profile is incomplete")
        void shouldReturnOkWhenProfileIncomplete() throws Exception {
            ProfileValidationResponse mockResponse =
                    ProfileValidationResponse.builder()
                            .profileComplete(false)
                            .userId("user-123")
                            .missingFields(List.of("phone", "country"))
                            .build();

            when(userService.validateProfile(any(ValidateProfileRequest.class)))
                    .thenReturn(mockResponse);

            when(messageSourceUtil.getMessage(any(), any()))
                    .thenReturn("Profile validation check completed");

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "user-123"
                                    }
                                    """))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("HTTP method tests")
    class HttpMethodTests {

        @Test
        @DisplayName("Should return 405 for GET request")
        void shouldReturn405ForGetRequest() throws Exception {
            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should return 405 for PUT request")
        void shouldReturn405ForPutRequest() throws Exception {
            mockMvc.perform(put(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "user-123"
                                    }
                                    """))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should return 405 for DELETE request")
        void shouldReturn405ForDeleteRequest() throws Exception {
            mockMvc.perform(delete(ENDPOINT))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Content-Type tests")
    class ContentTypeTests {

        @Test
        @DisplayName("Should return 415 for non-JSON content type")
        void shouldReturn415ForNonJsonContentType() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("userId=user-123"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}