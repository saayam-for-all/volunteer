package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.FindUserProfileUsingEmail;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.SignOffRequest;
import org.sfa.volunteer.dto.response.AddressStatusResponse;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.SignOffResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.WizardStatusResponse;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/0.0.1/users")
public class UserController {

    private final UserService userService;
    private final ResponseBuilder responseBuilder;
    private final ProfileImageStorageService profileImageStorageService;
    private static final String HDR_REGION  = "X-Dev-Region";


    @Autowired
    public UserController(UserService userService, ResponseBuilder responseBuilder, ProfileImageStorageService profileImageStorageService) {
        this.userService = userService;
        this.responseBuilder = responseBuilder;
        this.profileImageStorageService = profileImageStorageService;
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

    @GetMapping("/profileByEmail/{email}")
    public SaayamResponse<UserProfileResponse> getUserProfileByEmail(@PathVariable("email") @Email @NotBlank String email) {
        UserProfileResponse profile = userService.getUserProfileByEmail(email);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, profile);
    }

    @PostMapping("/userIdByEmail")
    public SaayamResponse<UserIdResponse> getUserIdByEmail(@RequestBody FindUserProfileUsingEmail userEmail) {
        UserIdResponse email = userService.getUserIdByEmail(userEmail.email());
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userEmail}, email);
    }

    @GetMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        UserProfileResponse response = userService.getUserProfileById(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @GetMapping("/wizard/{userId}")
    public SaayamResponse<WizardStatusResponse> getWizardStatus(@PathVariable String userId) {
        WizardStatusResponse response = userService.getWizardStatus(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @GetMapping("/addressStatus/{userId}")
    public SaayamResponse<AddressStatusResponse> getAddressStatus(@PathVariable String userId) {
    	AddressStatusResponse response = userService.getAddressStatus(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    } 

    @GetMapping("/login/{email}")
    public SaayamResponse<UserProfileResponse> getUserProfileAfterLogin(@PathVariable String email) {
        UserProfileResponse response = userService.getUserProfileByEmail(email);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{email}, response);
    }

    @PutMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_ACCOUNT_UPDATED, new Object[]{userId}, response);
    }

    @PutMapping("/organization/{userId}")
    public SaayamResponse<OrganizationResponse> updateUserOrganization(
            @PathVariable String userId,
            @RequestBody UpdateOrganizationRequest request) {
        OrganizationResponse response = userService.updateUserOrganization(userId, request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @GetMapping("/organization/{userId}")
    public SaayamResponse<OrganizationResponse> getOrganizationByUserId(@PathVariable String userId) {
        OrganizationResponse organization = userService.getOrganizationByUserId(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, organization);
    }
    /* Profile Pic Upload */
    // Helper
    private String regionHint(HttpServletRequest req) {
        String r = req.getHeader(HDR_REGION);
        return (r == null || r.isBlank()) ? "us-east-1" : r;
    }

    private static final String HDR_CALLER_USER_ID = "X-Caller-UserId";
    private static final String HDR_CALLER_GROUPS  = "X-Caller-Groups"; // "admins,superadmins"

    private void authorize(HttpServletRequest req, String targetUserId) {
        String callerUserId = req.getHeader(HDR_CALLER_USER_ID);
        String groups = req.getHeader(HDR_CALLER_GROUPS);

        if (callerUserId == null || callerUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing caller identity");
        }

        if (callerUserId.equals(targetUserId)) return;

        String g = (groups == null) ? "" : groups.toLowerCase();
        boolean isAdmin = g.contains("admin");

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Not allowed to modify another user's profile image");
        }
    }

    // 1) UPLOAD (Base64)
    @PostMapping("/profileImage")
    public SaayamResponse<Map<String, Object>> uploadProfileImage(@RequestBody Map<String, String> body, HttpServletRequest req) {

        String userId = body.get("userId");
        String contentType = body.get("contentType");
        String base64 = body.get("base64");

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        authorize(req, userId);

        var payload = profileImageStorageService.uploadBase64(
                userId, contentType, base64, regionHint(req)
        );

        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, payload);
    }

    // 2) VIEW (Base64 JSON)
    @PostMapping("/profileImage/view")
    public SaayamResponse<Map<String, Object>> viewProfileImage(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        var imgOpt = profileImageStorageService.download(userId, regionHint(req));
        if (imgOpt.isEmpty()) {
            return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, Map.of("found", false));
        }

        var img = imgOpt.get();
        String base64 = Base64.getEncoder().encodeToString(img.bytes());

        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, Map.of(
                "found", true,
                "userId", userId,
                "contentType", img.contentType(),
                "base64", base64
        ));
    }

    // 3) DELETE
    @DeleteMapping("/profileImage")
    public SaayamResponse<Map<String, String>> deleteProfileImage(@RequestBody Map<String, String> body, HttpServletRequest req) {

        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        authorize(req, userId);

        profileImageStorageService.delete(userId, regionHint(req));

        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS,
                Map.of("userId", userId, "message", "Profile image deleted")
        );
    }

    @DeleteMapping("/profile/signoff")
    public SaayamResponse<SignOffResponse> signOffUser(
            @Valid @RequestBody SignOffRequest request) {
        String userId = request.userId();
        String reason = request.reason();
        if (userService.getProfilePicturePath(userId).isPresent()) {
            profileImageStorageService.delete(userId, "us-east-1");
        }
        SignOffResponse response = userService.signOffUser(userId, reason);
        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.USER_DELETED,
                new Object[]{userId},
                response
        );
    }

}
