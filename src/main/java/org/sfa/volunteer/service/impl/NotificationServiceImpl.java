package org.sfa.volunteer.service.impl;

import java.sql.Timestamp;
import org.sfa.volunteer.dto.response.NotificationActionResponse;
import org.sfa.volunteer.entities.Notifications;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.NotificationAcceptException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.OffsetPageRequest;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.repository.UserRepository;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

        private final UserService userService;
        private final NotificationsRepository notificationsRepo;
        private final UserNotificationStatusRepository userNtfStatusRepo;

        public NotificationServiceImpl(
                        UserService userService,
                        NotificationsRepository notificationsRepo,
                        UserNotificationStatusRepository userNtfStatusRepo) {

                this.userService = userService;
                this.notificationsRepo = notificationsRepo;
                this.userNtfStatusRepo = userNtfStatusRepo;
        }

        @Override
        public GetNotificationsResponse getNotificationCounts(GetNotificationsRequest request) {

                String userId = request.userId();
                validateUser(userId);     
                
                return handleException(userId, () -> {

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
        });
        }

        @Override
        public GetNotificationsResponse getNotifications(GetNotificationsRequest request) {

                String userId = request.userId();
                int rowStart = request.rowStart();
                int rowEnd = request.rowEnd();
                validateUser(userId);   
                // 1. Get watermark timestamp (last seen).
                // A user with no watermark row has never opened the notifications page, so
                // every notification is new to them. Defaulting to "now" would mark them all
                // "old" here while getNotificationCounts() counts them all as new -- the two
                // endpoints would contradict each other for every first-time user.

                return handleException(userId, () -> {

                Timestamp lastSeen = handleException(userId,
                                () -> userNtfStatusRepo.getLastSeenTimestamp(userId));
              
                final Timestamp watermarkTs = (lastSeen != null) ? lastSeen
                                : Timestamp.valueOf(LocalDateTime.of(1970, 1, 1, 0, 0));
                // rowStart is an absolute row offset, not a page index, so it needs an
                // offset-addressed Pageable; PageRequest.of(rowStart, limit) would skip
                // rowStart * limit rows.
                int limit = rowEnd - rowStart + 1;
                Pageable pageable = new OffsetPageRequest(rowStart, limit, Sort.unsorted());

                // 2. Fetch paginated notifications sorted by createDttm DESC
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
                                                        .requestId(row.length > 5 ? (String) row[5] : null)
                                                        .actionStatus(row.length > 6 ? (ActionStatus) row[6] : null)
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

                        });
        }

        @Transactional
        @Override
        public UpsertLastSeenResponse upsertLastSeen(UpsertLastSeenRequest request) {

                String userId = request.userId();
                validateUser(userId);   

                return handleException(userId, () -> {

                boolean rowCreated = false;
                boolean rowUpdated = false;

                // Always returns UTC/GMT
                Instant timeGMT = Instant.now();
                // format it to Timestamp and saving it as UTC format in database table
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

        });
        }

    @Override
    @Transactional
    public NotificationActionResponse acceptNotification(Integer notificationId, String userId) {
        if (notificationId == null || notificationId <= 0 || userId == null || userId.isBlank()) {
            throw new NotificationAcceptException(SaayamStatusCode.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST, userId, notificationId);
        }

        Notifications notification = notificationsRepo
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_NOT_FOUND,
                        HttpStatus.NOT_FOUND, userId, notificationId));
        String requestId = notification.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            throw new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED,
                    HttpStatus.CONFLICT, userId, notification.getActionStatus());
        }

        // Every Accept for this request locks the same rows in the same order.
        // Deny's conditional update also takes a row lock, so the two flows serialize.
        var states = notificationsRepo.lockByRequestId(requestId);
        var current = states.stream()
                .filter(n -> notificationId.equals(n.getNotificationId()) && userId.equals(n.getUserId()))
                .findFirst()
                .orElseThrow(() -> new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_NOT_FOUND,
                        HttpStatus.NOT_FOUND, userId, notificationId));
        String status = current.getActionStatus();
        if (status == null) {
            throw new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED,
                    HttpStatus.CONFLICT, userId, null);
        }

        boolean alreadyAccepted = states.stream()
                .anyMatch(n -> ActionStatus.ACCEPTED.name().equals(n.getActionStatus()));
        if (alreadyAccepted || ActionStatus.REQUEST_TAKEN.name().equals(status)) {
            // A retry by the winner is safe. Other callers receive REQUEST_TAKEN.
            return NotificationActionResponse.builder()
                    .notificationId(notificationId).userId(userId).requestId(requestId)
                    .actionStatus(ActionStatus.ACCEPTED.name().equals(status)
                            ? ActionStatus.ACCEPTED : ActionStatus.REQUEST_TAKEN)
                    .build();
        }
        if (!ActionStatus.PENDING.name().equals(status)) {
            throw new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED,
                    HttpStatus.CONFLICT, userId, status);
        }

        int updated = notificationsRepo.updateActionStatusConditionally(
                notificationId, userId, ActionStatus.PENDING, ActionStatus.ACCEPTED);
        if (updated != 1) {
            throw new NotificationAcceptException(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED,
                    HttpStatus.CONFLICT, userId, status);
        }
        notificationsRepo.markOtherPendingNotificationsTaken(
                requestId, notificationId, ActionStatus.PENDING, ActionStatus.REQUEST_TAKEN);

        return NotificationActionResponse.builder()
                .notificationId(notificationId).userId(userId).requestId(requestId)
                .actionStatus(ActionStatus.ACCEPTED).build();
    }

    @Override
    @Transactional
    public NotificationActionResponse denyNotification(Integer notificationId, String userId) {
        if (notificationId == null || userId == null || userId.isBlank()) {
            throw new NotificationException(SaayamStatusCode.BAD_REQUEST.toString(), userId);
        }

        Notifications notification = notificationsRepo
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(
                        SaayamStatusCode.NOTIFICATION_NOT_FOUND.toString(), userId));

        ActionStatus current = notification.getActionStatus();

        // NULL means this notification type does not support Accept/Deny
        if (current == null) {
            throw new NotificationException(
                    SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString(), userId);
        }

        if (current != ActionStatus.PENDING) {
            throw new NotificationException(
                    SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString(), userId);
        }

        int updated = notificationsRepo.updateActionStatusConditionally(
                notificationId, userId, ActionStatus.PENDING, ActionStatus.DENIED);

        if (updated == 0) {
            // Another request updated it between our read and this write
            throw new NotificationException(
                    SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.toString(), userId);
        }

        return NotificationActionResponse.builder()
                .notificationId(notificationId)
                .userId(userId)
                .actionStatus(ActionStatus.DENIED)
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


        private void validateUser(String userId) {
              if (userId == null || userId.isBlank()) {
                 throw new NotificationException(
                                        SaayamStatusCode.BAD_REQUEST.toString(),
                                        userId);
                        
                        }

              if (!userService.userExists(userId)) {
                        throw new NotificationException(
                                        SaayamStatusCode.USER_NOT_FOUND.toString(),
                                        userId);
                        }
        }

}       
