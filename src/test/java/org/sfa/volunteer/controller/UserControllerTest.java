package org.sfa.volunteer.controller;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.AvailabilityResponse;
import org.sfa.volunteer.dto.response.AvailabilitySlotResponse;
import org.sfa.volunteer.dto.response.UserSkillsResponse;
import org.sfa.volunteer.model.SkillLevel;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ResponseBuilder responseBuilder;

    @MockBean
    private ProfileImageStorageService profileImageStorageService;

    @MockBean
    private MessageSourceUtil messageSourceUtil;

    @Test
    void shouldReturnUserSkills() throws Exception {

        String requestJson = """
        {
          "userId": "123"
        }
        """;

        UserSkillsResponse serviceResponse = new UserSkillsResponse(
                "123", List.of("cat.1"), Map.of("cat.1", SkillLevel.ADVANCED));

        when(userService.getUserSkills("123"))
                .thenReturn(serviceResponse);

        when(responseBuilder.buildSuccessResponse(any(), any(), any()))
                .thenReturn(
                        SaayamResponse.success(
                                SaayamStatusCode.SUCCESS,
                                "Success",
                                serviceResponse
                        )
                );

        mockMvc.perform(post("/0.0.1/users/profileSkills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.skills[0]").value("cat.1"))
                .andExpect(jsonPath("$.data.skillLevels['cat.1']").value("ADVANCED"));

        verify(userService).getUserSkills("123");
    }

    @Test
    void shouldFailWhenUserIdMissing() throws Exception {

        String requestJson = "{}";

        // GlobalExceptionHandler reports validation errors in the SaayamResponse body
        mockMvc.perform(post("/0.0.1/users/profileSkills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        verify(responseBuilder).buildErrorResponse(
                eq(HttpStatus.BAD_REQUEST.value()), eq(SaayamStatusCode.BAD_REQUEST), anyString());
        verify(userService, never()).getUserSkills(any());
    }

    @Test
    void shouldReturnAvailability() throws Exception {

        AvailabilityResponse serviceResponse = new AvailabilityResponse("123", "America/Chicago",
                List.of(new AvailabilitySlotResponse(1, "Weekdays", "09:00", "17:00")), null);

        when(userService.getAvailability("123")).thenReturn(serviceResponse);
        when(responseBuilder.buildSuccessResponse(any(), any(), any()))
                .thenReturn(SaayamResponse.success(SaayamStatusCode.SUCCESS, "Success", serviceResponse));

        mockMvc.perform(post("/0.0.1/users/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("America/Chicago"))
                .andExpect(jsonPath("$.data.slots[0].frequency").value("Weekdays"));

        verify(userService).getAvailability("123");
    }

    @Test
    void shouldFailGetAvailabilityWhenUserIdMissing() throws Exception {

        buildErrorResponsesForReal();

        // GlobalExceptionHandler.handleValidationException has no @ResponseStatus, so the HTTP status stays 200
        mockMvc.perform(post("/0.0.1/users/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.saayamCode").value(SaayamStatusCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("must not be blank"));

        verify(responseBuilder).buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(), SaayamStatusCode.BAD_REQUEST, "must not be blank");
        verify(userService, never()).getAvailability(any());
    }

    @Test
    void shouldReturn400ForInvalidTimezone() throws Exception {

        String message = "timezone must be a valid IANA zone: Mars/Base";
        buildErrorResponsesForReal();
        when(userService.updateAvailability(any())).thenThrow(new IllegalArgumentException(message));

        mockMvc.perform(put("/0.0.1/users/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"123\", \"timezone\": \"Mars/Base\", \"slots\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.saayamCode").value(SaayamStatusCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(message));

        verify(responseBuilder).buildErrorResponse(HttpStatus.BAD_REQUEST.value(), SaayamStatusCode.BAD_REQUEST, message);
    }

    @Test
    void shouldReturn400ForInvalidTimeFormat() throws Exception {

        String message = "slots[0].startTime must be HH:mm: 9am";
        buildErrorResponsesForReal();
        when(userService.updateAvailability(any())).thenThrow(new IllegalArgumentException(message));

        mockMvc.perform(put("/0.0.1/users/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"123\", \"timezone\": \"UTC\", "
                                + "\"slots\": [{\"frequency\": \"Monday\", \"startTime\": \"9am\", \"endTime\": \"10:00\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.saayamCode").value(SaayamStatusCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(message));

        verify(responseBuilder).buildErrorResponse(HttpStatus.BAD_REQUEST.value(), SaayamStatusCode.BAD_REQUEST, message);
    }

    @Test
    void shouldAcceptEmptySlots() throws Exception {

        AvailabilityResponse serviceResponse = new AvailabilityResponse("123", "UTC", List.of(), null);
        when(userService.updateAvailability(any())).thenReturn(serviceResponse);
        when(responseBuilder.buildSuccessResponse(any(), any(), any()))
                .thenReturn(SaayamResponse.success(SaayamStatusCode.SUCCESS, "Success", serviceResponse));

        mockMvc.perform(put("/0.0.1/users/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"123\", \"timezone\": \"UTC\", \"slots\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slots").isEmpty());

        verify(userService).updateAvailability(argThat((UpdateAvailabilityRequest r) -> r.getSlots().isEmpty()));
    }

    // ResponseBuilder is a mock here; build the real error envelope so the body can be asserted
    private void buildErrorResponsesForReal() {
        when(responseBuilder.buildErrorResponse(anyInt(), any(), anyString()))
                .thenAnswer(inv -> SaayamResponse.error(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
    }

    @Test
    void shouldUpdateAvailability() throws Exception {

        String requestJson = """
        {
          "userId": "123",
          "timezone": "America/Chicago",
          "slots": [{ "frequency": "Weekdays", "startTime": "09:00", "endTime": "17:00" }],
          "vacationMode": false,
          "vacationStartDate": "",
          "vacationEndDate": ""
        }
        """;

        AvailabilityResponse serviceResponse = new AvailabilityResponse("123", "America/Chicago",
                List.of(new AvailabilitySlotResponse(1, "Weekdays", "09:00", "17:00")), null);

        when(userService.updateAvailability(any())).thenReturn(serviceResponse);
        when(responseBuilder.buildSuccessResponse(any(), any(), any()))
                .thenReturn(SaayamResponse.success(SaayamStatusCode.SUCCESS, "Success", serviceResponse));

        mockMvc.perform(put("/0.0.1/users/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].startTime").value("09:00"));

        verify(userService).updateAvailability(argThat((UpdateAvailabilityRequest r) ->
                r.getUserId().equals("123")
                        && r.getTimezone().equals("America/Chicago")
                        && r.getSlots().size() == 1
                        && r.getSlots().get(0).getFrequency().equals("Weekdays")));
    }
}
