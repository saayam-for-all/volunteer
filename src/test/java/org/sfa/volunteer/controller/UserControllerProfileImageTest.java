package org.sfa.volunteer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerProfileImageTest {

    private static final String USER_ID = "user-1";
    private static final String S3_URI = "s3://saayam-us-private/users/user-1/profile";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserService userService;
    private ProfileImageStorageService profileImageStorageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        profileImageStorageService = mock(ProfileImageStorageService.class);

        MessageSourceUtil messageSourceUtil = mock(MessageSourceUtil.class);
        when(messageSourceUtil.getMessage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserController controller = new UserController(
                userService,
                new ResponseBuilder(messageSourceUtil),
                profileImageStorageService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadProfileImageStoresImageAndReturnsS3Uri() throws Exception {
        when(profileImageStorageService.uploadBase64(eq(USER_ID), eq("image/png"), eq("base64-image"), eq("us-east-1")))
                .thenReturn(Map.of(
                        "message", "Profile image uploaded",
                        "userId", USER_ID,
                        "s3Uri", S3_URI,
                        "bucket", "saayam-us-private",
                        "key", "users/user-1/profile"));

        mockMvc.perform(post("/0.0.1/users/profileImage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Caller-UserId", USER_ID)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", USER_ID,
                                "contentType", "image/png",
                                "base64", "base64-image"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.s3Uri").value(S3_URI));
    }

    @Test
    void profileSaveAfterImageUploadKeepsStoredProfilePicturePath() throws Exception {
        when(userService.updateUserProfile(eq(USER_ID), any()))
                .thenReturn(UserProfileResponse.builder()
                        .id(USER_ID)
                        .firstName("Neel")
                        .lastName("Harip")
                        .profilePicturePath(S3_URI)
                        .build());

        mockMvc.perform(put("/0.0.1/users/profile/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Neel",
                                "lastName", "Harip"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.profilePicturePath").value(S3_URI));
    }
}
