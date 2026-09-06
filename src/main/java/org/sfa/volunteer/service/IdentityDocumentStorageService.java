package org.sfa.volunteer.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.exception.IdentityDocumentException;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage for volunteer government identity documents.
 *
 * Modeled on {@link ProfileImageStorageService}. Two deliberate differences:
 * there is no delete method, and the accepted types are documents rather than
 * images.
 *
 * Replacement works by overwriting a deterministic key. Because S3 replaces the
 * object only when the PUT succeeds, there is no window in which a volunteer
 * has no document (DR-03), and no superseded object to clean up (DR-02).
 */
@Service
public class IdentityDocumentStorageService {

    private final VolunteerService volunteerService;
    private final S3Client s3ClientUs;
    private final S3Client s3ClientEu;
    private final S3Presigner s3PresignerUs;
    private final S3Presigner s3PresignerEu;

    @Value("${saayam.s3.buckets.euPrivate}")
    private String euBucket;

    @Value("${saayam.s3.buckets.usPrivate}")
    private String usBucket;
    
    @Value("${saayam.identity.presignMinutes:5}")
    private long presignMinutes;

    // Namespaced under saayam.identity rather than saayam.s3 so these do not
    // collide with the profile image settings, which share the saayam.s3 keys
    // but need different values.
    @Value("${saayam.identity.maxBytes:2097152}")
    private long maxBytes;

    @Value("${saayam.identity.allowedMime:image/jpeg,image/png,application/pdf}")
    private String allowedMimeCsv;

    @Value("${saayam.identity.keyPattern:users/%s/identity/slot%d}")
    private String keyPattern;

    public IdentityDocumentStorageService(
            VolunteerService volunteerService,
            @Qualifier("s3ClientUs") S3Client s3ClientUs,
            @Qualifier("s3ClientEu") S3Client s3ClientEu,
            @Qualifier("s3PresignerUs") S3Presigner s3PresignerUs,
            @Qualifier("s3PresignerEu") S3Presigner s3PresignerEu
    ) {
        this.volunteerService = volunteerService;
        this.s3ClientUs = s3ClientUs;
        this.s3ClientEu = s3ClientEu;
        this.s3PresignerUs = s3PresignerUs;
        this.s3PresignerEu = s3PresignerEu;
    }

