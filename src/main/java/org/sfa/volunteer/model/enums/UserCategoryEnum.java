package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum UserCategoryEnum {
    MEMBER(0),
    DONOR(1),
    VOLUNTEER(2);

    private final int id;

    UserCategoryEnum(int id) {
        this.id = id;
    }
}
