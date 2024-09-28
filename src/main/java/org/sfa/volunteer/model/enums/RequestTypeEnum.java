package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum RequestTypeEnum {
    UNSPECIFIED(0),
    IN_PERSON(1),
    REMOTE(2);

    private final int id;

    RequestTypeEnum(int id) {
        this.id = id;
    }

    public static RequestTypeEnum fromId(int id) {
        for (RequestTypeEnum type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid RequestTypeId: " + id);
    }
}


