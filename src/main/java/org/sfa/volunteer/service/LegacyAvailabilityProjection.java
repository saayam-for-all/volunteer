package org.sfa.volunteer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.LegacyAvailabilityRepository;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.util.AvailabilityTimeConverter;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Maps the relational schedule to the existing volunteer and steward response fields. */
@Component
public class LegacyAvailabilityProjection {
    public record View(Integer stage, JsonNode days, JsonNode times, boolean migrationRequired) {}

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private final AvailabilityAccountRepository accounts;
    private final VolunteerUserAvailabilityRepository availability;

    public LegacyAvailabilityProjection(AvailabilityAccountRepository accounts,
            VolunteerUserAvailabilityRepository availability) {
        this.accounts = accounts;
        this.availability = availability;
    }

    public Map<String, View> read(List<Volunteer> volunteers) {
        if (volunteers.isEmpty()) return Map.of();
        List<String> ids = volunteers.stream().map(Volunteer::getId).toList();
        var accountsById = accounts.findByIds(ids);
        var rowsById = availability.findByUser_IdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(row -> row.getUser().getId()));
        Map<String, View> result = new LinkedHashMap<>();
        for (Volunteer volunteer : volunteers) {
            String id = volunteer.getId();
            var account = accountsById.get(id);
            Integer stage = account == null ? null : account.promotionWizardStage();
            List<VolunteerUserAvailability> rows = rowsById.getOrDefault(id, List.of());
            if (rows.isEmpty()) {
                JsonNode days = volunteer.getAvailabilityDays();
                JsonNode times = volunteer.getAvailabilityTimes();
                result.put(id, new View(stage, days, times,
                        LegacyAvailabilityRepository.hasContent(days) || LegacyAvailabilityRepository.hasContent(times)));
            } else {
                ZoneId zone = AvailabilityScheduleService.zoneOrUtc(account == null ? null : account.timeZone());
                result.put(id, relational(stage, rows, zone));
            }
        }
        return result;
    }

    private static View relational(Integer stage, List<VolunteerUserAvailability> rows, ZoneId zone) {
        var days = JSON.arrayNode();
        var times = JSON.arrayNode();
        var localDays = EnumSet.noneOf(DayOfWeek.class);
        for (var row : rows) {
            var local = AvailabilityTimeConverter.toLocal(row.getStartTime(), row.getEndTime(), zone);
            localDays.add(local.day());
            times.addObject()
                    .put("id", row.getId())
                    .put("dayOfWeek", dayName(local.day()))
                    .put("startTime", row.getStartTime().toInstant(ZoneOffset.UTC).toString())
                    .put("endTime", row.getEndTime().toInstant(ZoneOffset.UTC).toString());
        }
        localDays.forEach(day -> days.add(dayName(day)));
        return new View(stage, days, times, false);
    }

    private static String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

}
