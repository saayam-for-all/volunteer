package org.sfa.volunteer.dto.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SaayamStatusCode {
    // total range (1200-1399)
    // Success codes (1200-1299)
    SUCCESS("SAAAYAM-1200"),
    USER_CREATED("SAAAYAM-1201"),
    USER_ACCOUNT_UPDATED("SAAAYAM-1202"),
    USER_CONTACT_UPDATED("SAAAYAM-1203"),
    USER_PERSONAL_INFO_UPDATED("SAAAYAM-1204"),
    USER_DELETED("SAAAYAM-1205"),
    USER_PROFILES_LIST_FETCHED_SUCCESS("SAAAYAM-1206"),
    USER_PROFILE_FETCHED_SUCCESS("SAAAYAM-1207"),
    VOLUNTEER_CREATED("SAAAYAM-1208"),
    VOLUNTEER_UPDATED("SAAAYAM-1209"),
    VOLUNTEER_EXISTS("SAAAYAM-1210"),


    // Client error codes (1300-1349)
    BAD_REQUEST("SAAAYAM-1300"),
    INVALID_PARAMETER("SAAAYAM-1301"),
    USER_NOT_FOUND("SAAAYAM-1302"),
    VOLUNTEER_NOT_FOUND("SAAAYAM-1303"),
    UNAUTHORIZED("SAAAYAM-1304"),
    FORBIDDEN("SAAAYAM-1305"),
    COUNTRY_NOT_FOUND("SAAYAM-1306"),
    STATE_NOT_FOUND("SAAYAM-1307"),
    INVALID_USER_STATUS("SAAAYAM-1308"),
    INVALID_USER_CATEGORY("SAAAYAM-1309"),
    USER_ALREADY_DELETED("SAAAYAM-1310"),
    ENUM_UNSPECIFIED("SAAAYAM-1311"),
    INVALID_VOLUNTEER_STEP("SAAAYAM-1312"),

    // Server error codes (1350-1399)
    INTERNAL_SERVER_ERROR("SAAAYAM-1350"),
    DATABASE_ERROR("SAAAYAM-1351"),
    EXTERNAL_SERVICE_ERROR("SAAAYAM-1352"),
    UNEXPECTED_ERROR("SAAAYAM-1353"),
    UNKNOWN_ERROR("SAAAYAM-1354");

    private final String code;

}
