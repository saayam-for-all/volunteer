package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdatePersonalInfoRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.UserPreferenceRequest;
import org.sfa.volunteer.dto.response.UserPreferenceResponse;
import org.sfa.volunteer.dto.response.*;
import org.springframework.stereotype.Service;
import java.util.List;

public interface UserService {
    List<org.sfa.volunteer.model.VolunteerUserAvailability> replaceAvailabilityRows(String userId,
            java.time.ZoneId zone, List<org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot> slots,
            java.time.LocalDate anchorDate);


    PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize);

    PaginationResponse<UserProfileResponse> searchUsers(String query, Integer pageNumber, Integer pageSize);

    boolean isAdminUser(String userId);

    UserProfileResponse getUserProfileById(String userId);

    WizardStatusResponse getWizardStatus(String userId);

    AddressStatusResponse getAddressStatus(String userId);

    UserProfileResponse getUserProfileByEmail(String email);

    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest updateUserProfileRequest);


    SignOffResponse signOffUser(String userId, String reason);

    UserIdResponse getUserIdByEmail(String email);
    // Profile Pic Upload
    // AWS (S3 URI <-> DB)
    void setProfilePicturePath(String userId, String s3Uri);

    java.util.Optional<String> getProfilePicturePath(String userId);

    boolean userExists(String userId);

    PersonalInfoResponse getPersonalInfoById(String userId);

    PersonalInfoResponse updatePersonalInfo(UpdatePersonalInfoRequest updateRequest);

    boolean isEmailOwnedByUser(String userId, String email);

    UserPreferenceResponse updateUserPreferences(String userId, UserPreferenceRequest request) throws Exception;

    UserSkillsResponse getUserSkills(String userId);

    AvailabilityResponse getAvailability(String userId);

    AvailabilityResponse updateAvailability(UpdateAvailabilityRequest request);
}
