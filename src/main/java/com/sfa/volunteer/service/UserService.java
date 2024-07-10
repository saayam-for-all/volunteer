package com.sfa.volunteer.service;

import com.sfa.volunteer.dto.CreateUserRequest;
import com.sfa.volunteer.dto.CreateUserResponse;
import com.sfa.volunteer.dto.PaginationResponse;
import com.sfa.volunteer.dto.UpdateUserProfileRequest;
import com.sfa.volunteer.dto.UserProfileResponse;
import com.sfa.volunteer.model.User;

import java.util.List;
import java.util.Locale;


public interface UserService {

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    UserProfileResponse getUserProfileById(String userId);

    CreateUserResponse createUser(CreateUserRequest createUserRequest, Locale locale);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);
}
