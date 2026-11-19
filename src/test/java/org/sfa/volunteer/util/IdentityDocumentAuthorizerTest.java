
package org.sfa.volunteer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @Test
    void adminIsAllowedWithoutOwnershipLookup() {
        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(
                        users, TARGET, EMAIL, "admin"));

        verifyNoInteractions(users);
    }

    @Test
    void ownerIsAllowed() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(
                        users, TARGET, EMAIL, "volunteer"));

        verify(users).isEmailOwnedByUser(TARGET, EMAIL);
    }

    @Test
    void nonOwnerIsForbidden() {
        when(users.isEmailOwnedByUser(TARGET, EMAIL))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(
                        users, TARGET, EMAIL, "volunteer"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankEmailIsForbiddenWithoutLookup(String email) {
        assertThrows(ForbiddenException.class, () ->
                IdentityDocumentAuthorizer.requireOwnerOrAdmin(
                        users, TARGET, email, "volunteer"));

        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "admin | true",
            "volunteer,admin | true",
            "Admin | true",
            "volunteer | false"
    })
    void parsesGroups(String groups, boolean expected) {
        assertEquals(expected,
                IdentityDocumentAuthorizer.isAdmin(groups));
    }

    @Test
    void nullOrEmptyGroupsAreNotAdmin() {
        assertFalse(IdentityDocumentAuthorizer.isAdmin(null));
        assertFalse(IdentityDocumentAuthorizer.isAdmin(""));
    }
}
