package org.sfa.volunteer.util;

import java.util.HashMap;
import java.util.Map;

public class Cors {
    public static Map<String, String> headers(String methods) {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        h.put("Access-Control-Allow-Headers",
                "Content-Type,Authorization,X-Dev-Region,X-Amz-Date,X-Api-Key,X-Amz-Security-Token");
        h.put("Access-Control-Allow-Methods", methods);
        h.put("Content-Type", "application/json");
        return h;
    }
}