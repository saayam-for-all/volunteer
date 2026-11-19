// package org.sfa.volunteer.controller;

// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// @RestController
// @RequestMapping("/0.0.1/stewards")
// public class StewardController {
    
// }
package org.sfa.volunteer.controller;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.StewardVolunteerResponse;
import org.sfa.volunteer.service.StewardService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/0.0.1/stewards")
public class StewardController {

    private final StewardService stewardService;
    private final ResponseBuilder responseBuilder;

    public StewardController(
            StewardService stewardService,
            ResponseBuilder responseBuilder
    ) {
        this.stewardService = stewardService;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/volunteers")
    public SaayamResponse<PaginationResponse<StewardVolunteerResponse>>
    getVolunteers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        PaginationResponse<StewardVolunteerResponse> response =
                stewardService.getVolunteers(page, size);

        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS,
                response
        );
    }
}