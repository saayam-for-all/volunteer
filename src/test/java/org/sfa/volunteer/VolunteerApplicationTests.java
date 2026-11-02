package org.sfa.volunteer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

//@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=test", // Set to "test" to prevent GitHub secrets-masking bugs
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false",
    
    // Covering dot format mappings
    "cors.allowed.origin=https://test-saayam.netlify.app",
    "cors.allowed.methods=GET,POST,PUT,DELETE,OPTIONS",
    "cors.allowed.headers=Authorization,Content-Type",
    "cors.allowed.credentials=true",
    "cors.allow-credentials=true",
    
    // Covering dash format mappings
    "cors.allowed-origin=https://test-saayam.netlify.app",
    "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
    "cors.allowed-headers=Authorization,Content-Type",
    "cors.allowed-credentials=true",
    "cors.allow-credentials=true",

    // S3 configuration for profileImageStorageService (dummy values for context load)
    "saayam.s3.buckets.euPrivate=test-bucket-eu",
    "saayam.s3.buckets.usPrivate=test-bucket-us",
    "saayam.s3.region=us-east-1"

})
public class VolunteerApplicationTests {

    @Test
    void contextLoads() {
    }
}