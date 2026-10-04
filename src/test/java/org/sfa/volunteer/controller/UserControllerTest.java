package org.sfa.volunteer.controller;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.UserSkillsResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void shouldReturnUserSkills() throws Exception {

        String requestJson = """
        {
          "userId": "123"
        }
        """;

        UserSkillsResponse serviceResponse = new UserSkillsResponse();

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

        mockMvc.perform(post("/profileSkills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").exists());

        verify(userService).getUserSkills("123");
    }


//    // 🔹 2. UPDATE USER SKILLS
//    @Test
//    void shouldUpdateUserSkills() throws Exception {
//
//        String requestJson = """
//        {
//          "userId": "123",
//          "skills": ["Java"]
//        }
//        """;
//
//        SaayamResponse<String> mockResponse = new SaayamResponse<>();
//        mockResponse.setStatus("SUCCESS");
//        mockResponse.setData("Skills updated successfully");
//
//        doNothing().when(userService)
//                .updateUserSkills(anyString(), anyList());
//
//        when(responseBuilder.buildSuccessResponse(any(), any(), any()))
//                .thenReturn(mockResponse);
//
//        mockMvc.perform(put("/profileSkills/update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("SUCCESS"));
//
//        verify(userService).updateUserSkills("123", List.of("Java"));
//    }
//
//    // 🔹 3. DELETE USER SKILLS
//    @Test
//    void shouldDeleteUserSkills() throws Exception {
//
//        String requestJson = """
//        {
//          "userId": "123",
//          "skills": ["Java"]
//        }
//        """;
//
//        SaayamResponse<String> mockResponse = new SaayamResponse<>();
//        mockResponse.setStatus("SUCCESS");
//        mockResponse.setData("Skills deleted successfully");
//
//        doNothing().when(userService)
//                .deleteUserSkills(anyString(), anyList());
//
//        when(responseBuilder.buildSuccessResponse(any(), any(), any()))
//                .thenReturn(mockResponse);
//
//        mockMvc.perform(delete("/profileSkills/delete")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("SUCCESS"));
//
//        verify(userService).deleteUserSkills("123", List.of("Java"));
//    }

    // 🔥 BONUS (recommended): invalid request
    @Test
    void shouldFailWhenUserIdMissing() throws Exception {

        String requestJson = "{}";

        mockMvc.perform(post("/profileSkills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}