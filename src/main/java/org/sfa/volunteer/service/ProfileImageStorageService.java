package org.sfa.volunteer.service;

import org.sfa.volunteer.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProfileImageStorageService {

    private final UserService userService;
    private final S3Client s3ClientUs;
    private final S3Client s3ClientEu;

    @Value("${saayam.s3.buckets.euPrivate}")
    private String euBucket;

    @Value("${saayam.s3.buckets.usPrivate}")
    private String usBucket;

    @Value("${saayam.s3.maxBytes:2097152}")
    private long maxBytes;

    @Value("${saayam.s3.allowedMime:image/jpeg,image/png,image/webp}")
    private String allowedMimeCsv;

    @Value("${saayam.s3.keyPattern:users/%s/profile}")
    private String keyPattern;

    public ProfileImageStorageService(
            UserService userService,
            @Qualifier("s3ClientUs") S3Client s3ClientUs,
            @Qualifier("s3ClientEu") S3Client s3ClientEu
    ) {
        this.userService = userService;
        this.s3ClientUs = s3ClientUs;
        this.s3ClientEu = s3ClientEu;
    }

    public Map<String, Object> uploadBase64(String userId, String contentType, String base64, String regionHint) {
        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        if (base64 == null || base64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "base64 is required");
        }

        String cleaned = stripDataUrlPrefix(base64);

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base64");
        }
        String detected = detectMime(bytes);
        if (detected == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image type. Allowed: image/jpeg, image/png, image/webp");
        }
        validate(detected, bytes.length);

        String effectiveRegion = (regionHint == null || regionHint.isBlank()) ? "us-east-1" : regionHint;
        String bucket = pickBucket(effectiveRegion);
        String key = buildKey(userId);

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
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload to S3", e);
        }

        String s3Uri = "s3://" + bucket + "/" + key;
        userService.setProfilePicturePath(userId, s3Uri);

        return Map.of(
                "message", "Profile image uploaded",
                "userId", userId,
                "s3Uri", s3Uri,
                "bucket", bucket,
                "key", key
        );
    }

    public void delete(String userId, String regionHint) {
        if (!userService.userExists(userId)) {
            throw new UserNotFoundException(userId);
        }

        var uriOpt = userService.getProfilePicturePath(userId);
        if (uriOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile image not found");
        }

        URI uri = URI.create(uriOpt.get());
        String ssp = uri.getSchemeSpecificPart();
        if (ssp.startsWith("//")) ssp = ssp.substring(2);

        int slash = ssp.indexOf('/');
        if (slash <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid profile picture URI");
        }

        String bucket = ssp.substring(0, slash);
        String key = ssp.substring(slash + 1);

        String effectiveRegion = (regionHint == null || regionHint.isBlank())
                ? (bucket.equals(euBucket) ? "eu-west-1" : "us-east-1")
                : regionHint;

        S3Client client = pickClient(effectiveRegion);

        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile image not found");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify profile image", e);
        }

        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete profile image", e);
        }

        userService.setProfilePicturePath(userId, null);
    }

    public Optional<DownloadedImage> download(String userId, String regionHint) {

        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }

        var uriOpt = userService.getProfilePicturePath(userId);
        if (uriOpt.isEmpty()) return Optional.empty();

        URI uri = URI.create(uriOpt.get());
        String ssp = uri.getSchemeSpecificPart();
        if (ssp.startsWith("//")) ssp = ssp.substring(2);

        int slash = ssp.indexOf('/');
        if (slash <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid profile picture URI");
        }

        String bucket = ssp.substring(0, slash);
        String key = ssp.substring(slash + 1);

        String effectiveRegion = (regionHint == null || regionHint.isBlank())
                ? (bucket.equals(euBucket) ? "eu-west-1" : "us-east-1")
                : regionHint;

        try {
            ResponseBytes<GetObjectResponse> obj = pickClient(effectiveRegion)
                    .getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());

            String contentType = obj.response().contentType();
            if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

            return Optional.of(new DownloadedImage(contentType, obj.asByteArray()));

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            return Optional.empty();
        }
    }

    public record DownloadedImage(String contentType, byte[] bytes) {}

    // ---------------- helpers ----------------

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image type. Allowed: " + String.join(", ", allowed));
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (size > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Max upload size is " + (maxBytes / (1024 * 1024)) + " MB");
        }
    }

    private String buildKey(String userId) {
        return String.format(keyPattern, userId);
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
    private String detectMime(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}