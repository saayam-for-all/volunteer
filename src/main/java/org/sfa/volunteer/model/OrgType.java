package org.sfa.volunteer.model;

public enum OrgType {
    NON_PROFIT("non_profit"),
    FOR_PROFIT("for_profit");

    private final String dbValue;

    OrgType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static OrgType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (OrgType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid organization type: " + value);
    }
}

