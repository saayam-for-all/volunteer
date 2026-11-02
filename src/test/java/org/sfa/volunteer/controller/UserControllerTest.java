package org.sfa.volunteer.controller;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
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

import static org.mockito.ArgumentMatchers.any;
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
}