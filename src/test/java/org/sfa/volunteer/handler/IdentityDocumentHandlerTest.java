
package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.exception.IdentityDocumentException;
import org.sfa.volunteer.service.IdentityDocumentStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityDocumentHandlerTest {

    private static final String BASE =
            "/0.0.1/volunteers/identity-document";

    private static final String USER_ID = "SID-001";
    private static final String EMAIL = "owner@example.org";

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR =
            VALIDATOR_FACTORY.getValidator();

    @Mock
    private UserService userService;

    @Mock
    private VolunteerService volunteerService;

    @Mock
    private IdentityDocumentStorageService storage;

    @Mock
    private MessageSourceUtil messages;

    private IdentityDocumentHandler handler;

    @BeforeEach
    void setUp() {
        // Real ResponseBuilder with a mocked message provider.
        lenient().when(messages.getMessage(
                anyString(),
                nullable(Object[].class)
        )).thenReturn("Test message");

        ResponseBuilder responseBuilder =
                new ResponseBuilder(messages);

        handler = new IdentityDocumentHandler(
                userService,
                volunteerService,
                storage,
                responseBuilder,
                messages,
                VALIDATOR
        );
    }

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    private APIGatewayProxyRequestEvent request(
            String method,
            String path,
            String body) {

        APIGatewayProxyRequestEvent event =
                new APIGatewayProxyRequestEvent()
                        .withHttpMethod(method)
                        .withPath(path)
                        .withBody(body);

        APIGatewayProxyRequestEvent.ProxyRequestContext context =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();

        context.setAuthorizer(Map.of(
                "claims",
                Map.of(
                        "email", EMAIL,
                        "cognito:groups", "volunteer"
                )
        ));

        event.setRequestContext(context);

        return event;
    }

    private String documentRequest() {
        return """
                {
                  "userId": "SID-001",
                  "documentSlot": 1
                }
                """;
    }

    private String uploadRequest() {
        String expiry = LocalDate.now()
                .plusYears(1)
                .toString();

        return """
                {
                  "userId": "SID-001",
                  "documentSlot": 1,
                  "documentName": "passport.pdf",
                  "base64": "JVBERi0xLjQ=",
                  "expiresOn": "%s",
                  "regionHint": "us-east-1"
                }
                """.formatted(expiry);
    }

    private void allowOwner() {
        when(userService.isEmailOwnedByUser(
                USER_ID, EMAIL
        )).thenReturn(true);
    }

    private APIGatewayProxyResponseEvent invoke(
            String method,
            String path,
            String body) {

        return handler.handleRequest(
                request(method, path, body),
                null
        );
    }

    // ----------------------------------
    // METADATA TESTS
    // ----------------------------------

    @Test
    void metadataReturns200WhenDocumentExists() throws Exception {
        allowOwner();

        IdentityDocumentMetadata metadata =
                new IdentityDocumentMetadata(
                        "passport.pdf",
                        LocalDate.now().plusYears(1),
                        null
                );

        when(volunteerService.getIdentityDocumentMetadata(
                USER_ID, 1
        )).thenReturn(metadata);

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                documentRequest()
        );

        assertEquals(200, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().contains("passport.pdf"));

        verify(volunteerService)
                .getIdentityDocumentMetadata(USER_ID, 1);
    }

    @Test
    void metadataReturns204WhenMissing() throws Exception {
        allowOwner();

        when(volunteerService.getIdentityDocumentMetadata(
                USER_ID, 1
        )).thenReturn(null);

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                documentRequest()
        );

        assertEquals(204, result.getStatusCode());
        assertNull(result.getBody());
    }

    // ----------------------------------
    // UPLOAD TEST
    // ----------------------------------

    @Test
    void uploadReturns200() {
        allowOwner();

        when(storage.uploadBase64(
                eq(USER_ID),
                eq(1),
                eq("passport.pdf"),
                eq("JVBERi0xLjQ="),
                any(LocalDate.class),
                eq("us-east-1")
        )).thenReturn(Map.of(
                "message", "Identity document uploaded",
                "userId", USER_ID
        ));

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/upload",
                uploadRequest()
        );

        assertEquals(200, result.getStatusCode());
        assertTrue(result.getBody()
                .contains("Identity document uploaded"));

        verify(storage).uploadBase64(
                eq(USER_ID),
                eq(1),
                eq("passport.pdf"),
                anyString(),
                any(LocalDate.class),
                eq("us-east-1")
        );
    }

    // ----------------------------------
    // FILE URL TESTS
    // ----------------------------------

    @Test
    void fileReturns200WithPresignedUrl() {
        allowOwner();

        when(storage.presignedUrl(
                USER_ID, 1, null
        )).thenReturn(Optional.of(
                "https://example.org/presigned-test-url"
        ));

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/file",
                documentRequest()
        );

        assertEquals(200, result.getStatusCode());
        assertTrue(result.getBody()
                .contains("presigned-test-url"));
    }

    @Test
    void fileReturns204WhenMissing() {
        allowOwner();

        when(storage.presignedUrl(
                USER_ID, 1, null
        )).thenReturn(Optional.empty());

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/file",
                documentRequest()
        );

        assertEquals(204, result.getStatusCode());
        assertNull(result.getBody());
    }

    // ----------------------------------
    // AUTHORIZATION TESTS
    // ----------------------------------

    @Test
    void missingAuthorizerReturns403() {
        APIGatewayProxyRequestEvent event = request(
                "POST",
                BASE + "/metadata",
                documentRequest()
        );

        event.setRequestContext(null);

        APIGatewayProxyResponseEvent result =
                handler.handleRequest(event, null);

        assertEquals(403, result.getStatusCode());

        verifyNoInteractions(
                userService, volunteerService, storage
        );
    }

    @Test
    void nonOwnerReturns403() {
        when(userService.isEmailOwnedByUser(
                USER_ID, EMAIL
        )).thenReturn(false);

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                documentRequest()
        );

        assertEquals(403, result.getStatusCode());

        verifyNoInteractions(
                volunteerService, storage
        );
    }

    @Test
    void adminCanAccessWithoutOwnershipLookup()
            throws Exception {

        APIGatewayProxyRequestEvent event = request(
                "POST",
                BASE + "/metadata",
                documentRequest()
        );

        event.getRequestContext().setAuthorizer(Map.of(
                "claims",
                Map.of(
                        "email", "admin@example.org",
                        "cognito:groups", "admin"
                )
        ));

        when(volunteerService.getIdentityDocumentMetadata(
                USER_ID, 1
        )).thenReturn(null);

        APIGatewayProxyResponseEvent result =
                handler.handleRequest(event, null);

        assertEquals(204, result.getStatusCode());

        verifyNoInteractions(userService);
        verify(volunteerService)
                .getIdentityDocumentMetadata(USER_ID, 1);
    }

    // ----------------------------------
    // VALIDATION TESTS
    // ----------------------------------

    @Test
    void invalidDocumentSlotReturns400() {
        String body = """
                {
                  "userId": "SID-001",
                  "documentSlot": 3
                }
                """;

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                body
        );

        assertEquals(400, result.getStatusCode());
        verifyNoInteractions(volunteerService, storage);
    }

    @Test
    void missingUserIdReturns400() {
        String body = """
                {
                  "documentSlot": 1
                }
                """;

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                body
        );

        assertEquals(400, result.getStatusCode());
    }

    @Test
    void expiredUploadDateReturns400() {
        String body = """
                {
                  "userId": "SID-001",
                  "documentSlot": 1,
                  "documentName": "passport.pdf",
                  "base64": "JVBERi0xLjQ=",
                  "expiresOn": "2020-01-01"
                }
                """;

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/upload",
                body
        );

        assertEquals(400, result.getStatusCode());
        verifyNoInteractions(storage);
    }

    @Test
    void invalidJsonReturns400() {
        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                "{ invalid json }"
        );

        assertEquals(400, result.getStatusCode());
    }

    @Test
    void emptyBodyReturns400() {
        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/metadata",
                ""
        );

        assertEquals(400, result.getStatusCode());
    }

    // ----------------------------------
    // ROUTING TESTS
    // ----------------------------------

    @Test
    void unsupportedMethodReturns405() {
        APIGatewayProxyResponseEvent result = invoke(
                "GET",
                BASE + "/metadata",
                documentRequest()
        );

        assertEquals(405, result.getStatusCode());
    }

    @Test
    void unknownRouteReturns404() {
        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/unknown",
                documentRequest()
        );

        assertEquals(404, result.getStatusCode());
    }

    @Test
    void optionsReturns204() {
        APIGatewayProxyResponseEvent result = invoke(
                "OPTIONS",
                BASE + "/upload",
                null
        );

        assertEquals(204, result.getStatusCode());

        assertNotNull(result.getHeaders());
        assertTrue(result.getHeaders()
                .containsKey("Access-Control-Allow-Origin"));
    }

    @Test
    void oversizedBodyReturns413() {
        String body = "x".repeat(5_000_100);

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/upload",
                body
        );

        assertEquals(413, result.getStatusCode());
        verifyNoInteractions(storage);
    }

    @Test
    void base64EncodedGatewayBodyIsAccepted()
            throws Exception {

        allowOwner();

        when(volunteerService.getIdentityDocumentMetadata(
                USER_ID, 1
        )).thenReturn(null);

        String encoded = Base64.getEncoder()
                .encodeToString(documentRequest()
                        .getBytes(StandardCharsets.UTF_8));

        APIGatewayProxyRequestEvent event = request(
                "POST",
                BASE + "/metadata",
                encoded
        );

        event.setIsBase64Encoded(true);

        APIGatewayProxyResponseEvent result =
                handler.handleRequest(event, null);

        assertEquals(204, result.getStatusCode());
    }

    // ----------------------------------
    // ERROR HANDLING
    // ----------------------------------

    @Test
    void storageExceptionPreservesHttpStatus() {
        allowOwner();

        when(storage.uploadBase64(
                eq(USER_ID),
                eq(1),
                anyString(),
                anyString(),
                any(LocalDate.class),
                any()
        )).thenThrow(new IdentityDocumentException(
                SaayamStatusCode.DOCUMENT_STORAGE_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "S3 upload failed"
        ));

        APIGatewayProxyResponseEvent result = invoke(
                "POST",
                BASE + "/upload",
                uploadRequest()
        );

        assertEquals(500, result.getStatusCode());
    }
}
