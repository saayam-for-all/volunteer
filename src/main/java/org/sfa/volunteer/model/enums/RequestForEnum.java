package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum RequestForEnum {
    UNSPECIFIED(0),
    SELF(1),
    OTHER(2);

    private final int id;

    RequestForEnum(int id) {
        this.id = id;
    }

    public static RequestForEnum fromString(String value) {
        for (RequestForEnum requestFor : values()) {
            if (requestFor.name().equalsIgnoreCase(value)) {
                return requestFor;
            }
        }
        throw new IllegalArgumentException("Unknown request type: " + value);
    }
}



