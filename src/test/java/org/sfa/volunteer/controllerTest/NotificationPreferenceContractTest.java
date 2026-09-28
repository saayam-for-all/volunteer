package org.sfa.volunteer.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.controller.NotificationController;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationPreferencesRequest;
import org.sfa.volunteer.dto.request.UpdateNotificationPreferencesRequest;
import org.sfa.volunteer.dto.response.GetNotificationPreferencesResponse;
import org.sfa.volunteer.dto.response.NotificationPreferenceResponse;
import org.sfa.volunteer.dto.response.UpdateNotificationPreferencesResponse;
import org.sfa.volunteer.service.NotificationPreferenceService;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the HTTP contract of the notification-preferences endpoints.
 *
 * <p>The verb and body shape matter beyond ordinary unit-test concerns here: these
 * endpoints are fronted by API Gateway proxy Lambdas that read the request body, so
 * a read modelled as GET with path/query parameters cannot be served by that handler.
 * This test fails if anyone changes the verb or the path.
 */
class NotificationPreferenceContractTest {

    private static final String BASE = "/0.0.1/notifications";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotificationPreferenceService preferenceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        preferenceService = mock(NotificationPreferenceService.class);
        NotificationService notificationService = mock(NotificationService.class);
        ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

        when(responseBuilder.buildSuccessResponse(any(SaayamStatusCode.class), any()))
                .thenAnswer(invocation -> SaayamResponse.success(
                        SaayamStatusCode.SUCCESS, "Operation successful", invocation.getArgument(1)));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(
                        notificationService, preferenceService, responseBuilder))
                .build();
    }

    @Test
    void preferencesIsServedAsAPostWithABody() throws Exception {
        when(preferenceService.getPreferences(any())).thenReturn(
                GetNotificationPreferencesResponse.builder()
                        .userId("SID-1")
                        .preferences(List.of(NotificationPreferenceResponse.builder()
                                .channelId(1).channelName("EMAIL").preference("both").build()))
                        .build());

        mockMvc.perform(post(BASE + "/preferences")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"SID-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("SID-1"))
                .andExpect(jsonPath("$.data.preferences[0].channelName").value("EMAIL"))
                .andExpect(jsonPath("$.data.preferences[0].preference").value("both"));
    }

    @Test
    void preferencesBindsTheUserIdFromTheBody() throws Exception {
        when(preferenceService.getPreferences(any())).thenReturn(
                GetNotificationPreferencesResponse.builder().userId("SID-9").preferences(List.of()).build());

        mockMvc.perform(post(BASE + "/preferences")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"SID-9\"}"))
                .andExpect(status().isOk());

        var captor = forClass(GetNotificationPreferencesRequest.class);
        verify(preferenceService).getPreferences(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("SID-9", captor.getValue().userId());
    }

    @Test
    void preferencesIsNotServedAsAGet() throws Exception {
        mockMvc.perform(get(BASE + "/preferences"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void preferencesUpdateIsServedAsAPostWithABody() throws Exception {
        when(preferenceService.updatePreferences(any())).thenReturn(
                UpdateNotificationPreferencesResponse.builder()
                        .userId("SID-1").created(1).updated(0).preferences(List.of()).build());

        mockMvc.perform(post(BASE + "/preferences/update")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"SID-1\",\"preferences\":[{\"channelId\":1,\"preference\":\"both\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1))
                .andExpect(jsonPath("$.data.updated").value(0));
    }

    @Test
    void preferencesUpdateBindsTheChannelListFromTheBody() throws Exception {
        when(preferenceService.updatePreferences(any())).thenReturn(
                UpdateNotificationPreferencesResponse.builder()
                        .userId("SID-1").created(0).updated(2).preferences(List.of()).build());

        mockMvc.perform(post(BASE + "/preferences/update")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"SID-1\",\"preferences\":["
                                + "{\"channelId\":1,\"preference\":\"both\"},"
                                + "{\"channelId\":3,\"preference\":null}]}"))
                .andExpect(status().isOk());

        var captor = forClass(UpdateNotificationPreferencesRequest.class);
        verify(preferenceService).updatePreferences(captor.capture());

        var request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(2, request.preferences().size());
        org.junit.jupiter.api.Assertions.assertEquals(1, request.preferences().get(0).channelId());
        org.junit.jupiter.api.Assertions.assertEquals("both", request.preferences().get(0).preference());
        org.junit.jupiter.api.Assertions.assertNull(request.preferences().get(1).preference(),
                "a null preference must survive binding, since it clears the channel");
    }
}
