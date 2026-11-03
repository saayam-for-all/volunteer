package org.sfa.volunteer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.sfa.volunteer.dto.request.OnboardingAvailabilityReadRequest;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySlot;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;

import java.util.ArrayList;

/** Binds the same strict Step 4 payload for Spring and Lambda. */
public final class OnboardingAvailabilityPayload {
    private static final ObjectReader JSON = new ObjectMapper().reader().with(
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    private OnboardingAvailabilityPayload() {}

    public static JsonNode object(String body) {
        if (body == null) throw badRequest("A JSON object body is required");
        try {
            JsonNode node = JSON.readTree(body);
            if (!node.isObject()) throw badRequest("A JSON object body is required");
            return node;
        } catch (JsonProcessingException failure) {
            throw badRequest("A valid JSON object body is required");
        }
    }

    public static OnboardingAvailabilityReadRequest read(JsonNode body) {
        return new OnboardingAvailabilityReadRequest(text(body, "userId"));
    }

    /** Include coercible values so the strict Step 4 parser rejects them before legacy binding. */
    public static boolean targetsStepFour(JsonNode body) {
        return body.path("step").asInt(Integer.MIN_VALUE) == 4;
    }

    public static OnboardingAvailabilitySaveRequest save(JsonNode body) {
        var step = body.path("step");
        var emergency = body.path("isEmergencyAvailable");
        var availability = body.path("availability");
        if (!step.isIntegralNumber() || !step.canConvertToInt()) throw badRequest("step must be an integer");
        if (!emergency.isBoolean()) throw badRequest("isEmergencyAvailable must be an explicit boolean");
        if (!availability.isArray()) throw badRequest("availability must be an array of local HH:mm slots");
        var slots = new ArrayList<OnboardingAvailabilitySlot>();
        for (JsonNode slot : availability) {
            slots.add(new OnboardingAvailabilitySlot(text(slot, "dayOfWeek"),
                    text(slot, "startTime"), text(slot, "endTime")));
        }
        return new OnboardingAvailabilitySaveRequest(text(body, "userId"), step.intValue(), slots,
                emergency.booleanValue());
    }

    private static String text(JsonNode body, String field) {
        var value = body.path(field);
        if (!value.isTextual()) throw badRequest(field + " must be a string");
        return value.textValue();
    }

    private static OnboardingAvailabilityException badRequest(String message) {
        return new OnboardingAvailabilityException(400, "BAD_REQUEST", message);
    }
}
