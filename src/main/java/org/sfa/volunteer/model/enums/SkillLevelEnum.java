package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum SkillLevelEnum {
    BEGINNER(0),
    INTERMEDIATE(1),
    ADVANCED(2),
    EXPERT(3);

    private final int id;

    SkillLevelEnum(int id) {
        this.id = id;
    }
}



