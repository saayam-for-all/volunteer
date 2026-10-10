package org.sfa.volunteer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.DeleteUserSkillsRequest;
import org.sfa.volunteer.dto.request.UpdateUserSkillsRequest;
import org.sfa.volunteer.dto.request.UserSkillsRequest;
import org.sfa.volunteer.dto.response.UserSkillsResponse;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerSkillsTest {

    private static final String USER_ID = "SID-00-000-002-10000";

    @Mock
    private UserService userService;

    @Mock
    private ResponseBuilder responseBuilder;

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @InjectMocks
    private UserController userController;

    private List<String> skills;
    private UserSkillsResponse skillsResponse;

    @BeforeEach
    void setUp() {
        skills = List.of("4.2", "0.0.0.0.0");
        skillsResponse = new UserSkillsResponse(USER_ID, skills);
    }

    @Test
    void getUserSkills_returnsSkillsInResponseData() {
        UserSkillsRequest request = new UserSkillsRequest();
        request.setUserId(USER_ID);

        SaayamResponse<UserSkillsResponse> wrapped =
                SaayamResponse.success(SaayamStatusCode.SUCCESS, "ok", skillsResponse);

        when(userService.getUserSkills(USER_ID)).thenReturn(skillsResponse);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                any(Object[].class),
                eq(skillsResponse)))
                .thenReturn(wrapped);

        SaayamResponse<UserSkillsResponse> result = userController.getUserSkills(request);

        assertThat(result).isSameAs(wrapped);
        verify(userService).getUserSkills(USER_ID);

        // The service response must reach the builder as the data payload.
        // Passing null here was the cause of #189.
        ArgumentCaptor<UserSkillsResponse> dataCaptor =
                ArgumentCaptor.forClass(UserSkillsResponse.class);
        verify(responseBuilder).buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                any(Object[].class),
                dataCaptor.capture());

        UserSkillsResponse passed = dataCaptor.getValue();
        assertThat(passed).isNotNull();
        assertThat(passed.getUserId()).isEqualTo(USER_ID);
        assertThat(passed.getSkills()).containsExactlyElementsOf(skills);
    }

    @Test
    void updateUserSkills_passesSkillsToService() {
        UpdateUserSkillsRequest request = new UpdateUserSkillsRequest();
        request.setUserId(USER_ID);
        request.setSkills(skills);

        SaayamResponse<String> wrapped = SaayamResponse.success(
                SaayamStatusCode.SUCCESS, "ok", "Skills updated successfully");

        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                any(Object[].class),
                any(String.class)))
                .thenReturn(wrapped);

        SaayamResponse<String> result = userController.updateUserSkills(request);

        assertThat(result).isSameAs(wrapped);
        verify(userService).updateUserSkills(USER_ID, skills);
    }

    @Test
    void deleteUserSkills_passesRemainingSkillsToService() {
        List<String> remaining = List.of("4.2");

        DeleteUserSkillsRequest request = new DeleteUserSkillsRequest();
        request.setUserId(USER_ID);
        request.setSkills(remaining);

        SaayamResponse<String> wrapped = SaayamResponse.success(
                SaayamStatusCode.SUCCESS, "ok", "Skills deleted successfully");

        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                any(Object[].class),
                any(String.class)))
                .thenReturn(wrapped);

        SaayamResponse<String> result = userController.deleteUserSkills(request);

        assertThat(result).isSameAs(wrapped);
        verify(userService).updateUserSkills(USER_ID, remaining);
    }
}