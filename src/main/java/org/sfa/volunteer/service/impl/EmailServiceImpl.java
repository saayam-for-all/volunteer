package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendMeetingInvites(List<String> attendeeEmails, String topic, String joinUrl, String startTime) {
        for (String email : attendeeEmails) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("noreply@saayamforall.org");
                message.setTo(email);
                message.setSubject("Zoom Meeting Invite: " + topic);
                message.setText(
                        "You've been invited to a meeting.\n\n" +
                                "Topic: " + topic + "\n" +
                                "When: " + startTime + "\n" +
                                "Join URL: " + joinUrl + "\n");
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Failed to send invite to " + email + " - " + e.getMessage());
            }
        }
    }
}