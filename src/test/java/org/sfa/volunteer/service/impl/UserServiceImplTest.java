package org.sfa.volunteer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.sfa.volunteer.model.UserAdditionalDetail;
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
}