package org.sfa.volunteer.service;


public interface EmailVerificationService {
    void sendVerificationCode(String email, Long userId, boolean useMagicLink);
    boolean verifyCode(String email, String code);
    boolean verifyToken(String token);
    void resendVerificationCode(String email, Long userId, boolean useMagicLink);
}
