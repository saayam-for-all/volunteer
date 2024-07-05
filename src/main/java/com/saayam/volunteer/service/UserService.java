package com.saayam.volunteer.service;

import com.saayam.volunteer.dto.CreateUserRequest;
import com.saayam.volunteer.dto.CreateUserResponse;
import com.saayam.volunteer.dto.PaginationResponse;
import com.saayam.volunteer.dto.UpdateUserProfileRequest;
import com.saayam.volunteer.dto.UserProfileResponse;
import com.saayam.volunteer.model.User;

import java.util.List;


public interface UserService {

    List<User> findAllUsers();  // will be deprecated

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    UserProfileResponse getUserProfileById(String userId);

    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);
}
