package org.sfa.volunteer.service;

import java.util.List;

public interface EmailService {
    void sendMeetingInvites(List<String> attendeeEmails, String topic, String joinUrl, String startTime);
}