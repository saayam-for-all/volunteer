package org.sfa.volunteer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.config.MessageSourceConfig;
import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;
import org.sfa.volunteer.exception.SkillsException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.SkillsService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SkillsController.class)
@Import({ResponseBuilder.class, MessageSourceUtil.class, MessageSourceConfig.class})
class SkillsControllerTest {

    private static final String URL = "/0.0.1/volunteerStep3/skills";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillsService skillsService;

    @Test
    void saveUserSkills_returns200_onSuccess() throws Exception {
        SaveUserSkillsResponse serviceResponse = SaveUserSkillsResponse.builder()
                .userId("user-1")
                .savedCatIds(List.of("1.1", "1.2"))
                .wizardStage(3)
                .build();

        when(skillsService.saveUserSkills(any())).thenReturn(serviceResponse);

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of("1.1", "1.2"))
                .build();

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.wizardStage").value(3));
    }

    @Test
    void saveUserSkills_returnsError_whenUserNotFound() throws Exception {
        when(skillsService.saveUserSkills(any())).thenThrow(new UserNotFoundException("user-x"));

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-x")
                .catIds(List.of("1.1"))
                .build();

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveUserSkills_returnsError_whenBadRequest() throws Exception {
        when(skillsService.saveUserSkills(any())).thenThrow(SkillsException.badRequest("user-1"));

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of("1.1"))
                .build();

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveUserSkills_returnsError_whenInvalidCategory() throws Exception {
        when(skillsService.saveUserSkills(any())).thenThrow(SkillsException.invalidCategory("user-1"));

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of("9.9"))
                .build();

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
