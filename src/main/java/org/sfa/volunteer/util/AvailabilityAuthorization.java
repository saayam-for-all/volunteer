package org.sfa.volunteer.util;

import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Caller email must come from a verified principal or API Gateway authorizer, never a header/body. */
public final class AvailabilityAuthorization {
    private AvailabilityAuthorization() { }

    public static void requireOwner(UserService users, String userId, String email) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId must not be blank");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to manage availability");
        }
        if (!users.userExists(userId)) throw new UserNotFoundException(userId);
        if (!users.isEmailOwnedByUser(userId, email.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this availability");
        }
    }
}
