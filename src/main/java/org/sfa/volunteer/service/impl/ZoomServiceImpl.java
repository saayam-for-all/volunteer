// ZoomServiceImpl.java
package org.sfa.volunteer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.sfa.volunteer.dto.request.CreateMeetingRequest;
import org.sfa.volunteer.dto.response.MeetingResponse;
import org.sfa.volunteer.entities.Meeting;
import org.sfa.volunteer.entities.MeetingAttendee;
import org.sfa.volunteer.repository.MeetingRepository;
import org.sfa.volunteer.service.ZoomService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ZoomServiceImpl implements ZoomService {

    private final WebClient zoomWebClient;
    private final MeetingRepository meetingRepository;

    @Value("${zoom.oauth.account-id}")
    private String accountId;

    @Value("${zoom.oauth.client-id}")
    private String clientId;

    @Value("${zoom.oauth.client-secret}")
    private String clientSecret;

    public ZoomServiceImpl(WebClient zoomWebClient, MeetingRepository meetingRepository) {
        this.zoomWebClient = zoomWebClient;
        this.meetingRepository = meetingRepository;
    }

    @Override
    public String getAccessToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        JsonNode response = WebClient.create("https://zoom.us")
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/oauth/token")
                        .queryParam("grant_type", "account_credentials")
                        .queryParam("account_id", accountId)
                        .build())
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return response.get("access_token").asText();
    }

    @Override
    public MeetingResponse createMeeting(CreateMeetingRequest request) {
        String accessToken = getAccessToken();

        Map<String, Object> settings = new HashMap<>();
        settings.put("host_video", true);
        settings.put("participant_video", true);
        settings.put("join_before_host", false);
        settings.put("mute_upon_entry", true);
        settings.put("audio", "both");
        settings.put("approval_type", 0);           // Auto-approve registrants
        settings.put("registration_type", 1);        // Register once
        settings.put("registrants_email_notification", true);  // Zoom sends emails!

        Map<String, Object> meetingData = new HashMap<>();
        meetingData.put("topic", request.topic());
        meetingData.put("type", 2); // Scheduled meeting
        meetingData.put("start_time", request.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        meetingData.put("duration", request.durationMinutes());
        meetingData.put("timezone", "America/New_York");
        meetingData.put("settings", settings);

        JsonNode response = zoomWebClient.post()
                .uri("/users/me/meetings")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(meetingData)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        // Build and save entity
        Meeting meeting = Meeting.builder()
                .zoomMeetingId(response.get("id").asText())
                .topic(response.get("topic").asText())
                .joinUrl(response.get("join_url").asText())
                .startUrl(response.get("start_url").asText())
                .password(response.has("password") ? response.get("password").asText() : "")
                .startTime(request.startTime())
                .durationMinutes(request.durationMinutes())
                .hostUserId(request.hostUserId())
                .build();

        // Add attendees
        for (String email : request.attendeeEmails()) {
            MeetingAttendee attendee = MeetingAttendee.builder()
                    .userEmail(email)
                    .build();
            meeting.addAttendee(attendee);
        }

        // Persist
        Meeting savedMeeting = meetingRepository.save(meeting);

        return new MeetingResponse(
                savedMeeting.getId(),
                savedMeeting.getZoomMeetingId(),
                savedMeeting.getTopic(),
                savedMeeting.getJoinUrl(),
                savedMeeting.getStartUrl(),
                savedMeeting.getPassword(),
                savedMeeting.getStartTime(),
                savedMeeting.getDurationMinutes(),
                savedMeeting.getHostUserId(),
                request.attendeeEmails()
        );
    }

    private void addRegistrant(String meetingId, String email, String accessToken) {
        Map<String, Object> registrant = new HashMap<>();
        registrant.put("email", email);
        registrant.put("first_name", email.split("@")[0]); // Extract name from email

        try {
            zoomWebClient.post()
                    .uri("/meetings/" + meetingId + "/registrants")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registrant)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to register attendee: " + email + " - " + e.getMessage());
        }
    }
}