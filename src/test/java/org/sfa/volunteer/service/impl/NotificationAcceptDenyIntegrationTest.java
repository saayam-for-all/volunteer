package org.sfa.volunteer.service.impl;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.response.NotificationActionResponse;
import org.sfa.volunteer.enums.ActionStatus;
import org.sfa.volunteer.exception.NotificationAcceptException;
import org.sfa.volunteer.exception.NotificationException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@SpringJUnitConfig(NotificationAcceptDenyIntegrationTest.Config.class)
class NotificationAcceptDenyIntegrationTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:accept_deny;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("org.sfa.volunteer.entities");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        NotificationsRepository notificationsRepository(EntityManagerFactory factory) {
            return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
                    .getRepository(NotificationsRepository.class);
        }

        @Bean
        NotificationService notificationService(NotificationsRepository repository) {
            var users = mock(UserService.class);
            when(users.userExists(anyString())).thenReturn(true);
            return new NotificationServiceImpl(users, repository, mock(UserNotificationStatusRepository.class));
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) { return new JdbcTemplate(dataSource); }
    }

    @Autowired private NotificationService service;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM notifications");
        jdbc.update("DELETE FROM notification_types");
        jdbc.update("INSERT INTO notification_types (type_id, type_name) VALUES (1, 'volunteer_request')");
    }

    private void insert(int id, String user, String request, String actionStatus) {
        jdbc.update("""
                INSERT INTO notifications
                  (notification_id, user_id, req_id, action_status, status, channel_id, message, type_id, created_at)
                VALUES (?, ?, ?, ?, 'unread', 1, 'Help requested', 1, CURRENT_TIMESTAMP)
                """, id, user, request, actionStatus);
    }

    private String status(int id) {
        return jdbc.queryForObject("SELECT action_status FROM notifications WHERE notification_id = ?",
                String.class, id);
    }

    @Test
    void denyThenAnotherVolunteerAcceptsWithoutChangingReadStateOrUnrelatedRequests() {
        insert(1, "user-1", "req-1", "PENDING");
        insert(2, "user-2", "req-1", "PENDING");
        insert(3, "user-3", "req-1", "PENDING");
        insert(4, "user-4", "req-2", "PENDING");
        insert(5, "user-5", "req-1", null);
        assertThat(service.denyNotification(1, "user-1").actionStatus()).isEqualTo(ActionStatus.DENIED);
        assertThat(service.acceptNotification(2, "user-2").actionStatus()).isEqualTo(ActionStatus.ACCEPTED);
        assertThat(service.acceptNotification(3, "user-3").actionStatus()).isEqualTo(ActionStatus.REQUEST_TAKEN);
        assertThat(status(1)).isEqualTo("DENIED");
        assertThat(status(2)).isEqualTo("ACCEPTED");
        assertThat(status(3)).isEqualTo("REQUEST_TAKEN");
        assertThat(status(4)).isEqualTo("PENDING");
        assertThat(status(5)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE status = 'unread'", Integer.class))
                .isEqualTo(5);
    }

    @Test
    void winnerCanRetryButDenyCannotUndoAcceptance() {
        insert(1, "user-1", "req-1", "PENDING");
        service.acceptNotification(1, "user-1");
        assertThat(service.acceptNotification(1, "user-1").actionStatus()).isEqualTo(ActionStatus.ACCEPTED);
        assertThatThrownBy(() -> service.denyNotification(1, "user-1")).isInstanceOf(NotificationException.class);
        assertThat(status(1)).isEqualTo("ACCEPTED");
    }

    @Test
    void ownershipAndNonActionableNotificationsAreRejected() {
        insert(1, "user-1", "req-1", "PENDING");
        insert(2, "user-1", "req-1", null);
        insert(3, "user-1", null, "PENDING");
        assertThatThrownBy(() -> service.acceptNotification(1, "other-user"))
                .isInstanceOf(NotificationAcceptException.class).hasMessage("NOTIFICATION_NOT_FOUND");
        assertThatThrownBy(() -> service.acceptNotification(2, "user-1"))
                .isInstanceOf(NotificationAcceptException.class).hasMessage("NOTIFICATION_ALREADY_ACTIONED");
        assertThatThrownBy(() -> service.acceptNotification(3, "user-1"))
                .isInstanceOf(NotificationAcceptException.class);
        assertThatThrownBy(() -> service.acceptNotification(1, " "))
                .isInstanceOf(NotificationAcceptException.class).hasMessage("BAD_REQUEST");
        assertThat(status(1)).isEqualTo("PENDING");
    }

    @Test
    void deniedNotificationCannotBeAccepted() {
        insert(1, "user-1", "req-1", "PENDING");
        service.denyNotification(1, "user-1");
        assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                .isInstanceOf(NotificationAcceptException.class);
        assertThat(status(1)).isEqualTo("DENIED");
    }

    @Test
    void failureUpdatingOtherNotificationsRollsBackAcceptance() {
        insert(1, "user-1", "req-1", "PENDING");
        insert(2, "user-2", "req-1", "PENDING");
        jdbc.execute("ALTER TABLE notifications ADD CONSTRAINT test_no_taken CHECK (action_status <> 'REQUEST_TAKEN')");
        try {
            assertThatThrownBy(() -> service.acceptNotification(1, "user-1"))
                    .isInstanceOf(RuntimeException.class);
            assertThat(status(1)).isEqualTo("PENDING");
            assertThat(status(2)).isEqualTo("PENDING");
        } finally {
            jdbc.execute("ALTER TABLE notifications DROP CONSTRAINT test_no_taken");
        }
    }

    @Test
    void listingExposesRequestReferenceAndCommittedActionStatuses() {
        insert(1, "user-1", "req-1", "PENDING");
        insert(2, "user-2", "req-1", "PENDING");
        service.acceptNotification(1, "user-1");
        var items = service.getNotifications(GetNotificationsRequest.builder()
                .userId("user-2").rowStart(0).rowEnd(9).build()).notifications();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).requestId()).isEqualTo("req-1");
        assertThat(items.get(0).actionStatus()).isEqualTo(ActionStatus.REQUEST_TAKEN);
    }

    @Test
    void simultaneousAcceptsOfDifferentNotificationsHaveOnlyOneWinner() throws Exception {
        insert(1, "user-1", "req-1", "PENDING");
        insert(2, "user-2", "req-1", "PENDING");
        var results = race(() -> service.acceptNotification(1, "user-1"),
                () -> service.acceptNotification(2, "user-2"));
        assertThat(results).containsExactlyInAnyOrder("ACCEPTED", "REQUEST_TAKEN");
        assertThat(List.of(status(1), status(2))).containsExactlyInAnyOrder("ACCEPTED", "REQUEST_TAKEN");
    }

    @Test
    void simultaneousAcceptAndDenyOfSameNotificationCommitExactlyOneAction() throws Exception {
        insert(1, "user-1", "req-1", "PENDING");
        var results = race(() -> service.acceptNotification(1, "user-1"),
                () -> service.denyNotification(1, "user-1"));
        assertThat(results).contains(SaayamStatusCode.NOTIFICATION_ALREADY_ACTIONED.name());
        assertThat(status(1)).isIn("ACCEPTED", "DENIED");
        assertThat(results).contains(status(1));
    }

    @Test
    void simultaneousAcceptAndAnotherUsersDenyPreserveWinner() throws Exception {
        insert(1, "user-1", "req-1", "PENDING");
        insert(2, "user-2", "req-1", "PENDING");
        var results = race(() -> service.acceptNotification(1, "user-1"),
                () -> service.denyNotification(2, "user-2"));
        assertThat(results).contains("ACCEPTED");
        assertThat(status(1)).isEqualTo("ACCEPTED");
        assertThat(status(2)).isIn("DENIED", "REQUEST_TAKEN");
    }

    private List<String> race(Supplier<NotificationActionResponse> first,
                              Supplier<NotificationActionResponse> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> runAction(first, ready, start));
            var b = executor.submit(() -> runAction(second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private String runAction(Supplier<NotificationActionResponse> action,
                             CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start timed out");
        try {
            return action.get().actionStatus().name();
        } catch (NotificationException exception) {
            return exception.getMessage();
        }
    }
}
