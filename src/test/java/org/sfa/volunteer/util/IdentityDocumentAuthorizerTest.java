package org.sfa.volunteer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.service.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityDocumentAuthorizerTest {

    private static final String TARGET = "SID-001";
    private static final String EMAIL = "owner@example.org";

    @Mock
    private UserService users;

    // ---------- owner / non-owner ----------

    @Test
    void adminIsAllowedWithoutOwnershipLookup() {
        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(users, TARGET, EMAIL, "Admins"));
        verifyNoInteractions(users);
    }

    @Test
    void ownerIsAllowed() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL)).thenReturn(true);
        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(users, TARGET, EMAIL, "Volunteers"));
    }

    @Test
    void nonOwnerIsForbidden() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL)).thenReturn(false);
        assertThrows(ForbiddenException.class, () ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(users, TARGET, EMAIL, "Volunteers"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankEmailIsForbiddenWithoutLookup(String email) {
        assertThrows(ForbiddenException.class, () ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(users, TARGET, email, "Volunteers"));
        verifyNoInteractions(users);
    }

    // ---------- admin group matching ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "Admins", "admins", "ADMINS",
            "SuperAdmins", "superadmins",
            "Super Admins",
            "Beneficiaries,Admins",
            "[Beneficiaries, Admins]",
            "[SuperAdmins]"
    })
    void recognisesRealAdminGroups(String groups) {
        assertTrue(IdentityDocumentAuthorizer.isAdmin(groups));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "admin",                 // the old guess; not a real Cognito group
            "Beneficiaries",
            "Volunteers",
            "Stewards",
            "admin-notifications",   // contains "admin" but is not an admin group
            "request-admin-viewers",
            "[Beneficiaries, us-east-1_hzvIMnDNi_Google]"
    })
    void rejectsEverythingElse(String groups) {
        assertFalse(IdentityDocumentAuthorizer.isAdmin(groups));
    }
}