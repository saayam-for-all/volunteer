package org.sfa.volunteer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.sfa.volunteer.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sfa.volunteer.dto.request.UserPreferenceRequest;
import org.sfa.volunteer.dto.response.UserPreferenceResponse;
import org.sfa.volunteer.exception.UserCategoryNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.UserAdditionalDetail;
import org.sfa.volunteer.model.UserCategory;
import org.sfa.volunteer.repository.UserAdditionalDetailRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import java.util.Optional;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private UserAdditionalDetailRepository userAdditionalDetailRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateUserPreferences_Success_WithNewDetail() throws Exception {
        // Arrange
        String userId = "testUserId";
        UserPreferenceRequest request = UserPreferenceRequest.builder()
                .language1("English")
                .secondaryEmail1("new@example.com")
                .build();

        User user = User.builder()
                .id(userId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAdditionalDetailRepository.findByUserId(userId)).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userAdditionalDetailRepository.save(any(UserAdditionalDetail.class))).thenAnswer(invocation -> {
            UserAdditionalDetail detail = invocation.getArgument(0);
            detail.setAdditionalDetailId(2L); // Simulate ID assignment
            return detail;
        });

        // Act
        UserPreferenceResponse response = userService.updateUserPreferences(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("English", response.language1());
        assertEquals("new@example.com", response.secondaryEmail1());

        verify(userRepository).findById(userId);
        verify(userCategoryRepository, never()).findById(anyInt()); // No category update
        verify(userRepository).save(user);
        verify(userAdditionalDetailRepository).findByUserId(userId);
        verify(userAdditionalDetailRepository).save(any(UserAdditionalDetail.class));
    }

     @Test
    void testUpdateUserPreferences_Success_WithExistingDetail() throws Exception {
        // Arrange
        String userId = "testUserId";
        UserPreferenceRequest request = UserPreferenceRequest.builder()
                .userCategoryId(2)
                .language1("English")
                .language2("Spanish")
                .language3("French")
                .secondaryEmail1("secondary1@example.com")
                .secondaryEmail2("secondary2@example.com")
                .secondaryPhone1("123-456-7890")
                .secondaryPhone2("098-765-4321")
                .build();

        User user = User.builder()
                .id(userId)
                .language1("Old English")
                .build();

        UserCategory userCategory = UserCategory.builder()
                .userCategoryId(2)
                .userCategory("Volunteer")
                .build();

        UserAdditionalDetail existingDetail = UserAdditionalDetail.builder()
                .additionalDetailId(1L)
                .user(user)
                .secondaryEmail1("old1@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userCategoryRepository.findById(2)).thenReturn(Optional.of(userCategory));
        when(userAdditionalDetailRepository.findByUserId(userId)).thenReturn(existingDetail);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userAdditionalDetailRepository.save(any(UserAdditionalDetail.class))).thenReturn(existingDetail);

        // Act
        UserPreferenceResponse response = userService.updateUserPreferences(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(2, response.userCategoryId());
        assertEquals("Volunteer", response.userCategory());
        assertEquals("English", response.language1());
        assertEquals("Spanish", response.language2());
        assertEquals("French", response.language3());
        assertEquals("secondary1@example.com", response.secondaryEmail1());
        assertEquals("secondary2@example.com", response.secondaryEmail2());
        assertEquals("123-456-7890", response.secondaryPhone1());
        assertEquals("098-765-4321", response.secondaryPhone2());

        verify(userRepository).findById(userId);
        verify(userCategoryRepository).findById(2);
        verify(userRepository).save(user);
        verify(userAdditionalDetailRepository).findByUserId(userId);
        verify(userAdditionalDetailRepository).save(existingDetail);

        // Verify user fields were updated
        assertEquals("English", user.getLanguage1());
        assertEquals("Spanish", user.getLanguage2());
        assertEquals("French", user.getLanguage3());
        assertEquals(userCategory, user.getUserCategory());

        // Verify additional detail fields were updated
        assertEquals("secondary1@example.com", existingDetail.getSecondaryEmail1());
        assertEquals("secondary2@example.com", existingDetail.getSecondaryEmail2());
        assertEquals("123-456-7890", existingDetail.getSecondaryPhone1());
        assertEquals("098-765-4321", existingDetail.getSecondaryPhone2());
    }

    @Test
    void testUpdateUserPreferences_UserCategoryNotFound() throws Exception {
        // Arrange
        String userId = "testUserId";
        UserPreferenceRequest request = UserPreferenceRequest.builder()
                .userCategoryId(999) // Non-existent category
                .build();

        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userCategoryRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserCategoryNotFoundException.class, () -> userService.updateUserPreferences(userId, request));

        verify(userRepository).findById(userId);
        verify(userCategoryRepository).findById(999);
        verify(userRepository, never()).save(any(User.class));
    }

        @Test
    void testUpdateUserPreferences_UserNotFound() throws Exception {
        // Arrange
        String userId = "nonExistentUser";
        UserPreferenceRequest request = UserPreferenceRequest.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.updateUserPreferences(userId, request));

        verify(userRepository).findById(userId);
        verify(userCategoryRepository, never()).findById(anyInt());
        verify(userRepository, never()).save(any(User.class));
        verify(userAdditionalDetailRepository, never()).findByUserId(anyString());
    }
}