package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetIdentityDocumentRequest;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.dto.request.UploadIdentityDocumentRequest;
import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.service.IdentityDocumentStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.JwtClaims;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/0.0.1/volunteers")
public class IdentityDocumentController {

    private final IdentityDocumentStorageService storage;
    private final UserService userService;
    private final VolunteerService volunteerService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public IdentityDocumentController(IdentityDocumentStorageService storage,
                                      UserService userService,
                                      VolunteerService volunteerService,
                                      ResponseBuilder responseBuilder) {
        this.storage = storage;
        this.userService = userService;
        this.volunteerService = volunteerService;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping("/identity-document/metadata")
    public ResponseEntity<SaayamResponse<IdentityDocumentMetadata>> getMetadata(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody GetIdentityDocumentRequest request) throws Exception {

        authorizeUser(JwtClaims.parse(authorization), request.userId());

        IdentityDocumentMetadata metadata =
                volunteerService.getIdentityDocumentMetadata(request.userId(), request.documentSlot());

        return metadata == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, metadata));
    }
    
    @PostMapping("/identity-document/upload")
    public SaayamResponse<Map<String, Object>> upload(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody UploadIdentityDocumentRequest request) {

    	authorizeUser(JwtClaims.parse(authorization), request.userId());

    	Map<String, Object> result = storage.uploadBase64(
            request.userId(), request.documentSlot(), request.documentName(),
            request.base64(), request.expiresOn(), request.regionHint());

    	return responseBuilder.buildSuccessResponse(
            SaayamStatusCode.IDENTITY_DOCUMENT_UPLOADED, result);
    }
    
   @PostMapping("/identity-document/file")
   public ResponseEntity<SaayamResponse<Map<String, String>>> viewFile(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody GetIdentityDocumentRequest request) {

    authorizeUser(JwtClaims.parse(authorization), request.userId());

    Optional<String> url = storage.presignedUrl(request.userId(), request.documentSlot(), null);

    return url.isEmpty()
            ? ResponseEntity.noContent().build()
            : ResponseEntity.ok(responseBuilder.buildSuccessResponse(
                    SaayamStatusCode.SUCCESS, Map.of("url", url.get())));
    }

    private void authorizeUser(JwtClaims claims, String targetUserId) {
        if (isAdmin(claims.groups())) return;

        if (claims.email() == null || claims.email().isBlank()) {
            throw new ForbiddenException("JWT email missing");
        }

        String callerSid;
        try {
            callerSid = userService.getUserIdByEmailForAuth(claims.email());
        } catch (Exception e) {
            throw new ForbiddenException("JWT user not mapped to DB user");
        }
        if (callerSid == null || callerSid.isBlank()) {
            throw new ForbiddenException("JWT user not mapped to DB user");
        }
        if (!callerSid.equals(targetUserId)) {
            throw new ForbiddenException("Not allowed");
        }
    }

    private boolean isAdmin(String groups) {
        if (groups == null || groups.isBlank()) return false;
        for (String g : groups.split(",")) {
            if (g.trim().equalsIgnoreCase("admin")) return true;
        }
        return false;
    }
}
