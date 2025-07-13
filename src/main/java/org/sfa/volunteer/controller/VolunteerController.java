package org.sfa.volunteer.controller;
import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerUserAvailabilityResponse;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/0.0.1/volunteers")
public class VolunteerController {
    private final VolunteerService volunteerService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public VolunteerController(VolunteerService volunteerService, ResponseBuilder responseBuilder) {
        this.volunteerService = volunteerService;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping("/createvolunteer")
    public SaayamResponse<VolunteerResponse> createVolunteer(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.createVolunteer(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_CREATED, response);
    }

    @PutMapping("/updatevolunteer")
    public SaayamResponse<VolunteerResponse> updateVolunteer(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteer(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, new Object[]{request.userId()}, response);
    }

    @PutMapping("/updatestep1")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep1(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep1(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, new Object[]{request.userId()}, response);
    }

    @PutMapping("/updatestep2")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep2(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep2(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, new Object[]{request.userId()}, response);
    }

    @PutMapping("/updatestep3")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep3(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep3(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, new Object[]{request.userId()}, response);
    }

    @PutMapping("/updatestep4")
    public SaayamResponse<VolunteerResponse> updateVolunteerStep4(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerStep4(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, new Object[] { request.userId() }, response);
    }

    @PutMapping("/updatecompletion")
    public SaayamResponse<VolunteerResponse> updateVolunteerCompletion(@Valid @RequestBody VolunteerRequest request) throws Exception {
        VolunteerResponse response = volunteerService.updateVolunteerCompletion(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.VOLUNTEER_UPDATED, new Object[]{request.userId()}, response);
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
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }


    @GetMapping("getAvailability")
    public SaayamResponse<List<VolunteerUserAvailabilityResponse>> getVolunteerAvailability(
            @RequestParam(value = "userId", required = true) String userId) throws Exception {
        List<VolunteerUserAvailabilityResponse> response = volunteerService.getVolunteerUserAvailability(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

    @PutMapping("updateAvailability")
    public SaayamResponse<List<VolunteerUserAvailabilityResponse>> updateVolunteerAvailability(
            @RequestParam(value = "userId", required = true) String userId,
            @Valid @RequestBody List<VolunteerUserAvailabilityRequest> request) throws Exception {
        List<VolunteerUserAvailabilityResponse> response = volunteerService.updateVolunteerUserAvailability(userId, request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new Object[]{userId}, response);
    }

}
