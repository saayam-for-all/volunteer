package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.CreateUserRequest;
import org.sfa.volunteer.dto.CreateUserResponse;
import org.sfa.volunteer.dto.PaginationResponse;
import org.sfa.volunteer.dto.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.UserProfileResponse;

import java.util.Locale;


public interface UserService {

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    UserProfileResponse getUserProfileById(String userId);

    CreateUserResponse createUser(CreateUserRequest createUserRequest, Locale locale);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);
}
