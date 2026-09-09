package org.sfa.volunteer.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.sfa.volunteer.repository.OffsetPageRequest;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        // 1. Get watermark timestamp (last seen).
        // A user with no watermark row has never opened the notifications page, so
        // every notification is new to them. Defaulting to "now" would mark them all
        // "old" here while getNotificationCounts() counts them all as new -- the two
        // endpoints would contradict each other for every first-time user.
        Timestamp lastSeen = handleException(userId,
                () -> userNtfStatusRepo.getLastSeenTimestamp(userId));
        final Timestamp watermarkTs = (lastSeen != null) ? lastSeen : Timestamp.valueOf(LocalDateTime.of(1970, 1, 1, 0, 0));

        // 2. Fetch notifications sorted by createDttm DESC.
        // rowStart is an absolute row offset, not a page index, so it needs an
        // offset-addressed Pageable; PageRequest.of(rowStart, limit) would skip
        // rowStart * limit rows.
        int limit = rowEnd - rowStart + 1;
        Pageable pageable = new OffsetPageRequest(rowStart, limit, Sort.unsorted());
        List<NotificationResponse> notifications = handleException(userId,
                () -> notificationsRepo.findNotifications(userId, pageable));

        // 3. Override status field with "new" or "old"
        List<NotificationResponse> items = notifications.stream()
                .map(n -> {
                    boolean isNew = n.createDttm().after(watermarkTs);

                    return NotificationResponse.builder()
                            .notificationId(n.notificationId())
                            .status(isNew ? "new" : "old") // <-- HERE
                            .typeName(n.typeName())
                            .message(n.message())
                            .createDttm(n.createDttm())
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

        if (userId == null || userId.isBlank()) {
            throw new UserNotFoundException(userId);
        }

        // The watermark column is TIMESTAMP WITHOUT TIME ZONE and the spec requires GMT.
        // Timestamp.from(Instant.now()) renders in the JVM default zone, and PgJDBC
        // writes that local rendering verbatim -- so on a non-UTC JVM it stores local
        // time, not GMT. Building from LocalDateTime.now(UTC) makes the stored
        // wall-clock genuinely UTC, matching how notifications.created_at is written.
        Timestamp watermarkTs = Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC));

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