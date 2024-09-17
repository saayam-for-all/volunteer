package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ACTIVE(0),
    INACTIVE(1),
    PENDING(2),
    ON_HOLD(3);

    private final int id;

    UserStatusEnum(int id) {
        this.id = id;
    }
}
