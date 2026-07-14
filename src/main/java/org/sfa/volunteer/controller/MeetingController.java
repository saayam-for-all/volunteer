package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateMeetingRequest;
import org.sfa.volunteer.dto.response.MeetingResponse;
import org.sfa.volunteer.service.ZoomService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/0.0.1/meetings")
public class MeetingController {

    private final ZoomService zoomService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public MeetingController(ZoomService zoomService, ResponseBuilder responseBuilder) {
        this.zoomService = zoomService;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping
    public SaayamResponse<MeetingResponse> createMeeting(@Valid @RequestBody CreateMeetingRequest request) {
        MeetingResponse response = zoomService.createMeeting(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, response);
    }
}