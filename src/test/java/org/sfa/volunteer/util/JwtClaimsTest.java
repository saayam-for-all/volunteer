package org.sfa.volunteer.util;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.exception.ForbiddenException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtClaimsTest {

    /** Builds a token whose payload is the given JSON, matching Cognito's base64url encoding. */
    private static String tokenWith(String payloadJson) {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return "header." + payload + ".signature";
    }

    @Test
    void readsEmailAndGroupsFromArray() {
        JwtClaims claims = JwtClaims.parse(tokenWith(
                "{\"email\":\"laxmi@example.com\",\"cognito:groups\":[\"volunteer\",\"admin\"]}"));

        assertEquals("laxmi@example.com", claims.email());
        assertEquals("volunteer,admin", claims.groups());
    }

    @Test
    void readsGroupsGivenAsString() {
        JwtClaims claims = JwtClaims.parse(tokenWith(
                "{\"email\":\"laxmi@example.com\",\"cognito:groups\":\"admin\"}"));

        assertEquals("admin", claims.groups());
    }

    @Test
    void groupsAreNullWhenClaimAbsent() {
        JwtClaims claims = JwtClaims.parse(tokenWith("{\"email\":\"laxmi@example.com\"}"));

        assertNull(claims.groups());
    }

    @Test
    void fallsBackToCognitoUsernameWhenEmailMissing() {
        JwtClaims claims = JwtClaims.parse(tokenWith(
                "{\"cognito:username\":\"laxmi@example.com\"}"));

        assertEquals("laxmi@example.com", claims.email());
    }

    @Test
    void fallsBackToUsernameWhenEmailAndCognitoUsernameMissing() {
        JwtClaims claims = JwtClaims.parse(tokenWith("{\"username\":\"laxmi@example.com\"}"));

        assertEquals("laxmi@example.com", claims.email());
    }

    @Test
    void stripsBearerPrefix() {
        JwtClaims claims = JwtClaims.parse(
                "Bearer " + tokenWith("{\"email\":\"laxmi@example.com\"}"));

        assertEquals("laxmi@example.com", claims.email());
    }

    @Test
    void emailIsNullWhenNoEmailClaimPresent() {
        JwtClaims claims = JwtClaims.parse(tokenWith("{\"sub\":\"abc-123\"}"));

        assertNull(claims.email());
    }

    @Test
    void rejectsNullHeader() {
        assertThrows(ForbiddenException.class, () -> JwtClaims.parse(null));
    }

    @Test
    void rejectsBlankHeader() {
        assertThrows(ForbiddenException.class, () -> JwtClaims.parse("   "));
    }

    @Test
    void rejectsTokenWithoutThreeSegments() {
        assertThrows(ForbiddenException.class, () -> JwtClaims.parse("header.payload"));
    }

    @Test
    void rejectsUndecodablePayload() {
        assertThrows(ForbiddenException.class, () -> JwtClaims.parse("header.!!!not-base64!!!.sig"));
    }

    @Test
    void rejectsPayloadThatIsNotJson() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("not json at all".getBytes(StandardCharsets.UTF_8));
        assertThrows(ForbiddenException.class, () -> JwtClaims.parse("header." + payload + ".sig"));
    }
}