package org.sfa.volunteer.exception;

import lombok.Getter;
import org.sfa.volunteer.dto.common.SaayamStatusCode;

@Getter
public class SkillsException extends RuntimeException {

    private final String userId;

    public SkillsException(String message, String userId) {
        super(message);
        this.userId = userId;
    }

    public static SkillsException badRequest(String userId) {
        return new SkillsException(SaayamStatusCode.BAD_REQUEST.toString(), userId);
    }

    public static SkillsException invalidCategory(String userId) {
        return new SkillsException(SaayamStatusCode.INVALID_SKILL_CATEGORY.toString(), userId);
    }
}
