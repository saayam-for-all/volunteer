package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;

import org.sfa.volunteer.dto.response.*;


public interface UserService {

    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    UserProfileResponse getUserProfileById(String userId);

    WizardStatusResponse getWizardStatus(String userId);

    AddressStatusResponse getAddressStatus(String userId);

    UserProfileResponse getUserProfileByEmail(String email);

    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);

    OrganizationResponse updateUserOrganization(String userId, UpdateOrganizationRequest request);

    OrganizationResponse getOrganizationByUserId(String userId);

    SignOffResponse signOffUser(String userId, String reason);

    UserIdResponse getUserIdByEmail(String email);

    // Profile Pic Upload
    // AWS (S3 URI <-> DB)
    void setProfilePicturePath(String userId, String s3Uri);
    java.util.Optional<String> getProfilePicturePath(String userId);
    boolean userExists(String userId);
    String getUserIdByEmailForAuth(String email);
}
