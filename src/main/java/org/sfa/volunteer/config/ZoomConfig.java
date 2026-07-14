package org.sfa.volunteer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ZoomConfig {

    @Value("${zoom.api.base-url}")
    private String zoomApiBaseUrl;

    @Bean
    public WebClient zoomWebClient() {
        return WebClient.builder()
                .baseUrl(zoomApiBaseUrl)
                .build();
    }
}