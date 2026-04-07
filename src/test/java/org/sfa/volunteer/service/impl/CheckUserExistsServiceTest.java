package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.CheckUserExistsRequest;
import org.sfa.volunteer.dto.response.UserExistsResponse;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserStatusRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckUserExistsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatusRepository userStatusRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private StateRepository stateRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Country testCountry;

    @BeforeEach
    void setUp() {
        testCountry = Country.builder()
                .countryId(1)
                .countryName("United States")
                .build();

        testUser = User.builder()
                .id("user-123")
                .firstName("John")
                .lastName("Doe")
                .primaryEmailAddress("john.doe@example.com")
                .primaryPhoneNumber("+1234567890")
                .country(testCountry)
                .build();
    }

    @Nested
    @DisplayName("Base match: firstName + lastName only")
    class BaseMatchTests {

        @Test
        @DisplayName("Should return exists=true when user found by firstName and lastName")
        void shouldReturnExistsWhenUserFoundByName() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should return exists=false when no user found by firstName and lastName")
        void shouldReturnNotExistsWhenNoUserFoundByName() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("Unknown")
                    .lastName("Person")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("Unknown", "Person"))
                    .thenReturn(Collections.emptyList());

            UserExistsResponse response = userService.checkUserExists(request);

            assertFalse(response.exists());
            assertNull(response.userId());
            assertNull(response.matchedOn());
        }

        @Test
        @DisplayName("Should perform case-insensitive name matching")
        void shouldMatchNamesCaseInsensitively() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("  JOHN  ")
                    .lastName("  DOE  ")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("JOHN", "DOE"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            verify(userRepository).findByFirstNameAndLastNameIgnoreCase("JOHN", "DOE");
        }
    }

    @Nested
    @DisplayName("Email strengthening filter")
    class EmailFilterTests {

        @Test
        @DisplayName("Should narrow match with email when email matches")
        void shouldNarrowMatchWithEmailWhenMatches() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName, email", response.matchedOn());
        }

        @Test
        @DisplayName("Should still match on name when email does not match any user")
        void shouldFallBackToNameWhenEmailDoesNotMatch() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("different@example.com")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should match email case-insensitively")
        void shouldMatchEmailCaseInsensitively() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("JOHN.DOE@EXAMPLE.COM")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName, email", response.matchedOn());
        }
    }

    @Nested
    @DisplayName("Phone strengthening filter")
    class PhoneFilterTests {

        @Test
        @DisplayName("Should narrow match with phone when phone matches")
        void shouldNarrowMatchWithPhoneWhenMatches() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .phone("+1234567890")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName, phone", response.matchedOn());
        }

        @Test
        @DisplayName("Should still match on name when phone does not match")
        void shouldFallBackToNameWhenPhoneDoesNotMatch() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .phone("+9999999999")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }
    }

    @Nested
    @DisplayName("Country strengthening filter")
    class CountryFilterTests {

        @Test
        @DisplayName("Should narrow match with country when country matches")
        void shouldNarrowMatchWithCountryWhenMatches() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .country("United States")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName, country", response.matchedOn());
        }

        @Test
        @DisplayName("Should match country case-insensitively")
        void shouldMatchCountryCaseInsensitively() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .country("UNITED STATES")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName, country", response.matchedOn());
        }

        @Test
        @DisplayName("Should still match on name when country does not match")
        void shouldFallBackToNameWhenCountryDoesNotMatch() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .country("Canada")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should handle user with null country gracefully")
        void shouldHandleNullCountryOnUser() {
            User userWithoutCountry = User.builder()
                    .id("user-456")
                    .firstName("Jane")
                    .lastName("Doe")
                    .country(null)
                    .build();

            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("Jane")
                    .lastName("Doe")
                    .country("India")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("Jane", "Doe"))
                    .thenReturn(List.of(userWithoutCountry));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-456", response.userId());
            assertEquals("firstName, lastName", response.matchedOn());
        }
    }

    @Nested
    @DisplayName("All fields combined")
    class AllFieldsCombinedTests {

        @Test
        @DisplayName("Should match on all criteria when all fields match")
        void shouldMatchOnAllCriteriaWhenAllFieldsMatch() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+1234567890")
                    .country("United States")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName, email, phone, country", response.matchedOn());
        }

        @Test
        @DisplayName("Should match on name + email only when phone and country differ")
        void shouldMatchPartialCriteria() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+9999999999")
                    .country("Canada")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName, email", response.matchedOn());
        }

        @Test
        @DisplayName("Should not exist when no names match even with all fields provided")
        void shouldNotExistWhenNoNameMatch() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("Nobody")
                    .lastName("Here")
                    .email("john.doe@example.com")
                    .phone("+1234567890")
                    .country("United States")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("Nobody", "Here"))
                    .thenReturn(Collections.emptyList());

            UserExistsResponse response = userService.checkUserExists(request);

            assertFalse(response.exists());
            assertNull(response.userId());
            assertNull(response.matchedOn());
        }
    }

    @Nested
    @DisplayName("Multiple users matching")
    class MultipleUsersTests {

        @Test
        @DisplayName("Should narrow to correct user using email from multiple name matches")
        void shouldPickCorrectUserByEmailFromMultiple() {
            User user2 = User.builder()
                    .id("user-789")
                    .firstName("John")
                    .lastName("Doe")
                    .primaryEmailAddress("john.other@example.com")
                    .primaryPhoneNumber("+5555555555")
                    .country(testCountry)
                    .build();

            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.other@example.com")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser, user2));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-789", response.userId());
            assertEquals("firstName, lastName, email", response.matchedOn());
        }

        @Test
        @DisplayName("Should narrow to correct user using phone from multiple name matches")
        void shouldPickCorrectUserByPhoneFromMultiple() {
            User user2 = User.builder()
                    .id("user-789")
                    .firstName("John")
                    .lastName("Doe")
                    .primaryEmailAddress("john.other@example.com")
                    .primaryPhoneNumber("+5555555555")
                    .country(testCountry)
                    .build();

            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .phone("+5555555555")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser, user2));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-789", response.userId());
            assertEquals("firstName, lastName, phone", response.matchedOn());
        }

        @Test
        @DisplayName("Should return first user when multiple match and no additional filters provided")
        void shouldReturnFirstUserWhenMultipleMatchNoFilters() {
            User user2 = User.builder()
                    .id("user-789")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser, user2));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should progressively narrow from multiple users with all filters")
        void shouldProgressivelyNarrowWithAllFilters() {
            Country india = Country.builder().countryId(2).countryName("India").build();

            User user2 = User.builder()
                    .id("user-789")
                    .firstName("John")
                    .lastName("Doe")
                    .primaryEmailAddress("john.doe@example.com")
                    .primaryPhoneNumber("+1234567890")
                    .country(india)
                    .build();

            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+1234567890")
                    .country("United States")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser, user2));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("user-123", response.userId());
            assertEquals("firstName, lastName, email, phone, country", response.matchedOn());
        }
    }

    @Nested
    @DisplayName("Edge cases: null and empty optional fields")
    class NullAndEmptyFieldTests {

        @Test
        @DisplayName("Should treat null email as not provided")
        void shouldTreatNullEmailAsNotProvided() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email(null)
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should treat empty email as not provided")
        void shouldTreatEmptyEmailAsNotProvided() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("  ")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should treat null phone as not provided")
        void shouldTreatNullPhoneAsNotProvided() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .phone(null)
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should treat empty phone as not provided")
        void shouldTreatEmptyPhoneAsNotProvided() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .phone("")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should treat null country as not provided")
        void shouldTreatNullCountryAsNotProvided() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .country(null)
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            assertEquals("firstName, lastName", response.matchedOn());
        }

        @Test
        @DisplayName("Should trim whitespace from all input fields")
        void shouldTrimWhitespaceFromInputFields() {
            CheckUserExistsRequest request = CheckUserExistsRequest.builder()
                    .firstName("  John  ")
                    .lastName("  Doe  ")
                    .email("  john.doe@example.com  ")
                    .phone("  +1234567890  ")
                    .country("  United States  ")
                    .build();

            when(userRepository.findByFirstNameAndLastNameIgnoreCase("John", "Doe"))
                    .thenReturn(List.of(testUser));

            UserExistsResponse response = userService.checkUserExists(request);

            assertTrue(response.exists());
            verify(userRepository).findByFirstNameAndLastNameIgnoreCase("John", "Doe");
        }
    }
}
