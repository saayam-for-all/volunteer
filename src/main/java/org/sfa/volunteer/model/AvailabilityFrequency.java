package org.sfa.volunteer.model;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

// The "frequency" choices on the webapp's Profile > Availability tab
public enum AvailabilityFrequency {
    EVERYDAY("Everyday", EnumSet.allOf(DayOfWeek.class)),
    WEEKDAYS("Weekdays", EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
    WEEKENDS("Weekends", EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    MONDAY("Monday", EnumSet.of(DayOfWeek.MONDAY)),
    TUESDAY("Tuesday", EnumSet.of(DayOfWeek.TUESDAY)),
    WEDNESDAY("Wednesday", EnumSet.of(DayOfWeek.WEDNESDAY)),
    THURSDAY("Thursday", EnumSet.of(DayOfWeek.THURSDAY)),
    FRIDAY("Friday", EnumSet.of(DayOfWeek.FRIDAY)),
    SATURDAY("Saturday", EnumSet.of(DayOfWeek.SATURDAY)),
    SUNDAY("Sunday", EnumSet.of(DayOfWeek.SUNDAY));

    private final String label;
    private final Set<DayOfWeek> days;

    AvailabilityFrequency(String label, Set<DayOfWeek> days) {
        this.label = label;
        this.days = days;
    }

    public String getLabel() {
        return label;
    }

    public Set<DayOfWeek> getDays() {
        return EnumSet.copyOf(days);
    }

    public static AvailabilityFrequency fromLabel(String label) {
        return Arrays.stream(values())
                .filter(f -> f.label.equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid frequency: " + label));
    }

    public static AvailabilityFrequency of(DayOfWeek day) {
        return valueOf(day.name());
    }
}
