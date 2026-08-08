package org.sfa.volunteer.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * Utility class for getting friendly display names for timezone IDs.
 * Maps IANA timezone IDs (e.g., "America/New_York") to user-friendly names (e.g., "Eastern Time (US & Canada)").
 */
@Slf4j
@Component
public class TimezoneUtil {

    private static final String TIMEZONES_PROPERTIES = "timezones.properties";
    private Map<String, String> timezoneMap;

    public TimezoneUtil() {
        loadTimezoneMapping();
    }

    /**
     * Load timezone friendly names from properties file
     */
    private void loadTimezoneMapping() {
        timezoneMap = new HashMap<>();
        try {
            Properties props = new Properties();
            props.load(new ClassPathResource(TIMEZONES_PROPERTIES).getInputStream());
            
            props.forEach((key, value) -> {
                String keyStr = key.toString();
                if (keyStr.startsWith("timezone.")) {
                    String tzId = keyStr.substring("timezone.".length());
                    timezoneMap.put(tzId, value.toString());
                }
            });
            log.info("Loaded {} timezone mappings", timezoneMap.size());
        } catch (IOException e) {
            log.error("Error loading timezone properties", e);
        }
    }

    /**
     * Get friendly display name for a timezone ID.
     * Example: "America/New_York" → "Eastern Time (US & Canada)"
     * 
     * @param timezoneId The IANA timezone ID (e.g., "America/New_York")
     * @return Friendly display name for the timezone, or the timezone ID if not found in mappings
     */
    public String getFriendlyName(String timezoneId) {
        if (timezoneId == null || timezoneId.isEmpty()) {
            return timezoneId;
        }
        return timezoneMap.getOrDefault(timezoneId, timezoneId);
    }
}
