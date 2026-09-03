package org.sfa.volunteer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration

public class CorsConfiguration {
    @Value("${cors.allowed.origin}")
    private String allowedOrigin;

    // Note: allowedHeaders field kept for future use when restricting headers per
    // environment
    // Currently overridden in corsConfig() with "*" for compatibility
    @Value("${cors.allowed.headers}")
    private String[] allowedHeaders;

    @Value("${cors.allowed.methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed.credentials}")
    private boolean allowedCredentials;

    @Bean
    public WebMvcConfigurer corsConfig() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigin)
                        .allowedMethods(allowedMethods)
                        // TODO: Restrict to specific headers before production deployment
                        // Wildcard needed to support real browser auth headers (DNT, User-Agent, etc.)
                        // Recommended production list: Authorization, Content-Type, DNT, User-Agent,
                        // X-Requested-With
                        .allowedHeaders("*")
                        .allowCredentials(allowedCredentials);
            }
        };
    }
}
