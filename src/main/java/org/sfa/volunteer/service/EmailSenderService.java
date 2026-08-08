package org.sfa.volunteer.service;

public interface EmailSenderService {
    void sendEmail(String to, String subject, String body);
}
