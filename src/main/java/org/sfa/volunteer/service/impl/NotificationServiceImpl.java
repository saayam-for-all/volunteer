package org.sfa.volunteer.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.enums.StatusType;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

        private final NotificationsRepository notificationsRepo;
        private final UserNotificationStatusRepository userNtfStatusRepo;

        public NotificationServiceImpl(
                        NotificationsRepository notificationsRepo,
                        UserNotificationStatusRepository userNtfStatusRepo) {

                this.notificationsRepo = notificationsRepo;
                this.userNtfStatusRepo = userNtfStatusRepo;
        }

        @Override
        public GetNotificationsResponse getNotificationCounts(GetNotificationsRequest request) {

                String userId = request.userId();
                int totalNewNotificationsCount = 0;

                // 1. Count total notifications
                int totalNotificationsCount = handleException(userId,
                                () -> notificationsRepo.countAllNotifications(userId));

                // 2. Query watermark table for userId
                Timestamp watermarkTs = handleException(userId,
                                () -> userNtfStatusRepo.getLastSeenTimestamp(userId));

                // If watermark is null → user has never seen/read notifications
                if (watermarkTs == null) {
                        // default to totalNtfCount because user has never seen any notifications.
                        totalNewNotificationsCount = totalNotificationsCount;
                } else {
                        // 3. Count new notifications (ntf.createDttm > watermarkTimestamp)
                        totalNewNotificationsCount = handleException(userId,
                                        () -> notificationsRepo.countNewNotifications(userId, watermarkTs));
                }

                // return only the counts with blank/null notifications data
                return GetNotificationsResponse.builder()
                                .totalCount(totalNotificationsCount)
                                .newNotificationsCount(totalNewNotificationsCount)
                                .build();

        }

        @Override
        public GetNotificationsResponse getNotifications(GetNotificationsRequest request) {

                String userId = request.userId();
                int rowStart = request.rowStart();
                int rowEnd = request.rowEnd();

                // // 1. Get watermark timestamp (last seen)
                Timestamp lastSeen = handleException(userId,
                                () -> userNtfStatusRepo.getLastSeenTimestamp(userId));
                final Timestamp watermarkTs = (lastSeen != null) ? lastSeen : Timestamp.from(Instant.now());

                // 2. Fetch paginated notifications sorted by createDttm DESC
                int limit = rowEnd - rowStart + 1; // number of records per page
                int offset = rowStart;
                Pageable pageable = PageRequest.of(offset / limit, limit);
                // List<NotificationResponse> notifications = handleException(userId,
                // () -> notificationsRepo.findNotifications(userId, pageable));

                Page<Object[]> notifications = handleException(userId,
                                () -> notificationsRepo.findNotifications(userId, pageable));

                // 4. Convert DB rows → DTO + compute "new"/"old"
                List<NotificationResponse> items = notifications.getContent().stream()
                                .map(row -> {

                                        Integer id = ((Number) row[0]).intValue();
                                        String status = (String) row[1];
                                        String typeName = (String) row[2];
                                        String message = (String) row[3];
                                        Timestamp createDttm = (Timestamp) row[4];

                                        boolean isNew = createDttm.after(watermarkTs);

                                        return NotificationResponse.builder()
                                                        .notificationId(id)
                                                        .status(isNew ? "new" : "old")
                                                        .typeName(typeName)
                                                        .message(message)
                                                        .createDttm(createDttm)
                                                        .build();
                                })
                                .toList();

                // 3. Count totals
                int totalCount = handleException(userId,
                                () -> notificationsRepo.countAllNotifications(userId));
                int newNotificationsCount = handleException(userId,
                                () -> notificationsRepo.countNewNotifications(userId, watermarkTs));

                // 5. Return final response
                return GetNotificationsResponse.builder()
                                .notifications(items)
                                .totalCount(totalCount)
                                .newNotificationsCount(newNotificationsCount)
                                .build();
        }

        @Transactional
        @Override
        public UpsertLastSeenResponse upsertLastSeen(UpsertLastSeenRequest request) {

                boolean rowCreated = false;
                boolean rowUpdated = false;
                String userId = request.userId();

                // Always returns UTC/GMT
                Instant timeGMT = Instant.now();
                // format it to Timestamp
                Timestamp watermarkTs = Timestamp.from(timeGMT);

                // 3. Check if user exists (wrapped in handleException)
                int exists = handleException(userId,
                                () -> userNtfStatusRepo.existsByUserId(userId));

                // 4. Insert or update (wrapped in handleException)
                if (exists == 0) {
                        rowCreated = handleException(userId,
                                        () -> userNtfStatusRepo.createLastSeenTimestamp(userId, watermarkTs) > 0);
                } else {
                        rowUpdated = handleException(userId,
                                        () -> userNtfStatusRepo.updateLastSeenTimestamp(userId, watermarkTs) > 0);
                }
                // TODO
                return UpsertLastSeenResponse.builder()
                                .created(rowCreated)
                                .updated(rowUpdated)
                                .message(rowCreated ? "Created" : rowUpdated ? "Updated" : "failed")
                                .build();
        }

        private <T> T handleException(String userId, Supplier<T> action) {
                try {
                        return action.get();
                } catch (DataAccessException ex) {
                        throw new NotificationException(
                                        SaayamStatusCode.DATABASE_ERROR.toString(),
                                        userId);
                } catch (RuntimeException ex) {
                        throw new NotificationException(
                                        SaayamStatusCode.UNEXPECTED_ERROR.toString(),
                                        userId);
                }
        }

}