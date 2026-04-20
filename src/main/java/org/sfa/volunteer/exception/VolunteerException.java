package org.sfa.volunteer.exception;
import lombok.Getter;
import org.sfa.volunteer.dto.common.SaayamStatusCode;

import java.util.Collections;
import java.util.List;

@Getter

public class VolunteerException extends RuntimeException {
    private final String userId;
    private final List<String> errors;

    public VolunteerException(String message, String userId) {
        super(message);
        this.userId = userId;
        this.errors = Collections.emptyList();
    }
    public VolunteerException(String message, List<String> errors) {
        super(message);
        this.userId = null;
        this.errors = errors;
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

    public static VolunteerException validationFailed(List<String> errors) throws Exception {
        return new VolunteerException(
                SaayamStatusCode.BAD_REQUEST.toString(),
                errors
        );
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getUserId() {
        return userId;
    }

}
