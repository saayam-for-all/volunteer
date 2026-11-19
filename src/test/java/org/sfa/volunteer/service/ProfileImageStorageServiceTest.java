package org.sfa.volunteer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private S3Client usS3;
    @Mock
    private S3Client euS3;

    private ProfileImageStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new ProfileImageStorageService(userService, usS3, euS3);
        ReflectionTestUtils.setField(storageService, "usBucket", "saayam-virginia-private");
        ReflectionTestUtils.setField(storageService, "euBucket", "saayam-ireland-private");
        ReflectionTestUtils.setField(storageService, "maxBytes", 5L * 1024 * 1024);
        ReflectionTestUtils.setField(storageService, "allowedMimeCsv", "image/jpeg,image/png,image/webp");
        ReflectionTestUtils.setField(storageService, "keyPattern", "users/%s/profile.jpg");
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

        assertThat(response).containsEntry("s3Uri", "s3://saayam-virginia-private/users/user-1/profile.jpg");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(usS3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("saayam-virginia-private");
        assertThat(request.getValue().key()).isEqualTo("users/user-1/profile.jpg");
        verifyNoInteractions(euS3);
        verify(userService).setProfilePicturePath(
                eq("user-1"), eq("s3://saayam-virginia-private/users/user-1/profile.jpg"));
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
                .satisfies(
                        error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(413))
                .hasMessageContaining("Max upload size is 5 MB");

        verifyNoInteractions(usS3, euS3);
        verify(userService, never()).setProfilePicturePath(any(), any());
    }

    @Test
    void uploadBase64RoutesEuRegionToEuBucketOnly() {
        when(userService.userExists("user-1")).thenReturn(true);
        byte[] png = pngBytes(1024);

        var response = storageService.uploadBase64(
                "user-1",
                "image/png",
                Base64.getEncoder().encodeToString(png),
                "eu-west-1");

        assertThat(response).containsEntry("s3Uri", "s3://saayam-ireland-private/users/user-1/profile.jpg");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(euS3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("saayam-ireland-private");
        verifyNoInteractions(usS3);

        verify(userService).setProfilePicturePath(
                eq("user-1"), eq("s3://saayam-ireland-private/users/user-1/profile.jpg"));
    }

    @Test
    void uploadBase64DoesNotSavePathWhenS3UploadFails() {
        when(userService.userExists("user-1")).thenReturn(true);
        when(usS3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 unavailable").build());
        byte[] png = pngBytes(1024);

        assertThatThrownBy(() -> storageService.uploadBase64(
                "user-1",
                "image/png",
                Base64.getEncoder().encodeToString(png),
                "us-east-1"))
                .isInstanceOf(RuntimeException.class);

        verify(userService, never()).setProfilePicturePath(any(), any());
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
