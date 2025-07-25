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

    @Value("${cors.allowed.headers}")
    private String allowedHeaders;

    @Value("${cors.allowed.methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed.credentials}")
    private boolean allowedCredentials;

    @Bean
    public WebMvcConfigurer corsConfig(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigin)
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders)
                        .allowCredentials(allowedCredentials);
            }
        };
    }
}
