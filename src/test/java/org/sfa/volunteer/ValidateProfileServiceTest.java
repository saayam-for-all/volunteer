package org.sfa.volunteer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.ValidateProfileRequest;
import org.sfa.volunteer.dto.response.ProfileValidationResponse;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.service.impl.UserServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Should return profileComplete true when all required fields are present")
    void shouldReturnProfileCompleteTrueWhenAllFieldsPresent() {
        User user = new User();
        user.setId("user-123");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPrimaryEmailAddress("john.doe@example.com");
        user.setPrimaryPhoneNumber("+1234567890");

        Country country = new Country();
        country.setCountryName("United States");
        user.setCountry(country);

        ValidateProfileRequest request = ValidateProfileRequest.builder()
                .userId("user-123")
                .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        ProfileValidationResponse response = userService.validateProfile(request);

        assertTrue(response.profileComplete());
        assertEquals("user-123", response.userId());
        assertTrue(response.missingFields().isEmpty());
    }

    @Test
    @DisplayName("Should return profileComplete false when phone and country are missing")
    void shouldReturnProfileCompleteFalseWhenPhoneAndCountryMissing() {
        User user = new User();
        user.setId("user-123");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPrimaryEmailAddress("john.doe@example.com");
        user.setPrimaryPhoneNumber(null);
        user.setCountry(null);

        ValidateProfileRequest request = ValidateProfileRequest.builder()
                .userId("user-123")
                .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        ProfileValidationResponse response = userService.validateProfile(request);

        assertFalse(response.profileComplete());
        assertEquals("user-123", response.userId());
        assertEquals(2, response.missingFields().size());
        assertTrue(response.missingFields().contains("phone"));
        assertTrue(response.missingFields().contains("country"));
    }

    @Test
    @DisplayName("Should return profileComplete false when all required fields are missing")
    void shouldReturnProfileCompleteFalseWhenAllRequiredFieldsMissing() {
        User user = new User();
        user.setId("user-123");
        user.setFirstName(null);
        user.setLastName(null);
        user.setPrimaryEmailAddress(null);
        user.setPrimaryPhoneNumber(null);
        user.setCountry(null);

        ValidateProfileRequest request = ValidateProfileRequest.builder()
                .userId("user-123")
                .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        ProfileValidationResponse response = userService.validateProfile(request);

        assertFalse(response.profileComplete());
        assertEquals("user-123", response.userId());
        assertEquals(5, response.missingFields().size());
        assertTrue(response.missingFields().contains("firstName"));
        assertTrue(response.missingFields().contains("lastName"));
        assertTrue(response.missingFields().contains("email"));
        assertTrue(response.missingFields().contains("phone"));
        assertTrue(response.missingFields().contains("country"));
    }

    @Test
    @DisplayName("Should treat blank fields as missing")
    void shouldTreatBlankFieldsAsMissing() {
        User user = new User();
        user.setId("user-123");
        user.setFirstName("   ");
        user.setLastName("");
        user.setPrimaryEmailAddress("   ");
        user.setPrimaryPhoneNumber("");
        
        Country country = new Country();
        country.setCountryName("   ");
        user.setCountry(country);

        ValidateProfileRequest request = ValidateProfileRequest.builder()
                .userId("user-123")
                .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        ProfileValidationResponse response = userService.validateProfile(request);

        assertFalse(response.profileComplete());
        assertEquals(5, response.missingFields().size());
        assertTrue(response.missingFields().contains("firstName"));
        assertTrue(response.missingFields().contains("lastName"));
        assertTrue(response.missingFields().contains("email"));
        assertTrue(response.missingFields().contains("phone"));
        assertTrue(response.missingFields().contains("country"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not found")
    void shouldThrowExceptionWhenUserNotFound() {
        ValidateProfileRequest request = ValidateProfileRequest.builder()
                .userId("user-123")
                .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.validateProfile(request)
        );

        assertEquals("User not found", exception.getMessage());
    }
}