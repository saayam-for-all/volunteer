package org.sfa.volunteer.controller;
import jakarta.validation.Valid;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.NotificationRequest;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.NotificationCountResponse;
import org.sfa.volunteer.dto.response.NotificationPaginationResponse;
import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.service.VolunteerService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/0.0.1/volunteers")
public class VolunteerController {
    private static final int DEFAULT_NOTIFICATION_PAGE = 0;
    private static final int DEFAULT_NOTIFICATION_SIZE = 10;

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

    @PostMapping("/count/{userId}")
    public SaayamResponse<Long> getNotificationsCount(@PathVariable String userId) {
        Long notificationCount = volunteerService.getNotificationsCountAfterLastAccessed(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, notificationCount);
    }

    @PostMapping("/getNotificationsCount")
    public SaayamResponse<NotificationCountResponse> getNotificationsCount(@Valid @RequestBody NotificationRequest request) {
        Long notificationCount = volunteerService.getNotificationsCountAfterLastAccessed(request.userId());
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, new NotificationCountResponse(notificationCount));
    }

    //    Need to test this
//    http://localhost:8080/0.0.1/volunteers/getNotifications?userId=1
//    @GetMapping("/getNotifications")
//    public SaayamResponse<NotificationPaginationResponse<NotificationsResponse>> getAllNotifications(@RequestParam String userId, @RequestParam int page, @RequestParam int size, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime refTime) {
//        NotificationPaginationResponse<NotificationsResponse> notificationsList = volunteerService.getNotificationsList(userId, page, size, refTime);
//        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, notificationsList);
//    }

    @PostMapping("/getNotifications/{userId}")
    public SaayamResponse<NotificationPaginationResponse<NotificationsResponse>> getAllNotifications(@PathVariable String userId, @RequestParam int page, @RequestParam int size, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime refTime) throws Exception {
        try{
            NotificationPaginationResponse<NotificationsResponse> notificationsList = volunteerService.getNotificationsList(userId, page, size, refTime);
            return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, notificationsList);
        } catch (VolunteerException e){
            return  responseBuilder.buildErrorResponse(500, SaayamStatusCode.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/getNotificationsList")
    public SaayamResponse<NotificationPaginationResponse<NotificationsResponse>> getAllNotifications(
            @Valid @RequestBody NotificationRequest request,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "clientRefTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime clientRefTime) throws Exception {
        int pageNumber = request.page() != null ? request.page() : (page != null ? page : DEFAULT_NOTIFICATION_PAGE);
        int pageSize = request.size() != null ? request.size() : (size != null ? size : DEFAULT_NOTIFICATION_SIZE);
        LocalDateTime referenceTime = request.clientRefTime() != null ? request.clientRefTime() : clientRefTime;

        try{
            NotificationPaginationResponse<NotificationsResponse> notificationsList = volunteerService.getNotificationsList(request.userId(), pageNumber, pageSize, referenceTime);
            return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, notificationsList);
        } catch (VolunteerException e){
            return  responseBuilder.buildErrorResponse(500, SaayamStatusCode.BAD_REQUEST, e.getMessage());
        }
    }
}
