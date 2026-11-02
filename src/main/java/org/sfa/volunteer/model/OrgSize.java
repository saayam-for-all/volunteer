package org.sfa.volunteer.model;

public enum OrgSize {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large");

    private final String dbValue;

    OrgSize(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static OrgSize fromString(String value) {
        if (value == null) {
            return null;
        }
        for (OrgSize size : values()) {
            if (size.dbValue.equalsIgnoreCase(value) || size.name().equalsIgnoreCase(value)) {
                return size;
            }
        }
        throw new IllegalArgumentException("Invalid organization size: " + value);
    }
}

