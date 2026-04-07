package org.sfa.volunteer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.request.CheckUserExistsRequest;
import org.sfa.volunteer.dto.response.UserExistsResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class DoesUserExistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private MessageSourceUtil messageSourceUtil;

    @MockBean
    private ResponseBuilder responseBuilder;

    private static final String ENDPOINT = "/0.0.1/users/doesUserExist";

    @Nested
    @DisplayName("Request validation tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return 400 when firstName is missing")
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            String requestJson = """
                    {
                        "lastName": "Doe"
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when lastName is missing")
        void shouldReturn400WhenLastNameMissing() throws Exception {
            String requestJson = """
                    {
                        "firstName": "John"
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when firstName is blank")
        void shouldReturn400WhenFirstNameBlank() throws Exception {
            String requestJson = """
                    {
                        "firstName": "   ",
                        "lastName": "Doe"
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when lastName is blank")
        void shouldReturn400WhenLastNameBlank() throws Exception {
            String requestJson = """
                    {
                        "firstName": "John",
                        "lastName": ""
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when both firstName and lastName are missing")
        void shouldReturn400WhenBothNamesMissing() throws Exception {
            String requestJson = """
                    {
                        "email": "john@example.com"
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when request body is empty")
        void shouldReturn400WhenRequestBodyEmpty() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Successful request tests")
    class SuccessfulRequestTests {

        @Test
        @DisplayName("Should accept valid request with only required fields")
        void shouldAcceptRequestWithOnlyRequiredFields() throws Exception {
            UserExistsResponse mockResponse = UserExistsResponse.builder()
                    .exists(true)
                    .userId("user-123")
                    .matchedOn("firstName, lastName")
                    .build();

            when(userService.checkUserExists(any(CheckUserExistsRequest.class)))
                    .thenReturn(mockResponse);
            when(messageSourceUtil.getMessage(any(), any()))
                    .thenReturn("User existence check completed");

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "John",
                                        "lastName": "Doe"
                                    }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept valid request with all fields")
        void shouldAcceptRequestWithAllFields() throws Exception {
            UserExistsResponse mockResponse = UserExistsResponse.builder()
                    .exists(true)
                    .userId("user-123")
                    .matchedOn("firstName, lastName, email, phone, country")
                    .build();

            when(userService.checkUserExists(any(CheckUserExistsRequest.class)))
                    .thenReturn(mockResponse);
            when(messageSourceUtil.getMessage(any(), any()))
                    .thenReturn("User existence check completed");

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "email": "john.doe@example.com",
                                        "phone": "+1234567890",
                                        "country": "United States"
                                    }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept valid request when user not found")
        void shouldAcceptRequestWhenUserNotFound() throws Exception {
            UserExistsResponse mockResponse = UserExistsResponse.builder()
                    .exists(false)
                    .userId(null)
                    .matchedOn(null)
                    .build();

            when(userService.checkUserExists(any(CheckUserExistsRequest.class)))
                    .thenReturn(mockResponse);
            when(messageSourceUtil.getMessage(any(), any()))
                    .thenReturn("User existence check completed");

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Unknown",
                                        "lastName": "Person"
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
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ENDPOINT))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should return 405 for PUT request")
        void shouldReturn405ForPutRequest() throws Exception {
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "firstName": "John",
                                                "lastName": "Doe"
                                            }
                                            """))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should return 405 for DELETE request")
        void shouldReturn405ForDeleteRequest() throws Exception {
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(ENDPOINT))
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
                            .content("firstName=John&lastName=Doe"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}