    /**
     * Uploads or replaces the identity document in a slot.
     *
     * @param expiresOn the document's own expiry date - required, since every
     *                  page state on the Identity Document page is derived
     *                  from it
     */
    public Map<String, Object> uploadBase64(String userId,
                                            int documentSlot,
                                            String documentName,
                                            String base64,
                                            LocalDate expiresOn,
                                            String regionHint) {

        requireVolunteer(userId);
        validateSlot(documentSlot);

        if (base64 == null || base64.isBlank()) {
                    throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_FILE,
                    HttpStatus.BAD_REQUEST, "base64 is required");
        }
        String safeName = sanitizeDocumentName(documentName);
        validateExpiry(expiresOn);

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(stripDataUrlPrefix(base64));
        } catch (IllegalArgumentException e) {
                    throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_FILE,
                    HttpStatus.BAD_REQUEST, "Invalid base64");
        }

        // The bytes decide the type; a client-declared content type is not
        // trusted for a government identity document.
        String detected = detectMime(bytes);
        if (detected == null) {
                 throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_TYPE,
                    HttpStatus.BAD_REQUEST,
                    "Unsupported document type. Allowed: image/jpeg, image/png, application/pdf");
        }
        validate(detected, bytes.length);

        String effectiveRegion = (regionHint == null || regionHint.isBlank()) ? "us-east-1" : regionHint;
        String bucket = pickBucket(effectiveRegion);
        String key = buildKey(userId, documentSlot);

        try {
            pickClient(effectiveRegion).putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(detected)
                            .serverSideEncryption("AES256")
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
        } catch (S3Exception e) {
                throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_STORAGE_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload to S3", e);
        }

        String s3Uri = "s3://" + bucket + "/" + key;
        try {
            volunteerService.updateGovtIdPath(userId, documentSlot, s3Uri);
        } catch (Exception e) {
                throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_STORAGE_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not save identity document record", e);
       }

       try {
            volunteerService.updateGovtIdMetadata(userId, documentSlot, safeName, expiresOn);
        } catch (Exception e) {
                 throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_STORAGE_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not save identity document metadata", e);
        }

        return Map.of(
                "message", "Identity document uploaded",
                "userId", userId,
                "documentSlot", documentSlot,
                "documentName", safeName,
                "expiresOn", expiresOn.toString()
        );
    }

    
    public Optional<DownloadedDocument> download(String userId, int documentSlot, String regionHint) {
    	Optional<StoredObject> found = resolveStored(userId, documentSlot, regionHint);
        if (found.isEmpty()) return Optional.empty();
    	StoredObject obj = found.get();

    	try {
             ResponseBytes<GetObjectResponse> res = pickClient(obj.region())
                .getObjectAsBytes(GetObjectRequest.builder().bucket(obj.bucket()).key(obj.key()).build());

             String contentType = res.response().contentType();
             if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

             return Optional.of(new DownloadedDocument(contentType, res.asByteArray()));
        } catch (S3Exception e) {
            return Optional.empty();
        }
    }

    public record DownloadedDocument(String contentType, byte[] bytes) {}

    // ---------------- helpers ----------------

    private void requireVolunteer(String userId) {
        try {
            volunteerService.getVolunteerByUserId(userId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Volunteer not found", e);
        }
    }

    private record StoredObject(String bucket, String key, String region) {}

    private Optional<StoredObject> resolveStored(String userId, int documentSlot, String regionHint) {
    requireVolunteer(userId);
    validateSlot(documentSlot);

    	String stored;
    	try {
        	stored = volunteerService.getGovtIdPath(userId, documentSlot);
    	} catch (Exception e) {
                throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_STORAGE_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR, "Could not read identity document record", e);
    	}
    	if (stored == null || stored.isBlank()) {
            return Optional.empty();
    	}

    	URI uri = URI.create(stored);
    	String ssp = uri.getSchemeSpecificPart();
    	if (ssp.startsWith("//")) ssp = ssp.substring(2);

    	int slash = ssp.indexOf('/');
    	if (slash <= 0) {
           throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_STORAGE_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR, "Invalid identity document URI");
    	}

    	String bucket = ssp.substring(0, slash);
    	String key = ssp.substring(slash + 1);

    	String effectiveRegion = (regionHint == null || regionHint.isBlank())
            ? (bucket.equals(euBucket) ? "eu-west-1" : "us-east-1")
            : regionHint;

    	return Optional.of(new StoredObject(bucket, key, effectiveRegion));
    }

    private void validateSlot(int documentSlot) {
        if (documentSlot != 1 && documentSlot != 2) {
           throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_SLOT,
                    HttpStatus.BAD_REQUEST, "documentSlot must be 1 or 2");
        }
    }

    private void validateExpiry(LocalDate expiresOn) {
        if (expiresOn == null) {
            throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_EXPIRY, HttpStatus.BAD_REQUEST, "expiresOn is required");
        }
        // An already-expired document lands in the expired state on arrival and
        // immediately demands the replacement the volunteer just performed.
        if (expiresOn.isBefore(LocalDate.now())) {
           throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_EXPIRY,
                    HttpStatus.BAD_REQUEST, "expiresOn is in the past");
        }
    }

    private String sanitizeDocumentName(String documentName) {
        if (documentName == null || documentName.isBlank()) {
           throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_NAME_REQUIRED,
                    HttpStatus.BAD_REQUEST, "documentName is required");
        }
        String name = documentName.trim().replaceAll(".*[/\\\\]", "");
        if (name.isEmpty()) {
           throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_EXPIRY,
                    HttpStatus.BAD_REQUEST, "expiresOn is in the past");
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private String stripDataUrlPrefix(String base64) {
        int comma = base64.indexOf(',');
        if (comma > 0 && base64.substring(0, comma).contains("base64")) {
            return base64.substring(comma + 1);
        }
        return base64;
    }

    private void validate(String mime, long size) {
        String m = Optional.ofNullable(mime).orElse("").trim();
        int semi = m.indexOf(';');
        if (semi > -1) m = m.substring(0, semi).trim();

        List<String> allowed = Arrays.asList(allowedMimeCsv.split(","));
        if (!allowed.contains(m)) {
           throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_TYPE,
                    HttpStatus.BAD_REQUEST,
                    "Unsupported document type. Allowed: " + String.join(", ", allowed));
        }
        if (size <= 0) {
           throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_FILE,
                    HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (size > maxBytes) {
           throw new IdentityDocumentException(SaayamStatusCode.DOCUMENT_SIZE_EXCEEDED,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Max upload size is " + (maxBytes / (1024 * 1024)) + " MB");
        }
    }

    private String buildKey(String userId, int documentSlot) {
        return String.format(keyPattern, userId, documentSlot);
    }

    private boolean isEu(String regionHint) {
        return regionHint != null && regionHint.trim().equalsIgnoreCase("eu-west-1");
    }

    private String pickBucket(String regionHint) {
        String bucket = isEu(regionHint) ? euBucket : usBucket;
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket config missing: saayam.s3.buckets.*");
        }
        return bucket;
    }

    private S3Client pickClient(String regionHint) {
        return isEu(regionHint) ? s3ClientEu : s3ClientUs;
    }
    
    private S3Presigner pickPresigner(String regionHint) {
        return isEu(regionHint) ? s3PresignerEu : s3PresignerUs;
    }

    /**
     * Magic-byte sniffing. Narrower than the profile image version: WEBP is not
     * an accepted identity document type, and PDF is.
     */
    private String detectMime(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;

        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return "application/pdf";
        }
        return null;
    }
    public Optional<String> presignedUrl(String userId, int documentSlot, String regionHint) {
        return resolveStored(userId, documentSlot, regionHint).map(obj -> {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(obj.bucket()).key(obj.key()).build();
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .getObjectRequest(get)
                .build();
        return pickPresigner(obj.region()).presignGetObject(presign).url().toString();
    });
  }
}
