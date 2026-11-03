package org.sfa.volunteer.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sfa.volunteer.controller.OnboardingAvailabilityController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration {
    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders;

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${cors.credentials}")
    private boolean allowedCredentials;

    @Bean
    public WebMvcConfigurer corsConfig() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigin)
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders)
                        .allowCredentials(allowedCredentials);
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        // The MVC preflight handler still validates and writes the configured CORS headers.
                        if (CorsUtils.isPreFlightRequest(request)) response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        return true;
                    }
                }).addPathPatterns(OnboardingAvailabilityController.PATH);
            }
        };
    }
}
