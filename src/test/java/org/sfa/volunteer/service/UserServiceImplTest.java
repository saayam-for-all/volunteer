package org.sfa.volunteer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSignOffReasonRepository;
import org.sfa.volunteer.repository.UserStatusRepository;
import org.sfa.volunteer.service.impl.UserServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserSignOffReasonRepository userSignOffReasonRepository;
    @Mock
    private UserCategoryRepository userCategoryRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private StateRepository stateRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateUserProfilePreservesExistingProfilePicturePathWhenRequestOmitsIt() {
        User existing = userWithProfilePath("s3://saayam-bucket/users/user-1/profile");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("Neel")
                .lastName("Harip")
                .profilePicturePath(null)
                .build();

        var response = userService.updateUserProfile("user-1", request);

        assertThat(existing.getProfilePicturePath()).isEqualTo("s3://saayam-bucket/users/user-1/profile");
        assertThat(response.profilePicturePath()).isEqualTo("s3://saayam-bucket/users/user-1/profile");
    }

    @Test
    void updateUserProfilePreservesExistingProfilePicturePathWhenRequestSendsBlankValue() {
        User existing = userWithProfilePath("s3://saayam-bucket/users/user-1/profile");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("Neel")
                .profilePicturePath(" ")
                .build();

        var response = userService.updateUserProfile("user-1", request);

        assertThat(response.profilePicturePath()).isEqualTo("s3://saayam-bucket/users/user-1/profile");
    }

    @Test
    void updateUserProfileAcceptsExplicitProfilePicturePath() {
        User existing = userWithProfilePath("s3://saayam-bucket/users/user-1/old-profile");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .profilePicturePath("s3://saayam-bucket/users/user-1/profile")
                .build();

        var response = userService.updateUserProfile("user-1", request);

        assertThat(response.profilePicturePath()).isEqualTo("s3://saayam-bucket/users/user-1/profile");
    }

    private static User userWithProfilePath(String profilePicturePath) {
        return User.builder()
                .id("user-1")
                .firstName("Old")
                .lastName("Name")
                .profilePicturePath(profilePicturePath)
                .build();
    }
}
