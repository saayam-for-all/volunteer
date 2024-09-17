package org.sfa.volunteer.model.enums;

import lombok.Getter;

@Getter
public enum RequestCategoryEnum {
    UNSPECIFIED(0),
    TECHNICAL_SUPPORT(1),
    FINANCIAL_SUPPORT(2),
    LEGAL_SUPPORT(3),
    OTHER(4);

    private final int id;

    RequestCategoryEnum(int id) {
        this.id = id;
    }
}

