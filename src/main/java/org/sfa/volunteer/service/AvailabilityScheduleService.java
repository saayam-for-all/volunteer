package org.sfa.volunteer.service;

import jakarta.persistence.EntityManager;
import org.sfa.volunteer.dto.request.AvailabilitySlotRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.response.AvailabilityResponse;
import org.sfa.volunteer.dto.response.AvailabilitySlotResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.AvailabilityFrequency;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.AvailabilityAccountRepository.Account;
import org.sfa.volunteer.repository.LegacyAvailabilityRepository;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.util.AvailabilityTimeConverter;
import org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot;
import org.sfa.volunteer.util.AvailabilityTimeConverter.UtcSlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared canonical schedule persistence. Profile retains its existing normalization policy. */
@Service
@Transactional(rollbackFor = Exception.class)
public class AvailabilityScheduleService {
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final int MAX_AVAILABILITY_SLOTS = 20;
    private static final DateTimeFormatter AVAILABILITY_TIME =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private final AvailabilityAccountRepository accountRepository;
    private final VolunteerUserAvailabilityRepository userAvailabilityRepository;
    private final LegacyAvailabilityRepository legacyAvailability;
    private final EntityManager entityManager;
    private final Clock clock;

    public AvailabilityScheduleService(AvailabilityAccountRepository accountRepository,
            VolunteerUserAvailabilityRepository userAvailabilityRepository, LegacyAvailabilityRepository legacyAvailability,
            EntityManager entityManager, Clock clock) {
        this.accountRepository = accountRepository;
        this.userAvailabilityRepository = userAvailabilityRepository;
        this.legacyAvailability = legacyAvailability;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AvailabilityResponse getAvailability(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return toAvailabilityResponse(account.userId(), account.timeZone(), readRows(userId));
    }

    List<VolunteerUserAvailability> readRows(String userId) {
        var rows = userAvailabilityRepository.findByUser_IdOrderByIdAsc(userId);
        if (rows.isEmpty() && legacyAvailability.containsSchedule(userId)) {
            throw new OnboardingAvailabilityException(409, "LEGACY_AVAILABILITY_MIGRATION_REQUIRED",
                    "Saved legacy availability requires migration or re-entry");
        }
        return rows;
    }

    public AvailabilityResponse updateAvailability(UpdateAvailabilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String userId = request.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        ZoneId zone = parseTimezone(request.getTimezone());
        List<LocalSlot> localSlots = parseSlots(request.getSlots());

        Account account = accountRepository.lockById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        accountRepository.updateTimezone(userId, zone.getId());
        return toAvailabilityResponse(account.userId(), zone.getId(), replaceRows(userId, zone, localSlots));
    }

    /** Replace canonical rows under the same account lock used by Profile writes. */
    public List<VolunteerUserAvailability> replace(String userId, ZoneId zone, List<LocalSlot> localSlots) {
        accountRepository.lockById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return replaceRows(userId, zone, localSlots);
    }

    private List<VolunteerUserAvailability> replaceRows(String userId, ZoneId zone, List<LocalSlot> localSlots) {
        // The lazy reference supplies the FK without hydrating the drifted legacy User mapping.
        User user = entityManager.getReference(User.class, userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<VolunteerUserAvailability> rows = new ArrayList<>();
        for (LocalSlot slot : localSlots) {
            UtcSlot utc = AvailabilityTimeConverter.toUtc(slot.day(), slot.start(), slot.end(), zone, today);
            rows.add(VolunteerUserAvailability.builder()
                    .user(user)
                    .dayOfWeek(dayName(utc.day()))
                    .startTime(utc.start())
                    .endTime(utc.end())
                    .lastUpdatedAt(now)
                    .build());
        }

        // Replace, not merge: flush the deletes so they run before the inserts
        userAvailabilityRepository.deleteByUser_Id(userId);
        userAvailabilityRepository.flush();
        var saved = userAvailabilityRepository.saveAll(rows);
        legacyAvailability.clear(userId);
        return saved;
    }

    // Checked against the IANA list rather than relying on ZoneId.of() throwing: ZoneId.of() also accepts
    // fixed offsets such as "+05:30", "UTC+5", "GMT-3" and "Z". Those never follow daylight saving, so slots
    // saved with one would be an hour off for half the year (BRD NFR-07 asks for an IANA zone).
    private static ZoneId parseTimezone(String timezone) {
        if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new IllegalArgumentException("timezone must be a valid IANA zone: " + timezone);
        }
        return ZoneId.of(timezone);
    }

    // One local slot per (day, start, end). Several slots on the same day are fine, but they must not overlap;
    // exact duplicates (e.g. Weekdays 9-17 plus Monday 9-17) are dropped rather than rejected.
    private static List<LocalSlot> parseSlots(List<AvailabilitySlotRequest> slots) {
        if (slots == null) {
            throw new IllegalArgumentException("slots is required");
        }
        if (slots.size() > MAX_AVAILABILITY_SLOTS) {
            throw new IllegalArgumentException("slots cannot have more than " + MAX_AVAILABILITY_SLOTS + " entries");
        }

        // local slot -> index of the first request entry that produced it
        Map<LocalSlot, Integer> localSlots = new LinkedHashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            AvailabilitySlotRequest slot = slots.get(i);
            String field = "slots[" + i + "]";
            if (slot == null) {
                throw new IllegalArgumentException(field + " is required");
            }
            AvailabilityFrequency frequency;
            try {
                frequency = AvailabilityFrequency.fromLabel(slot.getFrequency());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(field + ".frequency: " + e.getMessage());
            }
            LocalTime start = parseTime(slot.getStartTime(), field + ".startTime");
            LocalTime end = parseTime(slot.getEndTime(), field + ".endTime");
            if (start.equals(end)) {
                throw new IllegalArgumentException(field + ": startTime and endTime must differ");
            }
            int index = i;
            frequency.getDays().forEach(day -> localSlots.putIfAbsent(new LocalSlot(day, start, end), index));
        }
        rejectOverlaps(localSlots);
        return List.copyOf(localSlots.keySet());
    }

