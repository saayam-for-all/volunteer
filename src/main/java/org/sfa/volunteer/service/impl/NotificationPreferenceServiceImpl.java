package org.sfa.volunteer.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.sfa.volunteer.dto.common.NotificationPreferenceType;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationPreferencesRequest;
import org.sfa.volunteer.dto.request.NotificationPreferenceUpdate;
import org.sfa.volunteer.dto.request.UpdateNotificationPreferencesRequest;
import org.sfa.volunteer.dto.response.GetNotificationPreferencesResponse;
import org.sfa.volunteer.dto.response.NotificationPreferenceResponse;
import org.sfa.volunteer.dto.response.UpdateNotificationPreferencesResponse;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.repository.UserNotificationPreferenceRepository;
import org.sfa.volunteer.repository.projection.NotificationPreferenceRow;
import org.sfa.volunteer.service.NotificationPreferenceService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceServiceImpl(UserNotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetNotificationPreferencesResponse getPreferences(GetNotificationPreferencesRequest request) {
        String userId = requireUserId(request.userId());

        List<NotificationPreferenceRow> rows = handleException(userId,
                () -> preferenceRepository.findPreferencesForUser(userId));

        return GetNotificationPreferencesResponse.builder()
                .userId(userId)
                .preferences(rows.stream().map(NotificationPreferenceServiceImpl::toResponse).toList())
                .build();
    }

    @Override
    @Transactional
    public UpdateNotificationPreferencesResponse updatePreferences(UpdateNotificationPreferencesRequest request) {
        String userId = requireUserId(request.userId());

        List<NotificationPreferenceUpdate> updates = request.preferences();
        if (updates == null || updates.isEmpty()) {
            throw new NotificationException(SaayamStatusCode.BAD_REQUEST.toString(), userId);
        }

        int created = 0;
        int updated = 0;

        for (NotificationPreferenceUpdate update : updates) {
            Integer channelId = update.channelId();
            if (channelId == null) {
                throw new NotificationException(SaayamStatusCode.BAD_REQUEST.toString(), userId);
            }

            // Reject unknown channels up front. The column is a foreign key, so an
            // unknown id would otherwise surface as an opaque constraint violation.
            int known = handleException(userId, () -> preferenceRepository.channelExists(channelId));
            if (known == 0) {
                throw new NotificationException(SaayamStatusCode.INVALID_PARAMETER.toString(), userId);
            }

            String preference = normalisePreference(update.preference(), userId);

            // No unique constraint on (user_id, channel_id) in the current schema, so
            // this is an update-then-insert rather than an ON CONFLICT upsert. Same
            // approach as the last-seen watermark.
            int rows = handleException(userId,
                    () -> preferenceRepository.updatePreference(userId, channelId, preference));

            if (rows > 0) {
                updated += rows;
            } else {
                handleException(userId,
                        () -> preferenceRepository.insertPreference(userId, channelId, preference));
                created++;
            }
        }

        List<NotificationPreferenceRow> rows = handleException(userId,
                () -> preferenceRepository.findPreferencesForUser(userId));

        return UpdateNotificationPreferencesResponse.builder()
                .userId(userId)
                .created(created)
                .updated(updated)
                .preferences(rows.stream().map(NotificationPreferenceServiceImpl::toResponse).toList())
                .build();
    }

    /**
     * A null or blank preference clears the channel's setting; anything else must be
     * one of the {@code preference_type} values.
     */
    private String normalisePreference(String preference, String userId) {
        if (preference == null || preference.isBlank()) {
            return null;
        }
        return NotificationPreferenceType.fromValue(preference)
                .map(NotificationPreferenceType::getValue)
                .orElseThrow(() -> new NotificationException(
                        SaayamStatusCode.INVALID_PARAMETER.toString(), userId));
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new NotificationException(SaayamStatusCode.USER_NOT_FOUND.toString(), userId);
        }
        return userId;
    }

    private static NotificationPreferenceResponse toResponse(NotificationPreferenceRow row) {
        return NotificationPreferenceResponse.builder()
                .channelId(row.getChannelId())
                .channelName(row.getChannelName())
                .description(row.getDescription())
                .preference(row.getPreference())
                .build();
    }

    private <T> T handleException(String userId, Supplier<T> action) {
        try {
            return action.get();
        } catch (NotificationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new NotificationException(SaayamStatusCode.DATABASE_ERROR.toString(), userId);
        } catch (RuntimeException ex) {
            throw new NotificationException(SaayamStatusCode.UNEXPECTED_ERROR.toString(), userId);
        }
    }
}
