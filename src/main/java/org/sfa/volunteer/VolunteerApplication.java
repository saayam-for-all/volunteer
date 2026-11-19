package org.sfa.volunteer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication(scanBasePackages = { "org.sfa.volunteer",
        "org.sfa.volunteer1" })
public class VolunteerApplication {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(VolunteerApplication.class, args);
    }

}
