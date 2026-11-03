package org.sfa.volunteer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.response.OnboardingAvailabilityResponse;
import org.sfa.volunteer.util.OnboardingAvailabilityApi;
import org.sfa.volunteer.util.OnboardingAvailabilityAuthorization;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OnboardingAvailabilityController.PATH)
public class OnboardingAvailabilityController {
    public static final String PATH = "/0.0.1/volunteers/onboarding/availability";

    private final OnboardingAvailabilityApi api;

    public OnboardingAvailabilityController(OnboardingAvailabilityApi api) {
        this.api = api;
    }

    @PostMapping
    public ResponseEntity<SaayamResponse<OnboardingAvailabilityResponse>> read(@RequestBody(required = false) String body,
            HttpServletRequest request) {
        return respond(api.read(body, OnboardingAvailabilityAuthorization.caller(request)));
    }

    @PutMapping
    public ResponseEntity<SaayamResponse<OnboardingAvailabilityResponse>> save(@RequestBody(required = false) String body,
            HttpServletRequest request) {
        return respond(api.save(body, OnboardingAvailabilityAuthorization.caller(request)));
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent().allow(HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS).build();
    }

    @RequestMapping
    public ResponseEntity<SaayamResponse<Void>> unsupportedMethod() {
        return ResponseEntity.status(405).allow(HttpMethod.POST, HttpMethod.PUT, HttpMethod.OPTIONS)
                .body(OnboardingAvailabilityApi.error(405, "METHOD_NOT_ALLOWED", "Method not allowed"));
    }

    static <T> ResponseEntity<SaayamResponse<T>> respond(SaayamResponse<T> envelope) {
        return ResponseEntity.status(envelope.statusCode()).body(envelope);
    }
}
