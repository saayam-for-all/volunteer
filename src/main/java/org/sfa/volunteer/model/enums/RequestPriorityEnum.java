package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum RequestPriorityEnum {
    UNSPECIFIED(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int id;

    RequestPriorityEnum(int id) {
        this.id = id;
    }
}