    private static void rejectOverlaps(Map<LocalSlot, Integer> localSlots) {
        List<Map.Entry<LocalSlot, Integer>> entries = List.copyOf(localSlots.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            for (int b = a + 1; b < entries.size(); b++) {
                Map.Entry<LocalSlot, Integer> first = entries.get(a);
                Map.Entry<LocalSlot, Integer> second = entries.get(b);
                // one request entry has a single time range, so it cannot overlap itself
                if (first.getValue().equals(second.getValue())
                        || !AvailabilityTimeConverter.overlaps(first.getKey(), second.getKey())) {
                    continue;
                }
                if (second.getValue() < first.getValue()) {
                    Map.Entry<LocalSlot, Integer> swap = first;
                    first = second;
                    second = swap;
                }
                throw new IllegalArgumentException("slots[" + first.getValue() + "] and slots[" + second.getValue()
                        + "] overlap on " + dayName(first.getKey().day()));
            }
        }
    }

    private static LocalTime parseTime(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return LocalTime.parse(value, AVAILABILITY_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be HH:mm: " + value);
        }
    }

    private record TimeRange(LocalTime start, LocalTime end) {}

    private record CollapsedSlot(Integer id, AvailabilityFrequency frequency, TimeRange range) {}

    // Rows are UTC, one per day; convert to the user's zone and group back into Everyday / Weekdays / Weekends
    private static AvailabilityResponse toAvailabilityResponse(String userId, String timezone, List<VolunteerUserAvailability> rows) {
        ZoneId zone = zoneOrUtc(timezone);

        // time range -> local day -> smallest row id
        Map<TimeRange, Map<DayOfWeek, Integer>> byRange = new LinkedHashMap<>();
        LocalDateTime lastUpdatedAt = null;
        for (VolunteerUserAvailability row : rows) {
            LocalSlot local = AvailabilityTimeConverter.toLocal(row.getStartTime(), row.getEndTime(), zone);
            byRange.computeIfAbsent(new TimeRange(local.start(), local.end()), r -> new EnumMap<>(DayOfWeek.class))
                    .merge(local.day(), row.getId(), Math::min);
            if (row.getLastUpdatedAt() != null
                    && (lastUpdatedAt == null || row.getLastUpdatedAt().isAfter(lastUpdatedAt))) {
                lastUpdatedAt = row.getLastUpdatedAt();
            }
        }

        List<CollapsedSlot> collapsed = new ArrayList<>();
        byRange.forEach((range, dayIds) -> {
            for (AvailabilityFrequency frequency : AvailabilityTimeConverter.collapse(dayIds.keySet())) {
                Integer id = frequency.getDays().stream()
                        .map(dayIds::get)
                        .filter(Objects::nonNull)
                        .min(Integer::compare)
                        .orElse(null);
                collapsed.add(new CollapsedSlot(id, frequency, range));
            }
        });
        collapsed.sort(Comparator.comparing(CollapsedSlot::frequency)
                .thenComparing(c -> c.range().start())
                .thenComparing(c -> c.range().end()));

        List<AvailabilitySlotResponse> slots = collapsed.stream()
                .map(c -> new AvailabilitySlotResponse(c.id(), c.frequency().getLabel(),
                        c.range().start().format(AVAILABILITY_TIME), c.range().end().format(AVAILABILITY_TIME)))
                .toList();
        return new AvailabilityResponse(userId, zone.getId(), slots, lastUpdatedAt);
    }

    static ZoneId zoneOrUtc(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    // "MONDAY" -> "Monday", as the day_of_week CHECK constraint expects
    private static String dayName(DayOfWeek day) {
        String name = day.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

}
