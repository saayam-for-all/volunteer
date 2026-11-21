package org.sfa.volunteer.support;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/** Explicit opt-in fixture configuration; no runtime/team datasource properties are consumed. */
public final class PostgresTestDatabase {
    private PostgresTestDatabase() {}

    public static void configure(DynamicPropertyRegistry properties, String schema) {
        properties(schema).forEach((key, value) -> properties.add(key, () -> value));
    }

    public static Map<String, String> properties(String schema) {
        if (!Boolean.getBoolean("ba33.pg.enabled")) {
            throw new IllegalStateException("Native PostgreSQL tests require -Dba33.pg.enabled=true");
        }
        if (!schema.matches("ba33_[a-z0-9_]+")) {
            throw new IllegalArgumentException("An isolated ba33_ test schema is required");
        }
        String url = System.getProperty("ba33.pg.url", "jdbc:postgresql://127.0.0.1:55433/ba33_test");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Native tests require PostgreSQL on loopback");
        }
        URI endpoint = URI.create(url.substring("jdbc:".length()));
        if (!Set.of("127.0.0.1", "localhost", "[::1]").contains(endpoint.getHost())
                || !"/ba33_test".equals(endpoint.getPath()) || endpoint.getPort() < 1024
                || endpoint.getPort() == 5432 || endpoint.getQuery() != null || endpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("Use a dedicated loopback port and ba33_test database, with no URL options");
        }
        return Map.of(
                "spring.datasource.url", url + "?currentSchema=" + schema,
                "spring.datasource.username", System.getProperty("ba33.pg.user", "ba33_test"),
                "spring.datasource.password", System.getProperty("ba33.pg.password", ""),
                "spring.datasource.driver-class-name", "org.postgresql.Driver",
                "spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.hibernate.ddl-auto", "none");
    }
}
