package com.saayam.volunteer.service;

import com.saayam.volunteer.domain.CreateUserRequest;
import com.saayam.volunteer.domain.CreateUserResponse;
import com.saayam.volunteer.domain.PaginationResponse;
import com.saayam.volunteer.domain.UpdateUserProfileRequest;
import com.saayam.volunteer.domain.UserProfileResponse;
import com.saayam.volunteer.model.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserService {

    List<User> findAllUsers();  // will be deprecated

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);
    UserProfileResponse getUserProfileById(String userId);

    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);
}
