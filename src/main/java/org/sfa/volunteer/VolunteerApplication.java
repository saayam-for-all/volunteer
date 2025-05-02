package org.sfa.volunteer;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VolunteerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolunteerApplication.class, args);
    }
//    @PostConstruct
//    public void afterStartup() {
//        System.out.println("✅ Backend is alive and running!");
//    }
}
