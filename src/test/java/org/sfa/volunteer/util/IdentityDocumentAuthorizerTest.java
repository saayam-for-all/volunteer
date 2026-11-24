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

    // ---------- write: owner only ----------

    @Test
    void ownerCanWrite() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL)).thenReturn(true);
        assertDoesNotThrow(() -> IdentityDocumentAuthorizer.requireWriteAccess(users, TARGET, EMAIL));
    }

    @Test
    void nonOwnerCannotWrite() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL)).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> IdentityDocumentAuthorizer.requireWriteAccess(users, TARGET, EMAIL));
    }

    @Test
    void adminWhoIsNotOwnerCannotWrite() {
        // Write takes no groups at all: being an admin cannot grant upload (FR-19).
        when(users.isEmailOwnedByUser(TARGET, "admin@example.org")).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> IdentityDocumentAuthorizer.requireWriteAccess(users, TARGET, "admin@example.org"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankEmailCannotWrite(String email) {
        assertThrows(ForbiddenException.class,
                () -> IdentityDocumentAuthorizer.requireWriteAccess(users, TARGET, email));
        verifyNoInteractions(users);
    }

    // ---------- read: owner or admin ----------

    @Test
    void adminCanReadWithoutOwnershipLookup() {
        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireReadAccess(users, TARGET, EMAIL, "Admins"));
        verifyNoInteractions(users);
    }

    @Test
    void ownerCanRead() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL)).thenReturn(true);
        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireReadAccess(users, TARGET, EMAIL, "Volunteers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Volunteers", "Beneficiaries"})
    void nonOwnerNonAdminCannotRead(String groups) {
        when(users.isEmailOwnedByUser(TARGET, EMAIL)).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> IdentityDocumentAuthorizer.requireReadAccess(users, TARGET, EMAIL, groups));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankEmailNonAdminCannotRead(String email) {
        assertThrows(ForbiddenException.class,
                () -> IdentityDocumentAuthorizer.requireReadAccess(users, TARGET, email, "Volunteers"));
        verifyNoInteractions(users);
    }

    // ---------- admin group matching ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "Admins", "admins", "ADMINS", "SuperAdmins", "superadmins", "Super Admins",
            "Beneficiaries,Admins", "[Beneficiaries, Admins]", "[SuperAdmins]"
    })
    void recognisesRealAdminGroups(String groups) {
        assertTrue(IdentityDocumentAuthorizer.isAdmin(groups));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "admin", "Beneficiaries", "Volunteers", "Stewards",
            "admin-notifications", "request-admin-viewers",
            "[Beneficiaries, us-east-1_hzvIMnDNi_Google]"
    })
    void rejectsEverythingElse(String groups) {
        assertFalse(IdentityDocumentAuthorizer.isAdmin(groups));
    }

    // ---------- stewards: read only ----------

    @ParameterizedTest
    @ValueSource(strings = {"Stewards", "stewards", "[Volunteers, Stewards]"})
    void stewardCanReadWithoutOwnershipLookup(String groups) {
        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireReadAccess(users, TARGET, EMAIL, groups));
        verifyNoInteractions(users);
    }

    @Test
    void stewardWhoIsNotOwnerCannotWrite() {
        when(users.isEmailOwnedByUser(TARGET, "steward@example.org")).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> IdentityDocumentAuthorizer.requireWriteAccess(users, TARGET, "steward@example.org"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"steward", "stewards-pending", "Volunteers"})
    void lookAlikeGroupsAreNotStewards(String groups) {
        assertFalse(IdentityDocumentAuthorizer.isSteward(groups));
    }
}
