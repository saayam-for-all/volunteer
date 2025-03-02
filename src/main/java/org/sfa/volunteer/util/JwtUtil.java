package org.sfa.volunteer.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${cognito.public-key}") String secret) {
        byte[] decodedKey = secret.getBytes(StandardCharsets.UTF_8); // Use UTF-8 encoding
        this.key = Keys.hmacShaKeyFor(decodedKey);
        System.out.println("🔐 Decoded Key Length: " + (decodedKey.length * 8) + " bits"); // Debugging
    }

    public Claims extractClaims(String token) {
        try {
            System.out.println("🔹 Verifying JWT: " + token);
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.err.println("🚨 Invalid JWT: " + e.getMessage());
            throw new RuntimeException("Invalid JWT Token");
        }
    }

    public String extractCountry(String token) {
        return extractClaims(token).get("custom:Country", String.class);
    }
}