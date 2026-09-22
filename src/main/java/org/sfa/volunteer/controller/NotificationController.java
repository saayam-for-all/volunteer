package org.sfa.volunteer.controller;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/0.0.1/notifications")
public class NotificationController {

        private final NotificationService notificationService;
        private final ResponseBuilder responseBuilder;

        @Autowired
        public NotificationController(NotificationService notificationService, ResponseBuilder responseBuilder) {
                this.notificationService = notificationService;
                this.responseBuilder = responseBuilder;

        }

        @PostMapping("/counts")
        public SaayamResponse<GetNotificationsResponse> getNotificationCounts(
                        @Valid @RequestBody GetNotificationsRequest request) throws Exception {

                GetNotificationsResponse response = notificationService.getNotificationCounts(request);
                return responseBuilder.buildSuccessResponse(
                                SaayamStatusCode.SUCCESS,
                                response);
        }

        @PostMapping("/details")
        public SaayamResponse<GetNotificationsResponse> getNotifications(
                        @Valid @RequestBody GetNotificationsRequest request)
                        throws Exception {

                validateRequestParameters(request);
                GetNotificationsResponse response = notificationService.getNotifications(request);
                return responseBuilder.buildSuccessResponse(
                                SaayamStatusCode.SUCCESS,
                                response);
        }

        @PostMapping("/lastseen")
        public SaayamResponse<UpsertLastSeenResponse> updateLastSeen(
                        @Valid @RequestBody UpsertLastSeenRequest request) throws Exception {
                String userId = request.userId();
                if (userId == null || userId.isBlank()) {
                        throw new NotificationException(
                                        SaayamStatusCode.USER_NOT_FOUND.toString(),
                                        userId);
                }

                UpsertLastSeenResponse response = notificationService.upsertLastSeen(request);
                return responseBuilder.buildSuccessResponse(
                                SaayamStatusCode.SUCCESS,
                                response);
        }

        private void validateRequestParameters(GetNotificationsRequest request) {

                String userId = request.userId();

                // Null check
                if (request.rowStart() == null || request.rowEnd() == null) {
                        throw new NotificationException(
                                        SaayamStatusCode.BAD_REQUEST.toString(),
                                        userId);
                }
                // Non-numeric or blank check
                try {
                        Integer.parseInt(String.valueOf(request.rowStart()));
                        Integer.parseInt(String.valueOf(request.rowEnd()));
                } catch (NumberFormatException ex) {
                        throw new NotificationException(
                                        SaayamStatusCode.BAD_REQUEST.toString(),
                                        userId);
                }

                int rowStart = request.rowStart();
                int rowEnd = request.rowEnd();
                if (rowStart < 0 || rowEnd < 0 || rowStart > rowEnd) {
                        throw new NotificationException(
                                        SaayamStatusCode.INVALID_PARAMETER.toString(),
                                        userId);
                }
        }
}