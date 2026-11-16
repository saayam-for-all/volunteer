package org.sfa.volunteer.util;

import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.service.UserService;

public final class IdentityDocumentAuthorizer {
    private IdentityDocumentAuthorizer() { }

    /** email and groups come from a verified principal (JWT claims or API Gateway authorizer). */
    public static void requireOwnerOrAdmin(UserService users, String targetUserId,
                                           String email, String groups) {
        if (isAdmin(groups)) return;
        if (email == null || email.isBlank()) throw new ForbiddenException("JWT email missing");
        if (!users.isEmailOwnedByUser(targetUserId, email.trim())) {
            throw new ForbiddenException("Not allowed");
        }
    }

    static boolean isAdmin(String groups) {
        if (groups == null || groups.isBlank()) return false;
        for (String g : groups.split(",")) {
            if (g.trim().equalsIgnoreCase("admin")) return true;
        }
        return false;
    }
}