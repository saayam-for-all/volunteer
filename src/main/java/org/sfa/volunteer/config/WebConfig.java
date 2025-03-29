package org.sfa.volunteer.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                //.allowedOriginPatterns("http://*.localhost:517*")
                .allowedOrigins("http://localhost:5173","http://localhost:5174") // allow your frontend domain
                .allowedMethods("GET", "POST", "PUT", "DELETE") // specify allowed HTTP methods
                .allowedHeaders("*")
                .allowCredentials(true); // Allow cookies and credentials (if necessary)
    }
}