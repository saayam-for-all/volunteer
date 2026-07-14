// ZoomService.java
package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateMeetingRequest;
import org.sfa.volunteer.dto.response.MeetingResponse;

public interface ZoomService {
    MeetingResponse createMeeting(CreateMeetingRequest request);
    String getAccessToken();
}