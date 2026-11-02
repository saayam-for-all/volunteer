# Accept built on Deny (#198)

This checkout is on `issue-198-accept-api`, based on Akshaj's
`upstream/realAkshaj_deny_notification_198` at `46bdc6a`, with the team's `test`
branch integrated at `4f3ea6d`. Previous workspace edits
are preserved in the stash named `Original workspace before issue-198-accept branch`.
The implementation here uses Akshaj's existing
notification entity, enum, repository, service, controller, and Deny logic.
The target for the PR is `saayam-for-all/volunteer:test`. Its preference APIs,
POST `/counts` and `/details` endpoints, user validation, and paginated repository
contract are retained. Accept and Deny retain their notification-ID URLs below.

## API

Both actions use the same notification ID and caller header, with no request body:

```http
POST /0.0.1/notifications/{notificationId}/accept
X-User-Id: volunteer-id

POST /0.0.1/notifications/{notificationId}/deny
X-User-Id: volunteer-id
```

`X-User-Id` follows the existing Deny contract. The authentication gateway must
provide/validate the caller identity; an arbitrary client-supplied header is not
authentication. Ownership is checked against that identity for both actions.

Accept succeeds with HTTP 200 and `data.actionStatus: ACCEPTED`. The response also
includes the notification ID, user ID, and `requestId`. The Java `requestId` field
maps to the confirmed database column `notifications.req_id`, not `request_id`.

A losing Accept returns HTTP 409, `saayamCode: SAAAYAM-1323`, and
`data.actionStatus: REQUEST_TAKEN`. Retrying the winner's Accept returns success.
An already denied or non-actionable notification returns HTTP 409; a missing or
unowned notification returns HTTP 404. Invalid Accept parameters return HTTP 400.

Deny keeps Akshaj's behavior: only `PENDING` can become `DENIED`. It cannot undo
acceptance or affect other volunteers. Its domain error handler now sends the
actual HTTP error status rather than HTTP 200 with an error status in the body.

## Transactions and simultaneous actions

Accept locks all existing notifications for `req_id` in notification-ID order,
then reads their locked scalar states. Scalar reads avoid using the stale entity
from the earlier ownership lookup. It conditionally accepts one notification and
marks all other pending notifications for that request `REQUEST_TAKEN`, in one
transaction. A failure updating the other notifications rolls back acceptance.

Deny's existing conditional update uses the same database row locks. For the same
notification, either Accept or Deny wins; the other action cannot overwrite it.
For different notifications, Deny can win its own row before another volunteer
accepts. Existing `DENIED` statuses, NULL action statuses, read/unread status, and
notifications for other requests are preserved.

Notification list responses now include `requestId` and `actionStatus`, alongside
the existing `typeName` and `message`, so the frontend can refresh the actions.
Clients should show buttons only for eligible notification types with `PENDING`
and hide/disable `REQUEST_TAKEN` notifications. This change does not add a push
transport or a new Subject field; the existing notification message remains intact.

## Local validation

Use Java 17 or newer. From this checkout in PowerShell, where Java 21 is installed:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn test
```

The tests exercise the actual JPA repository and transactions on H2 in PostgreSQL
compatibility mode, including two simultaneous Accepts, Accept/Deny races, and
rollback on a failed update. Akshaj's original eight Deny tests remain unchanged.
The inherited `VolunteerApplicationTests.contextLoads` has `@SpringBootTest`
commented out, so it does not validate production application startup.

## Issue #198 coverage

The issue-specific target is at least 90% coverage of the Accept implementation.
The clean JaCoCo report currently measures:

| Scope | Line coverage | Branch coverage |
| --- | --- | --- |
| `NotificationServiceImpl.acceptNotification` | 100% | 100% |
| `NotificationController.acceptNotification` | 100% | 100% |
| `NotificationAcceptException` | 100% | No branches |

Tests cover stale pending snapshots, notifications removed before locking,
failed conditional updates, invalid identities, missing request references,
concurrency, and rollback. The shared error handler has regression tests for its
notification HTTP status mappings and existing domain responses.

Run `mvn clean test jacoco:report` to rebuild the report at
`target/site/jacoco/index.html`. The combined run after merging `test` passed
323 tests with no failures or errors; Accept methods still have 100% line and
branch coverage.
Issue-specific coverage is distinct from overall service coverage. Build and CI
coverage policy now follow the latest `test` branch unchanged.

## Integration boundaries

Lead Volunteer assignment remains in the request microservice, as agreed in the
issue discussion. No RequestServiceClient call or assignment is added here.
The request-service team must confirm how it consumes committed acceptance.

Before deployment, confirm the physical `req_id` and nullable VARCHAR
`action_status` columns, then verify concurrent requests against PostgreSQL.
Notification producers must stop creating new pending notifications for a claimed
request. Row locks serialize the existing notification set; they do not coordinate
independent producers, deleted winner records, or request-service writes.

Open or update the PR against the team's `test` branch. It currently includes
Akshaj's Deny dependency because that implementation is not yet present in `test`.
The conflict-resolution changes require a normal push after local validation.
