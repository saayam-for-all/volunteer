package org.sfa.volunteer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AvailabilityConfiguration {
    @Bean
    public Clock availabilityClock() {
        return Clock.systemUTC();
    }
}
