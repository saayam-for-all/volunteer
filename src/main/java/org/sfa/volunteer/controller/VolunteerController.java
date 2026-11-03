package org.sfa.volunteer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.OnboardingAvailabilityResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.OnboardingAvailabilityApi;
import org.sfa.volunteer.util.OnboardingAvailabilityAuthorization;
import org.sfa.volunteer.util.OnboardingAvailabilityPayload;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/0.0.1/volunteers")
public class VolunteerController {
    private final VolunteerService volunteerService;
    private final ResponseBuilder responseBuilder;
    private final OnboardingAvailabilityApi onboarding;
    private final OnboardingAvailabilityAuthorization authorization;
    private final ObjectMapper json;
    private final Validator validator;

    @Autowired
    public VolunteerController(VolunteerService volunteerService, ResponseBuilder responseBuilder,
            OnboardingAvailabilityApi onboarding, OnboardingAvailabilityAuthorization authorization,
            ObjectMapper json, Validator validator) {
        this.volunteerService = volunteerService;
        this.responseBuilder = responseBuilder;
        this.onboarding = onboarding;
        this.authorization = authorization;
        this.json = json;
        this.validator = validator;
    }

    @PostMapping("/createvolunteer")
    public SaayamResponse<VolunteerResponse> createVolunteer(@Valid @RequestBody VolunteerRequest request)
            throws Exception {
        VolunteerResponse response = volunteerService.createVolunteer(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_CREATED, response);
    }

    @PutMapping("/updatevolunteer")
    public ResponseEntity<?> updateVolunteer(@RequestBody(required = false) String rawBody, HttpServletRequest servletRequest)
            throws Exception {
        JsonNode body;
        try {
            body = OnboardingAvailabilityPayload.object(rawBody);
        } catch (OnboardingAvailabilityException failure) {
            return OnboardingAvailabilityController.respond(OnboardingAvailabilityApi.error(
                    failure.getHttpStatus(), failure.getErrorCode(), failure.getMessage()));
        }
        if (OnboardingAvailabilityPayload.targetsStepFour(body)) {
            return OnboardingAvailabilityController.respond(onboarding.save(body,
                    OnboardingAvailabilityAuthorization.caller(servletRequest)));
        }

        VolunteerRequest request;
        try {
            request = json.treeToValue(body, VolunteerRequest.class);
        } catch (JsonProcessingException failure) {
            throw new HttpMessageNotReadableException("Invalid volunteer request", new ServletServerHttpRequest(servletRequest));
        }
        if (Integer.valueOf(4).equals(request.step())) {
            return OnboardingAvailabilityController.respond(OnboardingAvailabilityApi.error(
                    400, "BAD_REQUEST", "step must be an integer for onboarding availability"));
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(violation -> violation.getMessage()).collect(Collectors.joining("; "));
            return ResponseEntity.ok(responseBuilder.buildErrorResponse(400, SaayamStatusCode.BAD_REQUEST, message));
        }
        VolunteerResponse response = volunteerService.updateVolunteer(request);
        return ResponseEntity.ok(responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED,
                new Object[] { request.userId() }, response));
    }

    @PutMapping("/updatestep1")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep1(@Valid @RequestBody VolunteerRequest request)
            throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep1(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED,
                new Object[] { request.userId() }, response);
    }

    @PutMapping("/updatestep2")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep2(@Valid @RequestBody VolunteerRequest request)
            throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep2(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED,
                new Object[] { request.userId() }, response);
    }

    @PutMapping("/updatestep3")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep3(@Valid @RequestBody VolunteerRequest request)
            throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep3(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED,
                new Object[] { request.userId() }, response);
    }

    @PutMapping("/updatestep4")
    public ResponseEntity<SaayamResponse<OnboardingAvailabilityResponse>> updateVolunteerStep4(
            @RequestBody(required = false) String body, HttpServletRequest request) {
        return OnboardingAvailabilityController.respond(onboarding.save(body,
                OnboardingAvailabilityAuthorization.caller(request)));
    }

    @PutMapping("/updatecompletion")
    public ResponseEntity<SaayamResponse<VolunteerResponse>> updateVolunteerCompletion(
            @RequestBody(required = false) String rawBody, HttpServletRequest servletRequest) {
        return OnboardingAvailabilityController.respond(OnboardingAvailabilityApi.execute(() -> {
            JsonNode body = OnboardingAvailabilityPayload.object(rawBody);
            String userId = OnboardingAvailabilityPayload.read(body).userId();
            JsonNode step = body.get("step");
            if (step != null && (!step.isIntegralNumber() || !step.canConvertToInt())) {
                throw new OnboardingAvailabilityException(400, "BAD_REQUEST", "step must be an integer");
            }
            authorization.requireOwner(userId, servletRequest);
            var request = VolunteerRequest.builder().userId(userId).step(step == null ? null : step.intValue()).build();
            return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED,
                    new Object[] {userId}, volunteerService.updateVolunteerCompletion(request));
        }));
    }

    @GetMapping("/all")
    public SaayamResponse<PaginationResponse<VolunteerResponse>> getVolunteersWithPagination(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        PaginationResponse<VolunteerResponse> response = volunteerService.findAllVolunteersWithPagination(page, size);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, response);
    }

    @GetMapping("/{userId}")
    public SaayamResponse<VolunteerResponse> getVolunteerDetails(@PathVariable String userId) throws Exception {
        VolunteerResponse response = volunteerService.getVolunteerByUserId(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[] { userId }, response);
    }
}
