package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.VolunteerAvailabilityResponse;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.VolunteerRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("VolunteerServiceImpl - Step 4 Availability Tests")
class VolunteerServiceImplTest {

    @Mock
    private VolunteerRepository volunteerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VolunteerUserAvailabilityRepository userAvailabilityRepository;

    // Not using @InjectMocks — constructor has 4 params (2 same type), causes Mockito confusion
    private VolunteerServiceImpl volunteerService;

    private static final String USER_ID = "SID-00-000-002-001";

    private Volunteer mockVolunteer;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockVolunteer = Volunteer.builder()
                .id(USER_ID)
                .build();

        mockUser = User.builder()
                .id(USER_ID)
                .isEmergencyAvailable(false)
                .build();

        // Manually construct service — constructor has 4 params, 2 of same type
        // Pass userAvailabilityRepository twice (second param is unused in service)
        volunteerService = new VolunteerServiceImpl(
                volunteerRepository,
                userRepository,
                userAvailabilityRepository,
                userAvailabilityRepository
        );

        // Default lenient mocks — cover all repository calls across all tests
        lenient().when(volunteerRepository.findById(anyString()))
                .thenReturn(Optional.of(mockVolunteer));
        lenient().when(volunteerRepository.findVolunteerByUserId(anyString()))
                .thenReturn(mockVolunteer);
        lenient().when(volunteerRepository.save(any()))
                .thenReturn(mockVolunteer);
        lenient().when(userRepository.findById(anyString()))
                .thenReturn(Optional.of(mockUser));
        lenient().when(userRepository.save(any()))
                .thenReturn(mockUser);
        lenient().when(userAvailabilityRepository.findUserAvailability(anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(userAvailabilityRepository.saveAll(any()))
                .thenReturn(Collections.emptyList());
    }

    // -------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------

    private VolunteerUserAvailabilityRequest slot(String day, String start, String end) {
        return VolunteerUserAvailabilityRequest.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private VolunteerRequest buildRequest(Boolean emergency,
                                          List<VolunteerUserAvailabilityRequest> slots) {
        return VolunteerRequest.builder()
                .step(4)
                .userId(USER_ID)
                .isEmergencyAvailable(emergency)
                .availability(slots)
                .build();
    }

    private VolunteerUserAvailability mockEntity(String day, String start, String end) {
        return VolunteerUserAvailability.builder()
                .id(1)
                .user(mockUser)
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .lastUpdateDate(LocalDateTime.now())
                .build();
    }

    // =====================================================================
    // GET availability tests
    // =====================================================================

    @Nested
    @DisplayName("GET - getVolunteerUserAvailability")
    class GetAvailabilityTests {

        @Test
        @DisplayName("Should return availability response for valid user")
        void shouldReturnAvailability_whenValidUser() {
            // Arrange
            VolunteerUserAvailability entity = mockEntity("Monday", "09:00", "17:00");
            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(List.of(entity));

            // Act
            VolunteerAvailabilityResponse response =
                    volunteerService.getVolunteerUserAvailability(USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.totalSlots()).isEqualTo(1);
            assertThat(response.availability()).hasSize(1);
            assertThat(response.availability().get(0).startTime()).isEqualTo("09:00");
            assertThat(response.availability().get(0).endTime()).isEqualTo("17:00");
            assertThat(response.availability().get(0).dayOfWeek()).isEqualTo("Monday");
        }

        @Test
        @DisplayName("Should return empty list when no availability slots exist")
        void shouldReturnEmptyList_whenNoSlotsExist() {
            // Arrange
            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            VolunteerAvailabilityResponse response =
                    volunteerService.getVolunteerUserAvailability(USER_ID);

            // Assert
            assertThat(response.totalSlots()).isEqualTo(0);
            assertThat(response.availability()).isEmpty();
        }

        @Test
        @DisplayName("Should return multiple slots for multiple days")
        void shouldReturnMultipleSlots_whenMultipleDaysExist() {
            // Arrange
            List<VolunteerUserAvailability> entities = List.of(
                    mockEntity("Monday", "09:00", "12:00"),
                    mockEntity("Wednesday", "13:00", "17:00"),
                    mockEntity("Friday", "10:00", "15:00")
            );
            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID)).thenReturn(entities);

            // Act
            VolunteerAvailabilityResponse response =
                    volunteerService.getVolunteerUserAvailability(USER_ID);

            // Assert
            assertThat(response.totalSlots()).isEqualTo(3);
            assertThat(response.availability()).hasSize(3);
        }

        @Test
        @DisplayName("Should return emergency flag as true when set on user")
        void shouldReturnEmergencyFlag_whenSetOnUser() {
            // Arrange
            mockUser = User.builder().id(USER_ID).isEmergencyAvailable(true).build();
            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            VolunteerAvailabilityResponse response =
                    volunteerService.getVolunteerUserAvailability(USER_ID);

            // Assert
            assertThat(response.isEmergencyAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when volunteer not found - GET")
        void shouldThrowException_whenVolunteerNotFoundOnGet() {
            // Override default mock — volunteer not found
            when(volunteerRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.getVolunteerUserAvailability(USER_ID))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should throw exception when user not found - GET")
        void shouldThrowException_whenUserNotFoundOnGet() {
            // Override default mock — user not found
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.getVolunteerUserAvailability(USER_ID))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should format time correctly as HH:mm")
        void shouldFormatTime_asHHmm() {
            // Arrange
            VolunteerUserAvailability entity = mockEntity("Tuesday", "09:30", "14:45");
            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(List.of(entity));

            // Act
            VolunteerAvailabilityResponse response =
                    volunteerService.getVolunteerUserAvailability(USER_ID);

            // Assert
            assertThat(response.availability().get(0).startTime()).isEqualTo("09:30");
            assertThat(response.availability().get(0).endTime()).isEqualTo("14:45");
        }
    }

    // =====================================================================
    // SAVE / UPDATE availability tests
    // =====================================================================

    @Nested
    @DisplayName("POST - updateVolunteerStep4")
    class SaveAvailabilityTests {

        @Test
        @DisplayName("Should save single availability slot successfully")
        void shouldSaveSlot_whenValidRequest() throws Exception {
            // Arrange
            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Monday", "09:00", "17:00")));

            // Act
            volunteerService.updateVolunteerStep4(request);

            // Assert
            verify(userAvailabilityRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should save multiple slots on different days")
        void shouldSaveMultipleSlots_whenDifferentDays() throws Exception {
            // Arrange
            VolunteerRequest request = buildRequest(true, List.of(
                    slot("Monday", "09:00", "12:00"),
                    slot("Tuesday", "13:00", "17:00"),
                    slot("Wednesday", "10:00", "15:00")
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            volunteerService.updateVolunteerStep4(request);

            // Assert
            verify(userAvailabilityRepository).saveAll(argThat(list ->
                    ((List<?>) list).size() == 3));
        }

        @Test
        @DisplayName("Should set emergency flag to true when provided")
        void shouldSetEmergencyFlag_toTrue() throws Exception {
            // Arrange
            VolunteerRequest request = buildRequest(true,
                    List.of(slot("Monday", "09:00", "17:00")));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            volunteerService.updateVolunteerStep4(request);

            // Assert
            verify(userRepository).save(argThat(user -> ((User) user).isEmergencyAvailable()));
        }

        @Test
        @DisplayName("Should set emergency flag to false when null provided")
        void shouldSetEmergencyFlag_toFalse_whenNull() throws Exception {
            // Arrange
            VolunteerRequest request = buildRequest(null,
                    List.of(slot("Monday", "09:00", "17:00")));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            volunteerService.updateVolunteerStep4(request);

            // Assert
            verify(userRepository).save(argThat(user -> !((User) user).isEmergencyAvailable()));
        }

        @Test
        @DisplayName("Should delete existing slots before saving new ones")
        void shouldDeleteExistingSlots_beforeSavingNew() throws Exception {
            // Arrange
            List<VolunteerUserAvailability> existingSlots = List.of(
                    mockEntity("Monday", "09:00", "12:00"),
                    mockEntity("Tuesday", "10:00", "14:00")
            );

            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Friday", "09:00", "17:00")));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(existingSlots);

            // Act
            volunteerService.updateVolunteerStep4(request);

            // Assert - delete called first, then save
            verify(userAvailabilityRepository).deleteAll(existingSlots);
            verify(userAvailabilityRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should save empty availability list - clears all slots")
        void shouldSaveEmptyList_clearingAllSlots() throws Exception {
            // Arrange
            List<VolunteerUserAvailability> existingSlots = List.of(
                    mockEntity("Monday", "09:00", "12:00")
            );

            VolunteerRequest request = buildRequest(false, Collections.emptyList());

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(existingSlots);

            // Act
            volunteerService.updateVolunteerStep4(request);

            // Assert
            verify(userAvailabilityRepository).deleteAll(existingSlots);
            verify(userAvailabilityRepository).saveAll(Collections.emptyList());
        }

        @Test
        @DisplayName("Should throw exception when volunteer not found - POST")
        void shouldThrowException_whenVolunteerNotFoundOnSave() {
            // Arrange
            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Monday", "09:00", "17:00")));

            // Override default mock — volunteer not found
            when(volunteerRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(RuntimeException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found - POST")
        void shouldThrowException_whenUserNotFoundOnSave() {
            // Arrange
            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Monday", "09:00", "17:00")));

            // Override default mock — user not found
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(RuntimeException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }
    }

    // =====================================================================
    // VALIDATION tests
    // =====================================================================

    @Nested
    @DisplayName("Validation - Time Slot Rules")
    class ValidationTests {

        @Test
        @DisplayName("Should throw validation error when end time is before start time")
        void shouldFail_whenEndTimeBeforeStartTime() {
            // Arrange
            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Monday", "17:00", "09:00"))); // end before start

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(VolunteerException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should throw validation error when end time equals start time")
        void shouldFail_whenEndTimeEqualsStartTime() {
            // Arrange
            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Monday", "09:00", "09:00"))); // same time

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(VolunteerException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should throw validation error when slots overlap on same day")
        void shouldFail_whenSlotsOverlapOnSameDay() {
            // Arrange - Monday 09:00-13:00 overlaps with 12:00-17:00
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "09:00", "13:00"),
                    slot("Monday", "12:00", "17:00")  // overlaps
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(VolunteerException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should throw validation error when exact duplicate slot on same day")
        void shouldFail_whenExactDuplicateSlotOnSameDay() {
            // Arrange
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "09:00", "17:00"),
                    slot("Monday", "09:00", "17:00")  // exact duplicate
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(VolunteerException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should NOT throw error when slots on same day do not overlap")
        void shouldPass_whenSlotsOnSameDayDoNotOverlap() throws Exception {
            // Arrange - Monday 09:00-12:00 and 13:00-17:00 (no overlap)
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "09:00", "12:00"),
                    slot("Monday", "13:00", "17:00")
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - should not throw
            assertThatCode(() -> volunteerService.updateVolunteerStep4(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should NOT throw error when slots overlap on DIFFERENT days")
        void shouldPass_whenOverlappingTimesOnDifferentDays() throws Exception {
            // Arrange - same times but different days = no conflict
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "09:00", "17:00"),
                    slot("Tuesday", "09:00", "17:00")
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - should not throw
            assertThatCode(() -> volunteerService.updateVolunteerStep4(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should collect all validation errors and throw together")
        void shouldCollectAllErrors_andThrowTogether() {
            // Arrange - two invalid slots
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "17:00", "09:00"),   // end before start
                    slot("Tuesday", "15:00", "10:00")   // end before start
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(VolunteerException.class)
                    .satisfies(ex -> {
                        VolunteerException ve = (VolunteerException) ex;
                        assertThat(ve.getErrors()).hasSizeGreaterThan(1); // multiple errors collected
                    });

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should pass when slot ends exactly when next starts (adjacent, no overlap)")
        void shouldPass_whenSlotEndsExactlyWhenNextStarts() throws Exception {
            // Arrange - 09:00-12:00 and 12:00-17:00 (adjacent, not overlapping)
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "09:00", "12:00"),
                    slot("Monday", "12:00", "17:00")
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - should not throw (12:00 end meets 12:00 start, no overlap)
            assertThatCode(() -> volunteerService.updateVolunteerStep4(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw error when one slot contains another (fully contained overlap)")
        void shouldFail_whenOneSlotFullyContainsAnother() {
            // Arrange - 08:00-18:00 fully contains 09:00-17:00
            VolunteerRequest request = buildRequest(false, List.of(
                    slot("Monday", "08:00", "18:00"),
                    slot("Monday", "09:00", "17:00")  // fully inside first slot
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

            // Act & Assert
            assertThatThrownBy(() -> volunteerService.updateVolunteerStep4(request))
                    .isInstanceOf(VolunteerException.class);

            verify(userAvailabilityRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should handle midnight boundary slots correctly")
        void shouldPass_withMidnightBoundarySlot() throws Exception {
            // Arrange
            VolunteerRequest request = buildRequest(false,
                    List.of(slot("Saturday", "22:00", "23:59")));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            assertThatCode(() -> volunteerService.updateVolunteerStep4(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle all 7 days of the week")
        void shouldPass_withAllSevenDays() throws Exception {
            // Arrange - one slot per day, no overlaps
            VolunteerRequest request = buildRequest(true, List.of(
                    slot("Monday", "09:00", "10:00"),
                    slot("Tuesday", "09:00", "10:00"),
                    slot("Wednesday", "09:00", "10:00"),
                    slot("Thursday", "09:00", "10:00"),
                    slot("Friday", "09:00", "10:00"),
                    slot("Saturday", "09:00", "10:00"),
                    slot("Sunday", "09:00", "10:00")
            ));

            when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.of(mockVolunteer));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
            when(userAvailabilityRepository.findUserAvailability(USER_ID))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            assertThatCode(() -> volunteerService.updateVolunteerStep4(request))
                    .doesNotThrowAnyException();

            verify(userAvailabilityRepository).saveAll(argThat(list ->
                    ((List<?>) list).size() == 7));
        }
    }
}