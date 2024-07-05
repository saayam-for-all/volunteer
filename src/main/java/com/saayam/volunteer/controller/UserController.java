package com.saayam.volunteer.controller;

import com.saayam.volunteer.dto.CreateUserRequest;
import com.saayam.volunteer.dto.CreateUserResponse;
import com.saayam.volunteer.dto.PaginationResponse;
import com.saayam.volunteer.dto.UpdateUserProfileRequest;
import com.saayam.volunteer.dto.UserProfileResponse;
import com.saayam.volunteer.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public PaginationResponse<UserProfileResponse> getUsersWithPagination(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return userService.findAllUsersWithPagination(page, size);
    }

    @GetMapping("/profile/{userId}")
    public UserProfileResponse getUserProfile(@PathVariable String userId) {
        return userService.getUserProfileById(userId);
    }

    @PutMapping("/profile/{userId}")
    public UserProfileResponse updateUserProfile(
            @PathVariable String userId,
            @RequestBody UpdateUserProfileRequest request) {
        return userService.updateUserProfile(userId, request);
    }

}
