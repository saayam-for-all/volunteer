package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;

import java.net.URL;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import org.sfa.volunteer.exception.IdentityDocumentException;

@ExtendWith(MockitoExtension.class)
class IdentityDocumentStorageServiceTest {

    private static final String USER_ID = "SID-001";
    private static final String US_PATH = "s3://test-bucket-us/users/SID-001/identity/slot1";
    private static final String EU_PATH = "s3://test-bucket-eu/users/SID-001/identity/slot1";

    @Mock
    private VolunteerService volunteerService;
    @Mock
    private S3Client s3ClientUs;
    @Mock
    private S3Client s3ClientEu;
    @Mock
    private S3Presigner s3PresignerUs;
    @Mock
    private S3Presigner s3PresignerEu;

    private IdentityDocumentStorageService service;

    private String validBase64;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        service = new IdentityDocumentStorageService(
                volunteerService, s3ClientUs, s3ClientEu, s3PresignerUs, s3PresignerEu);

        ReflectionTestUtils.setField(service, "maxBytes", 2097152L);
        ReflectionTestUtils.setField(service, "allowedMimeCsv", "image/jpeg,image/png,application/pdf");
        ReflectionTestUtils.setField(service, "keyPattern", "users/%s/identity/slot%d");
        ReflectionTestUtils.setField(service, "euBucket", "test-bucket-eu");
        ReflectionTestUtils.setField(service, "usBucket", "test-bucket-us");
        ReflectionTestUtils.setField(service, "presignMinutes", 5L);

