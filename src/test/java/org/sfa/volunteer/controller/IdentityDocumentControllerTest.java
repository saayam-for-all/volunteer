package org.sfa.volunteer.controller;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.service.IdentityDocumentStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdentityDocumentController.class)
class IdentityDocumentControllerTest {

    private static final String METADATA_URL = "/0.0.1/volunteers/identity-document/metadata";
    private static final String UPLOAD_URL = "/0.0.1/volunteers/identity-document/upload";
    private static final String FILE_URL = "/0.0.1/volunteers/identity-document/file";

    private static final String OWNER = "SID-001";
    private static final String OTHER = "SID-999";
    private static final String EMAIL = "laxmi@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private IdentityDocumentStorageService storage;
    @MockBean private UserService userService;
    @MockBean private VolunteerService volunteerService;
    @MockBean private ResponseBuilder responseBuilder;
    @MockBean private MessageSourceUtil messageSourceUtil;

    /** Token whose payload carries the given claims, base64url encoded as Cognito emits it. */
    private static String token(String payloadJson) {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return "header." + payload + ".signature";
    }

    private static String volunteerToken() {
        return token("{\"email\":\"" + EMAIL + "\",\"cognito:groups\":[\"volunteer\"]}");
    }

    private static String adminToken() {
        return token("{\"email\":\"admin@example.com\",\"cognito:groups\":[\"admin\"]}");
    }

    private static String slotBody(String userId, int slot) {
        return "{\"userId\":\"" + userId + "\",\"documentSlot\":" + slot + "}";
    }

    private static String uploadBody(String userId) {
        return "{\"userId\":\"" + userId + "\","
                + "\"documentSlot\":1,"
                + "\"documentName\":\"passport.pdf\","
                + "\"base64\":\"JVBERi0xLjQK\","
                + "\"expiresOn\":\"" + LocalDate.now().plusDays(90) + "\"}";
    }

    // ---------- auth, exercised through the metadata endpoint ----------

    @Test
    void metadataRejectsMalformedToken() throws Exception {
        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", "garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void metadataRejectsMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(post(METADATA_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void metadataRejectsRequestForAnotherUsersDocument() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);

        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OTHER, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void metadataRejectsEmailNotMappedToAUser() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(null);

        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminMayReadAnotherUsersMetadata() throws Exception {
        when(volunteerService.getIdentityDocumentMetadata(OTHER, 1))
                .thenReturn(new IdentityDocumentMetadata(null, null, null));

        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OTHER, 1)))
                .andExpect(status().isOk());
    }

    // ---------- validation ----------

    @Test
    void rejectsSlotOutsideOneOrTwo() throws Exception {
        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 5)))
                .andExpect(status().isBadRequest());
    }

    // ---------- metadata behaviour ----------

    @Test
    void metadataReturns200WhenDocumentPresent() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);
        when(volunteerService.getIdentityDocumentMetadata(OWNER, 1))
                .thenReturn(new IdentityDocumentMetadata(null, null, null));

        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 1)))
                .andExpect(status().isOk());
    }

    @Test
    void metadataReturns204WhenSlotEmpty() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);
        when(volunteerService.getIdentityDocumentMetadata(OWNER, 2)).thenReturn(null);

        mockMvc.perform(post(METADATA_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 2)))
                .andExpect(status().isNoContent());
    }

    // ---------- upload ----------

    @Test
    void uploadReturns200OnSuccess() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);
        when(storage.uploadBase64(anyString(), anyInt(), anyString(), anyString(), any(), any()))
                .thenReturn(java.util.Map.of("message", "Identity document uploaded"));

        mockMvc.perform(post(UPLOAD_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody(OWNER)))
                .andExpect(status().isOk());
    }

    @Test
    void uploadRejectsRequestForAnotherUser() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);

        mockMvc.perform(post(UPLOAD_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadBody(OTHER)))
                .andExpect(status().isForbidden());
    }

    // ---------- view file ----------

    @Test
    void fileReturns200WithUrlWhenDocumentPresent() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);
        when(storage.presignedUrl(OWNER, 1, null))
                .thenReturn(Optional.of("https://example.s3.amazonaws.com/signed"));

        mockMvc.perform(post(FILE_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 1)))
                .andExpect(status().isOk());
    }

    @Test
    void fileReturns204WhenSlotEmpty() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);
        when(storage.presignedUrl(OWNER, 2, null)).thenReturn(Optional.empty());

        mockMvc.perform(post(FILE_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OWNER, 2)))
                .andExpect(status().isNoContent());
    }

    @Test
    void fileRejectsRequestForAnotherUser() throws Exception {
        when(userService.getUserIdByEmailForAuth(EMAIL)).thenReturn(OWNER);

        mockMvc.perform(post(FILE_URL)
                        .header("Authorization", volunteerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(OTHER, 1)))
                .andExpect(status().isForbidden());
    }
}