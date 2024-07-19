package org.sfa.volunteer.dto.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sfa.volunteer.util.MessageSourceUtil;

@Getter
@RequiredArgsConstructor
public enum SaayamStatusCode {
    // total range: 1200-1399
    // Success codes (1200-1299)
    SUCCESS("SAAAYAM-1200"),
    USER_CREATED("SAAAYAM-1201"),
    USER_ACCOUNT_UPDATED("SAAAYAM-1202"),
    USER_CONTACT_UPDATED("SAAAYAM-1203"),
    USER_PERSONAL_INFO_UPDATED("SAAAYAM-1204"),
    USER_DELETED("SAAAYAM-1205"),
    USER_PROFILES_LIST_FETCHED_SUCCESS("SAAAYAM-1206"),
    USER_PROFILE_FETCHED_SUCCESS("SAAAYAM-1207"),

    // Client error codes (1300-1399)
    BAD_REQUEST("SAAAYAM-1300"),
    INVALID_PARAMETER("SAAAYAM-1301"),
    USER_NOT_FOUND("SAAAYAM-1302"),
    UNAUTHORIZED("SAAAYAM-1304"),
    FORBIDDEN("SAAAYAM-1305"),
    COUNTRY_NOT_FOUND("SAAYAM-1306"),
    STATE_NOT_FOUND("SAAYAM-1307"),
    INVALID_USER_STATUS("SAAAYAM-1306"),
    INVALID_USER_CATEGORY("SAAAYAM-1307"),
    USER_ALREADY_DELETED("SAAAYAM-1308"),
    ENUM_UNSPECIFIED("SAAAYAM-1309"),

    // Server error codes (1400-1499)
    INTERNAL_SERVER_ERROR("SAAAYAM-1400"),
    DATABASE_ERROR("SAAAYAM-1401"),
    EXTERNAL_SERVICE_ERROR("SAAAYAM-1402"),
    UNEXPECTED_ERROR("SAAAYAM-1403");

    private final String code;

    public String getLocalizedMessage() {
        return MessageSourceUtil.getMessage(this.code, null);
    }
}