        byte[] pngBytes = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
             0x00, 0x00, 0x00, 0x00                                 // padding to reach 12 bytes
        };
        validBase64 = Base64.getEncoder().encodeToString(pngBytes);
        futureDate = LocalDate.now().plusDays(90);
    }

    // ---------- upload validation ----------

    @Test
    void rejectsNullExpiry() {
        IdentityDocumentException ex = assertThrows(IdentityDocumentException.class,
                () -> service.uploadBase64(USER_ID, 1, "passport.png", validBase64, null, "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getHttpStatus());
        assertEquals(SaayamStatusCode.INVALID_DOCUMENT_EXPIRY, ex.getStatusCode());
    }

    @Test
    void rejectsPastExpiry() {
        IdentityDocumentException ex = assertThrows(IdentityDocumentException.class,
                () -> service.uploadBase64(USER_ID, 1, "passport.png", validBase64,
                        LocalDate.now().minusDays(1), "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getHttpStatus());
        assertEquals(SaayamStatusCode.INVALID_DOCUMENT_EXPIRY, ex.getStatusCode());
    }

    @Test
    void rejectsUnsupportedFileType() {
        byte[] gifBytes = new byte[]{
            'G', 'I', 'F', '8', '9', 'a',
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00   // pad past the 12-byte minimum
        };
        String badBase64 = Base64.getEncoder().encodeToString(gifBytes);

        IdentityDocumentException ex = assertThrows(IdentityDocumentException.class,
                () -> service.uploadBase64(USER_ID, 1, "sneaky.png", badBase64, futureDate, "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getHttpStatus());
        assertEquals(SaayamStatusCode.INVALID_DOCUMENT_TYPE, ex.getStatusCode());
    }

    @Test
    void rejectsBlankDocumentName() {
        IdentityDocumentException ex = assertThrows(IdentityDocumentException.class,
                () -> service.uploadBase64(USER_ID, 1, "   ", validBase64, futureDate, "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getHttpStatus());
        assertEquals(SaayamStatusCode.DOCUMENT_NAME_REQUIRED, ex.getStatusCode());
    }

    // ---------- resolveStored, exercised through download ----------

    @Test
    void downloadReturnsEmptyWhenNoPathStored() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(null);

        assertTrue(service.download(USER_ID, 1, "us-east-1").isEmpty());
    }

    @Test
    void downloadReturnsEmptyWhenPathIsBlank() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn("   ");

        assertTrue(service.download(USER_ID, 1, "us-east-1").isEmpty());
    }

    @Test
    void downloadRejectsStoredUriWithoutAKey() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn("s3://bucket-only");

        assertThrows(IdentityDocumentException.class,
                () -> service.download(USER_ID, 1, "us-east-1"));
    }

    @Test
    void downloadReturnsBytesAndContentType() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);

        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("application/pdf").build(),
                new byte[]{1, 2, 3});
        when(s3ClientUs.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(response);

        var result = service.download(USER_ID, 1, "us-east-1");

        assertTrue(result.isPresent());
        assertEquals("application/pdf", result.get().contentType());
    }

    @Test
    void downloadFallsBackToOctetStreamWhenContentTypeMissing() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);

        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(), new byte[]{1});
        when(s3ClientUs.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(response);

        assertEquals("application/octet-stream",
                service.download(USER_ID, 1, "us-east-1").get().contentType());
    }

    @Test
    void downloadReturnsEmptyWhenObjectMissingInS3() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);
        when(s3ClientUs.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        assertTrue(service.download(USER_ID, 1, "us-east-1").isEmpty());
    }

    /** With no region hint, the region is derived from the bucket in the stored URI. */
    @Test
    void downloadUsesEuClientWhenStoredInEuBucket() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(EU_PATH);

        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("image/png").build(), new byte[]{1});
        when(s3ClientEu.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(response);

        service.download(USER_ID, 1, null);

        verify(s3ClientEu).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void downloadRequestsTheStoredBucketAndKey() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);

        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("application/pdf").build(),
                new byte[]{1});

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        when(s3ClientUs.getObjectAsBytes(captor.capture())).thenReturn(response);

        service.download(USER_ID, 1, "us-east-1");

        assertEquals("test-bucket-us", captor.getValue().bucket());
        assertEquals("users/SID-001/identity/slot1", captor.getValue().key());
    }

    // ---------- presignedUrl ----------

    @Test
    void presignedUrlIsEmptyWhenNoPathStored() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(null);

        assertTrue(service.presignedUrl(USER_ID, 1, "us-east-1").isEmpty());
    }

    @Test
    void presignedUrlReturnsSignedUrl() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/signed"));
        when(s3PresignerUs.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presigned);

        Optional<String> url = service.presignedUrl(USER_ID, 1, "us-east-1");

        assertTrue(url.isPresent());
        assertEquals("https://example.com/signed", url.get());
    }

    @Test
    void presignedUrlUsesEuPresignerForEuBucket() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(EU_PATH);

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/eu-signed"));
        when(s3PresignerEu.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presigned);

        service.presignedUrl(USER_ID, 1, null);

        verify(s3PresignerEu).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void presignedUrlUsesConfiguredTtl() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/signed"));

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        when(s3PresignerUs.presignGetObject(captor.capture())).thenReturn(presigned);

        service.presignedUrl(USER_ID, 1, "us-east-1");

        assertEquals(Duration.ofMinutes(5), captor.getValue().signatureDuration());
    }

    @Test
    void presignedUrlSignsTheStoredBucketAndKey() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(US_PATH);

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/signed"));

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        when(s3PresignerUs.presignGetObject(captor.capture())).thenReturn(presigned);

        service.presignedUrl(USER_ID, 1, "us-east-1");

        GetObjectRequest signed = captor.getValue().getObjectRequest();
        assertEquals("test-bucket-us", signed.bucket());
        assertEquals("users/SID-001/identity/slot1", signed.key());
    }
    // ---------- upload: safe replacement (DR-02, FR-09, FR-12) ----------

    @Test
    void firstUploadWritesUniqueKeyAndUpdatesRecordOnce() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1)).thenReturn(null);

        service.uploadBase64(USER_ID, 1, "passport.png", validBase64, futureDate, null);

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3ClientUs).putObject(put.capture(), any(RequestBody.class));
        String key = put.getValue().key();
        assertTrue(key.startsWith("users/SID-001/identity/slot1-"));

        verify(volunteerService).replaceGovtIdDocument(
                USER_ID, 1, "s3://test-bucket-us/" + key, "passport.png", futureDate);
        verify(volunteerService, never()).updateGovtIdPath(anyString(), anyInt(), anyString());
        verify(s3ClientUs, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void databaseFailureDeletesNewFileAndKeepsOldOne() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1))
                .thenReturn(US_PATH);

            doThrow(new RuntimeException("db down"))
                    .when(volunteerService)
                    .replaceGovtIdDocument(any(), anyInt(), any(), any(), any());

        assertThrows(IdentityDocumentException.class,
                () -> service.uploadBase64(
                        USER_ID, 1, "passport.png",
                        validBase64, futureDate, null));

        ArgumentCaptor<PutObjectRequest> put =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        ArgumentCaptor<DeleteObjectRequest> del =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);

        InOrder order = inOrder(s3ClientUs, volunteerService);

        order.verify(s3ClientUs)
                .putObject(put.capture(), any(RequestBody.class));

        order.verify(volunteerService)
                .replaceGovtIdDocument(any(), anyInt(), any(), any(), any());

        order.verify(s3ClientUs).deleteObject(del.capture());

        assertEquals(put.getValue().key(), del.getValue().key());

        assertNotEquals("users/SID-001/identity/slot1",
                del.getValue().key());

        verify(s3ClientUs).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void successfulReplacementDeletesOldFileAfterDatabaseUpdate()
        throws Exception {

        when(volunteerService.getGovtIdPath(USER_ID, 1))
                .thenReturn(US_PATH);

        service.uploadBase64(
                USER_ID, 1, "license.png",
                validBase64, futureDate, null);

        ArgumentCaptor<PutObjectRequest> put =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        ArgumentCaptor<DeleteObjectRequest> del =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);

        InOrder order = inOrder(s3ClientUs, volunteerService);

        order.verify(s3ClientUs)
                .putObject(put.capture(), any(RequestBody.class));

        String newKey = put.getValue().key();

        order.verify(volunteerService)
                .replaceGovtIdDocument(
                        USER_ID, 1,
                        "s3://test-bucket-us/" + newKey,
                        "license.png", futureDate);

        order.verify(s3ClientUs).deleteObject(del.capture());

        assertEquals("users/SID-001/identity/slot1",
                del.getValue().key());

        assertEquals("test-bucket-us", del.getValue().bucket());

        assertNotEquals(newKey, del.getValue().key());

        verify(s3ClientUs).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void s3UploadFailureKeepsOldDocument() throws Exception {
        when(volunteerService.getGovtIdPath(USER_ID, 1))
                .thenReturn(US_PATH);

        doThrow(S3Exception.builder().message("S3 upload failed").build())
                .when(s3ClientUs)
                .putObject(any(PutObjectRequest.class),
                        any(RequestBody.class));

        assertThrows(IdentityDocumentException.class,
                () -> service.uploadBase64(
                        USER_ID, 1, "license.png",
                        validBase64, futureDate, null));

                verify(volunteerService, never())
                .replaceGovtIdDocument(
                        any(), anyInt(), any(), any(), any());

            verify(s3ClientUs, never())
                    .deleteObject(any(DeleteObjectRequest.class));
    }
}
