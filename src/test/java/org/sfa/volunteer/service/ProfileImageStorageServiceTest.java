package org.sfa.volunteer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileImageStorageServiceTest {

    private UserService userService;
    private ProfileImageStorageService storageService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        storageService = new ProfileImageStorageService(userService, mock(S3Client.class), mock(S3Client.class));
        ReflectionTestUtils.setField(storageService, "usBucket", "saayam-us-private");
        ReflectionTestUtils.setField(storageService, "euBucket", "saayam-eu-private");
        ReflectionTestUtils.setField(storageService, "maxBytes", 5L * 1024 * 1024);
        ReflectionTestUtils.setField(storageService, "allowedMimeCsv", "image/jpeg,image/png,image/webp");
        ReflectionTestUtils.setField(storageService, "keyPattern", "users/%s/profile");
    }

    @Test
    void uploadBase64AllowsImagesUpToFiveMegabytesBeforeS3Upload() {
        when(userService.userExists("user-1")).thenReturn(true);
        byte[] png = pngBytes(5 * 1024 * 1024);

        var response = storageService.uploadBase64(
                "user-1",
                "image/png",
                Base64.getEncoder().encodeToString(png),
                "us-east-1");

        assertThat(response).containsEntry("s3Uri", "s3://saayam-us-private/users/user-1/profile");
        verify(userService).setProfilePicturePath(eq("user-1"), eq("s3://saayam-us-private/users/user-1/profile"));
    }

    @Test
    void uploadBase64RejectsImagesLargerThanFiveMegabytes() {
        when(userService.userExists("user-1")).thenReturn(true);
        byte[] png = pngBytes((5 * 1024 * 1024) + 1);

        assertThatThrownBy(() -> storageService.uploadBase64(
                "user-1",
                "image/png",
                Base64.getEncoder().encodeToString(png),
                "us-east-1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(413))
                .hasMessageContaining("Max upload size is 5 MB");
    }

    private static byte[] pngBytes(int size) {
        byte[] bytes = new byte[size];
        bytes[0] = (byte) 0x89;
        bytes[1] = 0x50;
        bytes[2] = 0x4E;
        bytes[3] = 0x47;
        bytes[4] = 0x0D;
        bytes[5] = 0x0A;
        bytes[6] = 0x1A;
        bytes[7] = 0x0A;
        return bytes;
    }
}
