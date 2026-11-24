package org.sfa.volunteer.util;

import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.service.UserService;

import java.util.Set;

public final class IdentityDocumentAuthorizer {

    // Exact Cognito group names (compared case-insensitively). Deliberately not a
    // substring match: identity documents are the most sensitive data in the
    // service, so a future group that merely contains "admin" must not get access.
    private static final Set<String> ADMIN_GROUPS = Set.of("admins", "superadmins", "super admins");

    private IdentityDocumentAuthorizer() { }

    /** email and groups come from a verified principal (JWT claims or API Gateway authorizer). */
    public static void requireOwnerOrAdmin(UserService users, String targetUserId,
                                           String email, String groups) {
        if (isAdmin(groups)) return;
        if (email == null || email.isBlank()) throw new ForbiddenException("Caller email missing");
        if (!users.isEmailOwnedByUser(targetUserId, email.trim())) {
            throw new ForbiddenException("Not allowed");
        }
    }

    static boolean isAdmin(String groups) {
        if (groups == null || groups.isBlank()) return false;
        // API Gateway may deliver the claim as "[Admins, Volunteers]".
        for (String g : groups.replace("[", "").replace("]", "").split(",")) {
            if (ADMIN_GROUPS.contains(g.trim().toLowerCase())) return true;
        }
        return false;
    }
}