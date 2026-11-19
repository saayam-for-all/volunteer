package org.sfa.volunteer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.exception.UserNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void isEmailOwnedByUserMatchesExactCaseEmail() {
        when(userRepository.existsByIdAndPrimaryEmailAddressIgnoreCase("user-1", "jane.doe@example.com"))
                .thenReturn(true);

        assertThat(userService.isEmailOwnedByUser("user-1", "jane.doe@example.com")).isTrue();
    }

    @Test
    void isEmailOwnedByUserIsCaseInsensitive() {
        when(userRepository.existsByIdAndPrimaryEmailAddressIgnoreCase("user-1", "Jane.Doe@Example.com"))
                .thenReturn(true);

        assertThat(userService.isEmailOwnedByUser("user-1", "Jane.Doe@Example.com")).isTrue();
    }

    @Test
    void isEmailOwnedByUserTrimsWhitespace() {
        when(userRepository.existsByIdAndPrimaryEmailAddressIgnoreCase("user-1", "jane.doe@example.com"))
                .thenReturn(true);

        assertThat(userService.isEmailOwnedByUser("user-1", " jane.doe@example.com ")).isTrue();
    }

    @Test
    void isEmailOwnedByUserReturnsFalseForEmailBelongingToADifferentDuplicateRow() {
        // e.g. two rows share an email; the caller claims a userId that isn't
        // the one owning this email -- should be denied, not resolved to a
        // different user's id.
        when(userRepository.existsByIdAndPrimaryEmailAddressIgnoreCase("other-user", "jane.doe@example.com"))
                .thenReturn(false);

        assertThat(userService.isEmailOwnedByUser("other-user", "jane.doe@example.com")).isFalse();
    }

    @Test
    void isEmailOwnedByUserReturnsFalseWhenNoUserMatches() {
        when(userRepository.existsByIdAndPrimaryEmailAddressIgnoreCase("user-1", "unknown@gmail.com"))
                .thenReturn(false);

        assertThat(userService.isEmailOwnedByUser("user-1", "unknown@gmail.com")).isFalse();
    }

    @Test
    void isEmailOwnedByUserReturnsFalseForBlankInput() {
        assertThat(userService.isEmailOwnedByUser(null, "jane.doe@example.com")).isFalse();
        assertThat(userService.isEmailOwnedByUser("user-1", null)).isFalse();
        assertThat(userService.isEmailOwnedByUser(" ", "jane.doe@example.com")).isFalse();
    }

    @Test
    void setProfilePicturePathUpdatesOnlyTheProfileColumns() {
        when(userRepository.updateProfilePicturePath(eq("user-1"), eq("s3://bucket/users/user-1/profile"), any()))
                .thenReturn(1);

        userService.setProfilePicturePath("user-1", "s3://bucket/users/user-1/profile");

        // no exception means the narrow update path succeeded without touching
        // the rest of the entity's (currently schema-drifted) columns
    }

    @Test
    void setProfilePicturePathThrowsWhenUserDoesNotExist() {
        when(userRepository.updateProfilePicturePath(eq("missing-user"), anyString(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> userService.setProfilePicturePath("missing-user", "s3://bucket/path"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getProfilePicturePathReturnsValueWhenPresent() {
        when(userRepository.findProfilePicturePathById("user-1"))
                .thenReturn(Optional.of("s3://bucket/users/user-1/profile"));

        assertThat(userService.getProfilePicturePath("user-1"))
                .contains("s3://bucket/users/user-1/profile");
    }

    @Test
    void getProfilePicturePathReturnsEmptyWhenBlankOrMissing() {
        when(userRepository.findProfilePicturePathById("user-1")).thenReturn(Optional.of(" "));
        when(userRepository.findProfilePicturePathById("user-2")).thenReturn(Optional.empty());

        assertThat(userService.getProfilePicturePath("user-1")).isEmpty();
        assertThat(userService.getProfilePicturePath("user-2")).isEmpty();
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
