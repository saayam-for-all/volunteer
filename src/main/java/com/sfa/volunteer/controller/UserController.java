package com.sfa.volunteer.controller;

import com.sfa.volunteer.dto.CreateUserRequest;
import com.sfa.volunteer.dto.CreateUserResponse;
import com.sfa.volunteer.dto.PaginationResponse;
import com.sfa.volunteer.dto.UpdateUserProfileRequest;
import com.sfa.volunteer.dto.UserProfileResponse;
import com.sfa.volunteer.service.UserService;
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

import java.util.Locale;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request,  Locale locale) {
        return userService.createUser(request, locale);
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
