package org.sfa.volunteer.exception;

public class AvailabilityErrorMessages {

    public static final String END_BEFORE_START =
            "End time must be after start time";


    public static final String SLOT_OVERLAP =
            "This slot overlaps with an existing slot for %s";


    public static final String MISSING_TIME_FIELDS =
            "Please select both a start and end time";

    private AvailabilityErrorMessages() {}
}
