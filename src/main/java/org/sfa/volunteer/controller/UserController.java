package org.sfa.volunteer.controller;
import com.amazonaws.services.lambda.runtime.events.S3ObjectLambdaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/0.0.1/users")
public class UserController {
    private final UserService userService;
    private final ResponseBuilder responseBuilder;
    @Value("${aws.s3.bucket}")
    private String bucketName;
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

    @GetMapping("/login/{email}")
    public SaayamResponse<UserProfileResponse> getUserProfileAfterLogin(@PathVariable String email) {
        UserProfileResponse response = userService.getUserProfileByEmail(email);
        String profilepath = response.profilePicturePath();
        if (profilepath != null) {
            String presignedUrl = userService.generatePresignedUrl(bucketName, profilepath);
            System.out.println("presigned URL is :  " + presignedUrl);
            UserProfileResponse updatedResponse = response.toBuilder()
                    .profileImageUrl(presignedUrl)
                    .build();
            return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, updatedResponse);
        }
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, response);

    }

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
