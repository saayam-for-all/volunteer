package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.response.OnboardingAvailabilityResponse;
import org.sfa.volunteer.dto.response.OnboardingAvailabilitySlotResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilityRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilitySchema;
import org.sfa.volunteer.util.AvailabilityTimeConverter;
import org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot;
import org.sfa.volunteer.util.AvailabilityTimeConverter.UtcSlot;
import org.sfa.volunteer.util.OnboardingAvailabilityPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(rollbackFor = Exception.class)
public class OnboardingAvailabilityService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final AvailabilityAccountRepository accounts;
    private final OnboardingAvailabilityRepository state;
    private final AvailabilityScheduleService schedules;
    private final Clock clock;
    private final OnboardingAvailabilitySchema schema;

    public OnboardingAvailabilityService(AvailabilityAccountRepository accounts,
            OnboardingAvailabilityRepository state, AvailabilityScheduleService schedules,
            Clock clock, OnboardingAvailabilitySchema schema) {
        this.accounts = accounts;
        this.state = state;
        this.schedules = schedules;
        this.clock = clock;
        this.schema = schema;
    }

    public OnboardingAvailabilityResponse save(OnboardingAvailabilitySaveRequest request) {
        List<LocalSlot> slots = OnboardingAvailabilityPolicy.normalize(request);
        schema.requireReady();
        var account = accounts.lockById(request.userId()).orElseThrow(() -> missingAccount());
        requireVolunteer(request.userId());
        ZoneId zone = storedZone(account.timeZone());
        requireRepresentableTimes(slots, zone);
        var saved = schedules.replace(request.userId(), zone, slots);
        state.saveState(request.userId(), request.isEmergencyAvailable(),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return response(account.userId(), request.isEmergencyAvailable(),
                Math.max(account.promotionWizardStage() == null ? 0 : account.promotionWizardStage(), 4), zone, saved);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OnboardingAvailabilityResponse read(String userId) {
        OnboardingAvailabilityPolicy.requireUserId(userId);
        schema.requireReady();
        var account = accounts.findById(userId).orElseThrow(() -> missingAccount());
        requireVolunteer(userId);
        return response(userId, state.emergencyAvailable(userId), account.promotionWizardStage(),
                storedZone(account.timeZone()), schedules.readRows(userId));
    }

    /** Acknowledges saved Step 4 or advances to Review while holding the shared account lock. */
    public void complete(String userId, Integer requestedStep) {
        OnboardingAvailabilityPolicy.requireUserId(userId);
        int step = requestedStep == null ? 4 : requestedStep;
        if (step != 4 && step != 5) {
            throw new OnboardingAvailabilityException(400, "BAD_REQUEST", "Completion step must be 4 or 5");
        }
        schema.requireReady();
        var account = accounts.lockById(userId).orElseThrow(() -> missingAccount());
        requireVolunteer(userId);
        if (account.promotionWizardStage() == null || account.promotionWizardStage() < 4) throw stepFourRequired();
        requirePersistedIntervals(schedules.readRows(userId), storedZone(account.timeZone()));
        if (step == 5 && account.promotionWizardStage() < step) {
            state.advanceProgress(userId, step, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        }
    }

    private static void requirePersistedIntervals(List<VolunteerUserAvailability> rows, ZoneId zone) {
        if (rows.isEmpty()) throw stepFourRequired();
        List<LocalSlot> local = new ArrayList<>(rows.size());
        for (var row : rows) {
            var start = row.getStartTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
            var end = row.getEndTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
            if (!start.toLocalDate().equals(end.toLocalDate())
                    || !end.toLocalTime().isAfter(start.toLocalTime())
                    || !end.toInstant().isAfter(start.toInstant())
                    || start.getSecond() != 0 || start.getNano() != 0 || end.getSecond() != 0 || end.getNano() != 0) {
                throw stepFourRequired();
            }
            local.add(new LocalSlot(start.getDayOfWeek(), start.toLocalTime(), end.toLocalTime()));
        }
        if (OnboardingAvailabilityPolicy.hasOverlap(local)) throw stepFourRequired();
    }

    private static OnboardingAvailabilityException stepFourRequired() {
        return new OnboardingAvailabilityException(409, "STEP_FOUR_REQUIRED",
                "Save valid onboarding availability before completing this step");
    }

    private static ZoneId storedZone(String timezone) {
        if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new OnboardingAvailabilityException(409, "ACCOUNT_TIMEZONE_REQUIRED",
                    "Set a valid account timezone before using onboarding availability");
        }
        return ZoneId.of(timezone);
    }

    private void requireRepresentableTimes(List<LocalSlot> slots, ZoneId zone) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        for (LocalSlot slot : slots) {
            UtcSlot utc;
            try {
                utc = AvailabilityTimeConverter.toUtc(slot.day(), slot.start(), slot.end(), zone, today);
            } catch (IllegalArgumentException invalidTime) {
                throw invalidOccurrence();
            }
            var roundTrip = AvailabilityTimeConverter.toLocal(utc.start(), utc.end(), zone);
            if (!slot.equals(roundTrip) || !utc.end().isAfter(utc.start())) {
                throw invalidOccurrence();
            }
        }
    }

    private static OnboardingAvailabilityException invalidOccurrence() {
        return new OnboardingAvailabilityException(400, "BAD_REQUEST",
                "Availability times must exist in the account timezone on their next occurrence");
    }

    private static OnboardingAvailabilityException missingAccount() {
        return new OnboardingAvailabilityException(404, "USER_NOT_FOUND", "Account not found");
    }

    private void requireVolunteer(String userId) {
        if (!state.volunteerExists(userId)) {
            throw new OnboardingAvailabilityException(404, "VOLUNTEER_NOT_FOUND", "Volunteer not found");
        }
    }

    private static OnboardingAvailabilityResponse response(String userId, boolean emergency, Integer stage,
            ZoneId zone, List<VolunteerUserAvailability> rows) {
        var availability = rows.stream().map(row -> {
            var local = AvailabilityTimeConverter.toLocal(row.getStartTime(), row.getEndTime(), zone);
            String day = local.day().name().charAt(0) + local.day().name().substring(1).toLowerCase(Locale.ROOT);
            return new OnboardingAvailabilitySlotResponse(row.getId(), day,
                    local.start().format(TIME), local.end().format(TIME));
        }).sorted(Comparator.comparing((OnboardingAvailabilitySlotResponse slot) ->
                DayOfWeek.valueOf(slot.dayOfWeek().toUpperCase(Locale.ROOT)))
                .thenComparing(OnboardingAvailabilitySlotResponse::startTime)
                .thenComparing(OnboardingAvailabilitySlotResponse::endTime)).toList();
        return new OnboardingAvailabilityResponse(userId, emergency, stage, availability);
    }
}
