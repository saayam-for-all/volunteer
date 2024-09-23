package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;


public interface UserService {

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    UserProfileResponse getUserProfileById(String userId);

    UserProfileResponse getUserProfileByEmail(String email);

    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);
}
