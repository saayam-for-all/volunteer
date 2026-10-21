package org.sfa.volunteer.service.impl;

import java.sql.Timestamp;
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
import org.springframework.http.HttpStatus;
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
                // // 1. Get watermark timestamp (last seen)

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