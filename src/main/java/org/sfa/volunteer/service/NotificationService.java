package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;

public interface NotificationService {

    GetNotificationsResponse getNotificationCounts(GetNotificationsRequest request);

    GetNotificationsResponse getNotifications(GetNotificationsRequest request);

    UpsertLastSeenResponse upsertLastSeen(UpsertLastSeenRequest request);

}
