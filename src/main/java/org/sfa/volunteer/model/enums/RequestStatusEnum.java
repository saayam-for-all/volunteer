package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum RequestStatusEnum {
    UNSPECIFIED(0),
    CREATED(1),
    PENDING_VOLUNTEER_ASSIGNMENT(2),
    IN_PROGRESS(3),
    COMPLETED(4),
    CANCELLED(5),
    DELETED(6),
    RATED_BY_REQUESTER(7),
    RATED_BY_VOLUNTEER(8);

    private final int id;

    RequestStatusEnum(int id) {
        this.id = id;
    }
}

