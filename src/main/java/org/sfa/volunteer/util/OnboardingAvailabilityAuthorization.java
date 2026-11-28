package org.sfa.volunteer.util;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.stereotype.Component;

@Component
public class OnboardingAvailabilityAuthorization {
    public record Caller(String subject, String email, boolean emailVerified) {}

    private final UserRepository accounts;
    private final VolunteerService volunteers;

    public OnboardingAvailabilityAuthorization(UserRepository accounts, VolunteerService volunteers) {
        this.accounts = accounts;
        this.volunteers = volunteers;
    }

    public void requireOwner(String userId, HttpServletRequest request) {
        requireOwner(userId, caller(request));
    }

    public static Caller caller(HttpServletRequest request) {
        if (request.getAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY) instanceof AwsProxyRequestContext context
                && context.getAuthorizer() != null && context.getAuthorizer().getClaims() != null) {
            var claims = context.getAuthorizer().getClaims();
            return new Caller(claims.getSubject(), claims.getEmail(), claims.isEmailVerified());
        }
        return null;
    }

    /** Identity values must come from the trusted REST API Gateway authorizer context. */
    public void requireOwner(String userId, Caller caller) {
        if (caller == null || caller.subject() == null || caller.subject().isBlank()
                || caller.email() == null || caller.email().isBlank() || !caller.emailVerified()) {
            throw forbidden();
        }
        if (userId == null || userId.isBlank()) throw new OnboardingAvailabilityException(400, "BAD_REQUEST", "userId is required");
        volunteers.requireOnboardingAvailabilityReady();
        var account = accounts.findAvailabilityAccountById(userId).orElseThrow(() ->
                new OnboardingAvailabilityException(404, "USER_NOT_FOUND", "Account not found"));
        if (account.getPrimaryEmailAddress() == null || !account.getPrimaryEmailAddress().strip().equalsIgnoreCase(caller.email().strip())) {
            throw forbidden();
        }
    }

    private static OnboardingAvailabilityException forbidden() {
        return new OnboardingAvailabilityException(403, "FORBIDDEN",
                "A verified account owner is required");
    }
}
