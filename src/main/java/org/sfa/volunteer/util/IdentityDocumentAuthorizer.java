package org.sfa.volunteer.util;

import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.service.UserService;

import java.util.Locale;
import java.util.Set;

public final class IdentityDocumentAuthorizer {

    // Exact Cognito group names (compared case-insensitively). Deliberately not a
    // substring match: identity documents are the most sensitive data in the
    // service, so a future group that merely contains "admin" must not get access.
    private static final Set<String> ADMIN_GROUPS =
            Set.of("admins", "superadmins", "super admins");

    // Stewards review volunteers and get read-only access (FR-19, pending D9).
    private static final Set<String> STEWARD_GROUPS = Set.of("stewards");

    private IdentityDocumentAuthorizer() { }

    /**
     * Upload / replace: the document's owner only, whatever their role.
     * Takes no groups, so no role can grant upload. Admins and stewards can
     * view documents but not edit them (FR-19).
     */
    public static void requireWriteAccess(UserService users, String targetUserId, String email) {
        requireCallerEmail(email);
        if (!isOwner(users, targetUserId, email)) {
            throw new ForbiddenException("Not allowed");
        }
    }

    /**
     * Metadata / file: the owner, an admin, or a steward (FR-19). Steward
     * access is not yet scoped to assigned volunteers (no assignment data
     * exists; D9).
     */
    public static void requireReadAccess(UserService users, String targetUserId,
                                         String email, String groups) {
        if (isAdmin(groups) || isSteward(groups)) return;
        requireCallerEmail(email);
        if (!isOwner(users, targetUserId, email)) {
            throw new ForbiddenException("Not allowed");
        }
    }

    static boolean isAdmin(String groups) {
        return hasAnyGroup(groups, ADMIN_GROUPS);
    }

    static boolean isSteward(String groups) {
        return hasAnyGroup(groups, STEWARD_GROUPS);
    }

    private static boolean hasAnyGroup(String groups, Set<String> wanted) {
        if (groups == null || groups.isBlank()) return false;
        // API Gateway may deliver the claim as "[Admins, Volunteers]".
        for (String g : groups.replace("[", "").replace("]", "").split(",")) {
            if (wanted.contains(g.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static void requireCallerEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ForbiddenException("Caller email missing");
        }
    }

    private static boolean isOwner(UserService users, String targetUserId, String email) {
        return users.isEmailOwnedByUser(targetUserId, email.trim());
    }
}