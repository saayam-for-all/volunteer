package org.sfa.volunteer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sfa.volunteer.exception.ForbiddenException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public record JwtClaims(String email, String groups) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JwtClaims parse(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ForbiddenException("Missing Authorization header");
        }

        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : authorization.trim();

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ForbiddenException("Malformed JWT");
        }

        Map<String, Object> claims;
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            claims = MAPPER.readValue(new String(payload, StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            throw new ForbiddenException("Could not read JWT claims");
        }

        String email = asString(claims.get("email"));
        if (isBlank(email)) email = asString(claims.get("cognito:username"));
        if (isBlank(email)) email = asString(claims.get("username"));

        String groups = null;
        Object g = claims.get("cognito:groups");
        if (g instanceof String s) {
            groups = s;
        } else if (g instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            for (Object o : it) {
                if (sb.length() > 0) sb.append(",");
                sb.append(String.valueOf(o));
            }
            groups = sb.toString();
        }

        return new JwtClaims(email, groups);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}