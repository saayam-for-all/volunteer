package org.sfa.volunteer.util;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.springframework.stereotype.Component;

@Component
public class OnboardingAvailabilityAuthorization {
    public record Caller(String subject, String email, boolean emailVerified) {}

    private final AvailabilityAccountRepository accounts;

    public OnboardingAvailabilityAuthorization(AvailabilityAccountRepository accounts) {
        this.accounts = accounts;
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
        OnboardingAvailabilityPolicy.requireUserId(userId);
        var account = accounts.findById(userId).orElseThrow(() ->
                new OnboardingAvailabilityException(404, "USER_NOT_FOUND", "Account not found"));
        if (account.email() == null || !account.email().strip().equalsIgnoreCase(caller.email().strip())) {
            throw forbidden();
        }
    }

    private static OnboardingAvailabilityException forbidden() {
        return new OnboardingAvailabilityException(403, "FORBIDDEN",
                "A verified account owner is required");
    }
}
