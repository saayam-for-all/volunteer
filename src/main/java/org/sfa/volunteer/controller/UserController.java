package org.sfa.volunteer.controller;
import com.amazonaws.services.lambda.runtime.events.S3ObjectLambdaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/0.0.1/users")
public class UserController {
    private final UserService userService;
    private final ResponseBuilder responseBuilder;
    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Value("${cors.allowed.origin}")
    private String allowedOrigin;
    @Value("${cors.allowed.credentials}")
    private String allowedCredentials;

    @Autowired
    public UserController(UserService userService, ResponseBuilder responseBuilder) {
        this.userService = userService;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping
    public SaayamResponse<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userService.createUser(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_CREATED, response);
    }

    @GetMapping
    public SaayamResponse<PaginationResponse<UserProfileResponse>> getUsersWithPagination(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        PaginationResponse<UserProfileResponse> response = userService.findAllUsersWithPagination(page, size);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, response);
    }

    @GetMapping("/profile")
    public SaayamResponse<UserProfileResponse> getUserProfileByEmail(@Valid @RequestBody CreateUserRequest request) {
        String email = request.email().toString();
        UserProfileResponse response = userService.getUserProfileByEmail(email);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, response);
    }

    @GetMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        UserProfileResponse response = userService.getUserProfileById(userId);

        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

//    @GetMapping("/login/{email}")
//    public SaayamResponse<UserProfileResponse> getUserProfileAfterLogin(@PathVariable String email) {
//        UserProfileResponse response = userService.getUserProfileByEmail(email);
//        String profilepath = response.profilePicturePath();
//        if (profilepath != null) {
//            // String presignedUrl = userService.generatePresignedUrl(bucketName, profilepath);
//            String imageProxyUrl = "/api/profile-image/" + URLEncoder.encode(profilepath, StandardCharsets.UTF_8);
//
//            System.out.println("imageProxyUrl is :  " + imageProxyUrl);
//            UserProfileResponse updatedResponse = response.toBuilder()
//                    .profileImageUrl(imageProxyUrl)
//                    .build();
//            return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, updatedResponse);
//        }
//        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, response);
//
//    }

    @GetMapping("/login/{email}")
    public SaayamResponse<UserProfileResponse> getUserProfileAfterLogin(@PathVariable String email) {
        UserProfileResponse response = userService.getUserProfileByEmail(email);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, response);
    }

//    @GetMapping("/api/profile-image/**")
//    public ResponseEntity<byte[]> getProfileImage(HttpServletRequest request) {
//        try {
//            System.out.println("Decoded S3");
//            String basePath = "/api/profile-image/";
//            String encodedUrl = request.getRequestURI().substring(request.getRequestURI().indexOf(basePath) + basePath.length());
//            String decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
//
//            // ✅ Use Java URI to extract path from full URL
//            URI uri = new URI(decodedUrl);
//            String key = uri.getPath().substring(1); // Remove leading "/"
//
//            System.out.println("Decoded S3 key: " + key);
//
//            // ✅ Generate presigned URL using the object key
//            String presignedUrl = userService.generatePresignedUrl(bucketName, key);
//
//            // ✅ Fetch the image using presigned URL
//            URL url = new URL(presignedUrl);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("GET");
//
//            InputStream is = conn.getInputStream();
//            byte[] imageBytes = is.readAllBytes();
//            is.close();
//
//            String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(imageBytes));
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
//
//            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }



    @PutMapping(value = "/profile/{userId}")
    public SaayamResponse<UserProfileResponse> updateUserProfile(
            @PathVariable String userId,
            @RequestParam("ProfileRequest") String ProfileRequestJson,
            @RequestParam("profileImg") MultipartFile profileImg
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        UpdateUserProfileRequest request = objectMapper.readValue(ProfileRequestJson, UpdateUserProfileRequest.class);
        UserProfileResponse response = userService.updateUserProfile(userId, request, profileImg);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_ACCOUNT_UPDATED, new Object[]{userId}, response);
    }
}
