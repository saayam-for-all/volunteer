package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.GetNotificationPreferencesRequest;
import org.sfa.volunteer.dto.request.UpdateNotificationPreferencesRequest;
import org.sfa.volunteer.dto.response.GetNotificationPreferencesResponse;
import org.sfa.volunteer.dto.response.UpdateNotificationPreferencesResponse;

/**
 * Per-channel notification delivery preferences.
 *
 * <p>Backs the settings control on the notification centre, which previously had no
 * API behind it at all.
 */
public interface NotificationPreferenceService {

    GetNotificationPreferencesResponse getPreferences(GetNotificationPreferencesRequest request);

    UpdateNotificationPreferencesResponse updatePreferences(UpdateNotificationPreferencesRequest request);
}
