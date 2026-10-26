package org.sfa.volunteer.serviceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.request.GetNotificationPreferencesRequest;
import org.sfa.volunteer.dto.request.NotificationPreferenceUpdate;
import org.sfa.volunteer.dto.request.UpdateNotificationPreferencesRequest;
import org.sfa.volunteer.dto.response.GetNotificationPreferencesResponse;
import org.sfa.volunteer.dto.response.UpdateNotificationPreferencesResponse;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.repository.UserNotificationPreferenceRepository;
import org.sfa.volunteer.repository.projection.NotificationPreferenceRow;
import org.sfa.volunteer.service.impl.NotificationPreferenceServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behaviour of the notification-preferences API, which backs the settings control
 * on the notification centre.
 */
class NotificationPreferenceServiceImplTest {

    private static final String USER = "SID-00-000-001";

    private UserNotificationPreferenceRepository repository;
    private NotificationPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UserNotificationPreferenceRepository.class);
        service = new NotificationPreferenceServiceImpl(repository);
    }

    /** Plain stub rather than a Mockito mock, so rows can be built inline in when(). */
    private record Row(Integer channelId, String channelName, String description, String preference)
            implements NotificationPreferenceRow {
        @Override
        public Integer getChannelId() {
            return channelId;
        }

        @Override
        public String getChannelName() {
            return channelName;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getPreference() {
            return preference;
        }
    }

    private static List<NotificationPreferenceRow> allChannels(String emailPreference) {
        return List.of(
                new Row(1, "EMAIL", "Outbound email", emailPreference),
                new Row(2, "SMS", "Outbound text", null),
                new Row(3, "PUSH", "Mobile push", null),
                new Row(4, "IN_APP", "In-app centre", null));
    }

    // ---------- read ----------

    /** Every channel is returned, including ones the user has never configured. */
    @Test
    void getReturnsEveryChannelIncludingUnsetOnes() {
        when(repository.findPreferencesForUser(USER)).thenReturn(allChannels(null));

        GetNotificationPreferencesResponse response =
                service.getPreferences(new GetNotificationPreferencesRequest(USER));

        assertEquals(USER, response.userId());
        assertEquals(4, response.preferences().size());
        assertEquals("EMAIL", response.preferences().get(0).channelName());
        assertNull(response.preferences().get(0).preference(), "an unset channel reports a null preference");
    }

    @Test
    void getRejectsABlankUserId() {
        assertThrows(NotificationException.class,
                () -> service.getPreferences(new GetNotificationPreferencesRequest("  ")));
        assertThrows(NotificationException.class,
                () -> service.getPreferences(new GetNotificationPreferencesRequest(null)));
    }

    // ---------- write ----------

    /** No row yet: the update finds nothing to change, so a row is inserted. */
    @Test
    void updateInsertsWhenNoRowExists() {
        when(repository.channelExists(1)).thenReturn(1);
        when(repository.updatePreference(USER, 1, "both")).thenReturn(0);
        when(repository.findPreferencesForUser(USER)).thenReturn(allChannels("both"));

        UpdateNotificationPreferencesResponse response = service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER,
                        List.of(new NotificationPreferenceUpdate(1, "both"))));

        verify(repository).insertPreference(USER, 1, "both");
        assertEquals(1, response.created());
        assertEquals(0, response.updated());
    }

    /** Row already there: update it, do not insert a duplicate. */
    @Test
    void updateUpdatesWhenARowAlreadyExists() {
        when(repository.channelExists(1)).thenReturn(1);
        when(repository.updatePreference(USER, 1, "email")).thenReturn(1);
        when(repository.findPreferencesForUser(USER)).thenReturn(allChannels("email"));

        UpdateNotificationPreferencesResponse response = service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER,
                        List.of(new NotificationPreferenceUpdate(1, "email"))));

        verify(repository, never()).insertPreference(anyString(), anyInt(), anyString());
        assertEquals(0, response.created());
        assertEquals(1, response.updated());
    }

    /** A null preference clears the channel rather than being rejected. */
    @Test
    void aNullPreferenceClearsTheChannel() {
        when(repository.channelExists(1)).thenReturn(1);
        when(repository.updatePreference(USER, 1, null)).thenReturn(1);
        when(repository.findPreferencesForUser(USER)).thenReturn(allChannels(null));

        service.updatePreferences(new UpdateNotificationPreferencesRequest(USER,
                List.of(new NotificationPreferenceUpdate(1, null))));

        verify(repository).updatePreference(USER, 1, null);
    }

    @Test
    void preferenceValueIsCaseInsensitiveAndNormalisedToLowerCase() {
        when(repository.channelExists(1)).thenReturn(1);
        when(repository.updatePreference(eq(USER), eq(1), anyString())).thenReturn(1);
        when(repository.findPreferencesForUser(USER)).thenReturn(allChannels("both"));

        service.updatePreferences(new UpdateNotificationPreferencesRequest(USER,
                List.of(new NotificationPreferenceUpdate(1, "  BoTh "))));

        // The database enum spells these lower case; anything else fails the cast.
        verify(repository).updatePreference(USER, 1, "both");
    }

    /**
     * An unusable value is rejected before it reaches the database, so the caller
     * gets INVALID_PARAMETER instead of an opaque enum-cast failure.
     */
    @Test
    void anUnknownPreferenceValueIsRejectedBeforeHittingTheDatabase() {
        when(repository.channelExists(1)).thenReturn(1);

        assertThrows(NotificationException.class, () -> service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER,
                        List.of(new NotificationPreferenceUpdate(1, "carrier-pigeon")))));

        verify(repository, never()).updatePreference(anyString(), anyInt(), any());
        verify(repository, never()).insertPreference(anyString(), anyInt(), any());
    }

    /**
     * channel_id is a foreign key, so an unknown one would otherwise surface as a
     * constraint violation rather than a useful error.
     */
    @Test
    void anUnknownChannelIsRejectedBeforeHittingTheDatabase() {
        when(repository.channelExists(999)).thenReturn(0);

        assertThrows(NotificationException.class, () -> service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER,
                        List.of(new NotificationPreferenceUpdate(999, "email")))));

        verify(repository, never()).updatePreference(anyString(), anyInt(), any());
        verify(repository, never()).insertPreference(anyString(), anyInt(), any());
    }

    @Test
    void updateRejectsAnEmptyPreferenceList() {
        assertThrows(NotificationException.class, () -> service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER, List.of())));
        assertThrows(NotificationException.class, () -> service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER, null)));
    }

    @Test
    void updateRejectsANullChannelId() {
        assertThrows(NotificationException.class, () -> service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER,
                        List.of(new NotificationPreferenceUpdate(null, "email")))));
    }

    /** Several channels in one call are applied independently. */
    @Test
    void severalChannelsAreAppliedInOneCall() {
        when(repository.channelExists(anyInt())).thenReturn(1);
        when(repository.updatePreference(USER, 1, "both")).thenReturn(1);
        when(repository.updatePreference(USER, 3, "email")).thenReturn(0);
        when(repository.findPreferencesForUser(USER)).thenReturn(allChannels("both"));

        UpdateNotificationPreferencesResponse response = service.updatePreferences(
                new UpdateNotificationPreferencesRequest(USER, List.of(
                        new NotificationPreferenceUpdate(1, "both"),
                        new NotificationPreferenceUpdate(3, "email"))));

        verify(repository).updatePreference(USER, 1, "both");
        verify(repository).insertPreference(USER, 3, "email");
        assertEquals(1, response.created());
        assertEquals(1, response.updated());
    }

    @Test
    void updateRejectsABlankUserId() {
        assertThrows(NotificationException.class, () -> service.updatePreferences(
                new UpdateNotificationPreferencesRequest("", 
                        List.of(new NotificationPreferenceUpdate(1, "email")))));
    }
}
