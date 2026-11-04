package org.sfa.volunteer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilitySchema;
import org.sfa.volunteer.service.OnboardingAvailabilityService;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.OnboardingAvailabilityApi;
import org.sfa.volunteer.util.OnboardingAvailabilityAuthorization;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OnboardingAvailabilityStandaloneTest {
    private final AvailabilityAccountRepository accounts = mock(AvailabilityAccountRepository.class);
    private final OnboardingAvailabilitySchema schema = mock(OnboardingAvailabilitySchema.class);
    private final OnboardingAvailabilityService schedules = mock(OnboardingAvailabilityService.class);
    private final ResponseBuilder responses = new ResponseBuilder(mock(MessageSourceUtil.class));
    private final OnboardingAvailabilityAuthorization authorization = new OnboardingAvailabilityAuthorization(accounts, schema);
    private final OnboardingAvailabilityApi api = new OnboardingAvailabilityApi(schedules, authorization, responses);

    @Test
    void standaloneCannotUseArbitraryPrincipalOrHeaderAsGatewayIdentity() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingAvailabilityController(api)).build();
        mvc.perform(post(OnboardingAvailabilityController.PATH)
                        .principal(() -> "owner@example.test")
                        .header("Authorization", "Bearer synthetic.payload.signature")
                        .header("X-Caller", "owner@example.test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"SID-T4\",\"email\":\"owner@example.test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403));
        verifyNoInteractions(accounts, schema, schedules);
    }

    @Test
    void genericVolunteerUpdateRetainsLegacyStepThreeRequestAndResponse() throws Exception {
        var legacy = mock(VolunteerService.class);
        var stepThree = VolunteerRequest.builder().step(3).userId("SID-T4").build();
        when(legacy.updateVolunteer(stepThree)).thenReturn(VolunteerResponse.builder().userId("SID-T4").build());
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var controller = new VolunteerController(legacy, responses, api, authorization,
                    new ObjectMapper(), validators.getValidator());
            var mvc = MockMvcBuilders.standaloneSetup(controller).build();
            mvc.perform(put("/0.0.1/volunteers/updatevolunteer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"step\":3,\"userId\":\"SID-T4\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value("SID-T4"));
        }
        verify(legacy).updateVolunteer(stepThree);
        verifyNoInteractions(accounts, schema, schedules);
    }
}
