package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.VolunteerProfileResponse;
import org.sfa.volunteer.model.entity.Request;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final ResponseBuilder responseBuilder;

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

    @GetMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        UserProfileResponse response = userService.getUserProfileById(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @PutMapping("/profile/{userId}")
    public SaayamResponse<UserProfileResponse> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.USER_ACCOUNT_UPDATED, new Object[]{userId}, response);
    }

    @GetMapping("/topVolunteers")
    public ResponseEntity<PaginationResponse<VolunteerProfileResponse>> getTopVolunteers(
            @RequestBody Request request,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        PaginationResponse<VolunteerProfileResponse> topVolunteers = userService.getTopVolunteersForRequestCategory(page, size, request);
        return ResponseEntity.ok(topVolunteers);
    }

}
