package org.sfa.volunteer.exception;
import lombok.Getter;
import org.sfa.volunteer.dto.common.SaayamStatusCode;

@Getter

public class VolunteerException extends RuntimeException {
    private final String userId;

    public VolunteerException(String message, String userId) {
        super(message);
        this.userId = userId;
    }

    public static VolunteerException volunteerNotFound(String UserId) throws Exception {
        return new VolunteerException(SaayamStatusCode.VOLUNTEER_NOT_FOUND.toString(), UserId);
    }

    public static VolunteerException volunteerExists(String UserId) throws Exception {
        return new VolunteerException(SaayamStatusCode.VOLUNTEER_EXISTS.toString(), UserId);
    }

    public static VolunteerException volunteerInvalidStep(String UserId) throws Exception {
        return new VolunteerException(SaayamStatusCode.INVALID_VOLUNTEER_STEP.toString(), UserId);
    }

}
