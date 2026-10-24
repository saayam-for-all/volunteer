package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.AvailabilitySlotRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.AvailabilityResponse;
import org.sfa.volunteer.dto.response.AvailabilitySlotResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.util.AvailabilityTimeConverter;
import org.sfa.volunteer.util.AvailabilityTimeConverter.UtcSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAvailabilityTest {

    private static final String USER_ID = "SID-1";
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    @Mock
    private UserRepository userRepository;
    @Mock
    private VolunteerUserAvailabilityRepository userAvailabilityRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private static User user(String timeZone) {
        return User.builder().id(USER_ID).timeZone(timeZone).build();
    }

    private static AvailabilitySlotRequest slot(String frequency, String start, String end) {
        return new AvailabilitySlotRequest(frequency, start, end);
    }

    private static UpdateAvailabilityRequest update(String timezone, List<AvailabilitySlotRequest> slots) {
        return new UpdateAvailabilityRequest(USER_ID, timezone, slots, null, null, null);
    }

    private static VolunteerUserAvailability row(int id, DayOfWeek localDay, String start, String end,
                                                 LocalDateTime lastUpdatedAt) {
        UtcSlot utc = AvailabilityTimeConverter.toUtc(localDay, LocalTime.parse(start), LocalTime.parse(end),
                CHICAGO, LocalDate.of(2026, 7, 6));
        return VolunteerUserAvailability.builder()
                .id(id)
                .dayOfWeek(utc.day().name())
                .startTime(utc.start())
                .endTime(utc.end())
                .lastUpdatedAt(lastUpdatedAt)
                .build();
    }

    // saveAll echoes the rows back with ids, as the IDENTITY column would
    private void saveAllAssignsIds() {
        AtomicInteger ids = new AtomicInteger(100);
        when(userAvailabilityRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<VolunteerUserAvailability> rows = inv.getArgument(0);
            rows.forEach(r -> r.setId(ids.incrementAndGet()));
            return rows;
        });
    }

    @SuppressWarnings("unchecked")
    private List<VolunteerUserAvailability> savedRows() {
        ArgumentCaptor<List<VolunteerUserAvailability>> captor = ArgumentCaptor.forClass(List.class);
        verify(userAvailabilityRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // getAvailability

    @Test
    void getReturnsEmptySlotsWhenNothingSaved() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("America/Chicago")));
        when(userAvailabilityRepository.findByUser_IdOrderByIdAsc(USER_ID)).thenReturn(List.of());

        AvailabilityResponse response = userService.getAvailability(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getTimezone()).isEqualTo("America/Chicago");
        assertThat(response.getSlots()).isEmpty();
        assertThat(response.getLastUpdatedAt()).isNull();
    }

    @Test
    void getGroupsDailyRowsBackIntoFrequenciesInLocalTime() {
        LocalDateTime older = LocalDateTime.of(2026, 10, 1, 12, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 10, 2, 12, 0);
        List<VolunteerUserAvailability> rows = new ArrayList<>();
        int id = 1;
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            rows.add(row(id++, day, "09:00", "17:00", older));
        }
        rows.add(row(id, DayOfWeek.SATURDAY, "22:00", "02:00", newer));
        Collections.reverse(rows);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("America/Chicago")));
        when(userAvailabilityRepository.findByUser_IdOrderByIdAsc(USER_ID)).thenReturn(rows);

        AvailabilityResponse response = userService.getAvailability(USER_ID);

        assertThat(response.getSlots()).containsExactly(
                new AvailabilitySlotResponse(1, "Weekdays", "09:00", "17:00"),
                new AvailabilitySlotResponse(6, "Saturday", "22:00", "02:00"));
        assertThat(response.getLastUpdatedAt()).isEqualTo(newer);
    }

    @Test
    void getFallsBackToUtcWhenStoredTimezoneIsInvalid() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("Not/AZone")));
        when(userAvailabilityRepository.findByUser_IdOrderByIdAsc(USER_ID)).thenReturn(List.of());

        assertThat(userService.getAvailability(USER_ID).getTimezone()).isEqualTo("UTC");
    }

    @Test
    void getRejectsBlankUserId() {
        assertThatThrownBy(() -> userService.getAvailability(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        verifyNoInteractions(userRepository, userAvailabilityRepository);
    }

    @Test
    void getThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAvailability(USER_ID)).isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(userAvailabilityRepository);
    }

    // updateAvailability

    @Test
    void updateReplacesRowsInUtcAndSavesTimezone() {
        User user = user("UTC");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        saveAllAssignsIds();
        UpdateAvailabilityRequest request = update("America/Chicago", List.of(
                slot("Weekdays", "09:00", "17:00"),
                slot("Saturday", "22:00", "02:00")));
        // ignored for now: no vacation columns in user_availability
        request.setVacationMode(true);
        request.setVacationStartDate("2026-12-20");
        request.setVacationEndDate("2026-12-31");

        AvailabilityResponse response = userService.updateAvailability(request);

        assertThat(user.getTimeZone()).isEqualTo("America/Chicago");
        verify(userRepository).save(user);

        InOrder order = inOrder(userAvailabilityRepository);
        order.verify(userAvailabilityRepository).deleteByUser_Id(USER_ID);
        order.verify(userAvailabilityRepository).flush();
        order.verify(userAvailabilityRepository).saveAll(anyList());

        List<VolunteerUserAvailability> rows = savedRows();
        assertThat(rows).hasSize(6);
        // Saturday 22:00 in Chicago is Sunday in UTC
        assertThat(rows).extracting(VolunteerUserAvailability::getDayOfWeek)
                .containsExactlyInAnyOrder("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Sunday");
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.getUser()).isSameAs(user);
            assertThat(r.getLastUpdatedAt()).isNotNull();
            assertThat(r.getDayOfWeek()).isEqualToIgnoringCase(r.getStartTime().getDayOfWeek().name());
            assertThat(r.getEndTime()).isAfter(r.getStartTime());
        });

        assertThat(response.getTimezone()).isEqualTo("America/Chicago");
        assertThat(response.getSlots()).extracting(AvailabilitySlotResponse::getFrequency,
                        AvailabilitySlotResponse::getStartTime, AvailabilitySlotResponse::getEndTime)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Weekdays", "09:00", "17:00"),
                        org.assertj.core.groups.Tuple.tuple("Saturday", "22:00", "02:00"));
    }

    @Test
    void updateExpandsEverydayToSevenRows() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("UTC")));
        saveAllAssignsIds();

        AvailabilityResponse response =
                userService.updateAvailability(update("UTC", List.of(slot("Everyday", "08:00", "10:00"))));

        assertThat(savedRows()).hasSize(7);
        assertThat(response.getSlots()).extracting(AvailabilitySlotResponse::getFrequency).containsExactly("Everyday");
    }

    @Test
    void updateDropsExactDuplicateDays() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("UTC")));
        saveAllAssignsIds();

        userService.updateAvailability(update("UTC", List.of(
                slot("Weekdays", "09:00", "17:00"),
                slot("Monday", "09:00", "17:00"))));

        assertThat(savedRows()).hasSize(5);
    }

    @Test
    void updateAllowsSeveralSlotsOnTheSameDay() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("UTC")));
        saveAllAssignsIds();

        AvailabilityResponse response = userService.updateAvailability(update("America/Chicago", List.of(
                slot("Monday", "16:00", "18:00"),
                slot("Monday", "10:00", "14:00"))));

        assertThat(savedRows()).hasSize(2);
        assertThat(response.getSlots()).extracting(AvailabilitySlotResponse::getFrequency,
                        AvailabilitySlotResponse::getStartTime, AvailabilitySlotResponse::getEndTime)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Monday", "10:00", "14:00"),
                        org.assertj.core.groups.Tuple.tuple("Monday", "16:00", "18:00"));
    }

    @Test
    void updateAllowsAnExtraSlotOnADayAlreadyCoveredByWeekdays() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("UTC")));
        saveAllAssignsIds();

        AvailabilityResponse response = userService.updateAvailability(update("America/Chicago", List.of(
                slot("Weekdays", "10:00", "14:00"),
                slot("Monday", "16:00", "18:00"))));

        assertThat(savedRows()).hasSize(6);
        assertThat(response.getSlots()).extracting(AvailabilitySlotResponse::getFrequency,
                        AvailabilitySlotResponse::getStartTime)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Weekdays", "10:00"),
                        org.assertj.core.groups.Tuple.tuple("Monday", "16:00"));
    }

    @Test
    void updateAllowsTouchingSlots() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("UTC")));
        saveAllAssignsIds();

        userService.updateAvailability(update("UTC", List.of(
                slot("Monday", "10:00", "14:00"),
                slot("Monday", "14:00", "16:00"))));

        assertThat(savedRows()).hasSize(2);
    }

    @Test
    void updateRejectsOverlappingSlotsOnTheSameDay() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(
                slot("Monday", "10:00", "14:00"),
                slot("Tuesday", "10:00", "14:00"),
                slot("Monday", "12:00", "16:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0] and slots[2] overlap on Monday");
        verifyNoInteractions(userRepository, userAvailabilityRepository);
    }

    @Test
    void updateRejectsSlotOverlappingAGroupedFrequency() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(
                slot("Weekdays", "09:00", "17:00"),
                slot("Wednesday", "12:00", "13:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0] and slots[1] overlap on Wednesday");
    }

    @Test
    void updateRejectsOvernightSlotRunningIntoTheNextDaysSlot() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(
                slot("Monday", "22:00", "02:00"),
                slot("Tuesday", "01:00", "03:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0] and slots[1] overlap on Monday");
    }

    @Test
    void updateRejectsSundayNightWrappingIntoMonday() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(
                slot("Monday", "00:30", "02:00"),
                slot("Sunday", "23:00", "01:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0] and slots[1] overlap on Monday");
    }

    @Test
    void updateWithNoSlotsClearsAvailability() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user("UTC")));
        saveAllAssignsIds();

        AvailabilityResponse response = userService.updateAvailability(update("Asia/Kolkata", List.of()));

        verify(userAvailabilityRepository).deleteByUser_Id(USER_ID);
        assertThat(savedRows()).isEmpty();
        assertThat(response.getSlots()).isEmpty();
        assertThat(response.getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void updateThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of())))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(userAvailabilityRepository);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRejectsMissingBody() {
        assertThatThrownBy(() -> userService.updateAvailability(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request body is required");
    }

    @Test
    void updateRejectsBlankUserId() {
        UpdateAvailabilityRequest request = update("UTC", List.of());
        request.setUserId("");

        assertThatThrownBy(() -> userService.updateAvailability(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        verifyNoInteractions(userRepository, userAvailabilityRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Mars/Base", "CST", "+05:30", "UTC+5", "GMT-3", "Z"})
    void updateRejectsInvalidTimezone(String timezone) {
        assertThatThrownBy(() -> userService.updateAvailability(update(timezone, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("timezone must be a valid IANA zone");
        verifyNoInteractions(userRepository, userAvailabilityRepository);
    }

    @Test
    void updateRejectsNullTimezone() {
        assertThatThrownBy(() -> userService.updateAvailability(update(null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("timezone must be a valid IANA zone");
    }

    @Test
    void updateRejectsMissingSlots() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots is required");
    }

    @Test
    void updateRejectsMoreThanTwentySlots() {
        List<AvailabilitySlotRequest> slots = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            slots.add(slot("Monday", String.format("%02d:00", i), String.format("%02d:30", i)));
        }

        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", slots)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots cannot have more than 20 entries");
    }

    @Test
    void updateRejectsUnknownFrequency() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(
                slot("Monday", "09:00", "10:00"),
                slot("Daily", "09:00", "10:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[1].frequency: Invalid frequency: Daily");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9:00", "24:00", "09:60", "9am", "09:00:00"})
    void updateRejectsTimesThatAreNotHhMm(String time) {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(slot("Monday", time, "10:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0].startTime must be HH:mm: " + time);
    }

    @Test
    void updateRejectsMissingEndTime() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(slot("Monday", "09:00", null)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0].endTime is required");
    }

    @Test
    void updateRejectsEqualStartAndEnd() {
        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", List.of(slot("Monday", "09:00", "09:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0]: startTime and endTime must differ");
        verifyNoInteractions(userAvailabilityRepository);
    }

    @Test
    void updateRejectsNullSlot() {
        List<AvailabilitySlotRequest> slots = new ArrayList<>();
        slots.add(null);

        assertThatThrownBy(() -> userService.updateAvailability(update("UTC", slots)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slots[0] is required");
    }
}
