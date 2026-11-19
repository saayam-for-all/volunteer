# Issue #198: Lambda and frontend integration

## 1. Review the adapters

`AcceptNotificationHandler` and `DenyNotificationHandler` adapt API Gateway REST
API proxy events (payload version 1.0) to the existing notification service. The
shared `NotificationActionHandlerBase` parses input, authorizes ownership, handles
preflight, and returns the existing JSON response envelope and actual HTTP status.
The service's transaction and request-level locks remain the source of action
behavior. These adapters do not yet call the request service to assign a Lead.

Identity follows the existing availability handler: API Gateway must verify the
token and supply `requestContext.authorizer.claims.email`. `X-User-Id` selects the
internal Saayam user; the handler checks the verified email owns that user before
calling the notification service. A header or JSON body cannot supply the trusted
email. Missing claims return 401; a different user's ID returns 403.

## 2. Run tests and build

In Git Bash, from the repository root:

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -Dtest=NotificationActionHandlersTest test
mvn clean package jacoco:report
```

The package command runs all tests and creates the shaded deployment JAR at
`target/volunteer-0.0.1-SNAPSHOT.jar`. The existing pipeline may rename that JAR
when uploading it to the team's artifact bucket. Coverage is under
`target/site/jacoco/index.html`.

Local validation: `mvn package jacoco:report` passed 349 tests (including 26 new
handler tests) and built the shaded JAR. The shared Lambda `handleRequest` method
has 100% line and 96.97% branch coverage. This does not validate AWS cold-start
configuration, Gateway authorizer settings, or connectivity to the deployed DB.

## 3. Coordinate deployment with the team

This repository contains an artifact upload workflow, but no infrastructure
template provisioning these two functions and Gateway routes. Confirm the AWS
account/region, function names, execution role, database connectivity, and existing
API Gateway/authorizer with the deployment owner. Reuse the team's existing
notification function environment and database configuration.

Create or update two Java 17 (or approved newer Java) Lambda functions using the
same deployment JAR:

| Action | Lambda handler setting | Gateway route |
| --- | --- | --- |
| Accept | `org.sfa.volunteer.handler.AcceptNotificationHandler::handleRequest` | `POST /0.0.1/notifications/{notificationId}/accept` |
| Deny | `org.sfa.volunteer.handler.DenyNotificationHandler::handleRequest` | `POST /0.0.1/notifications/{notificationId}/deny` |

Use REST API Lambda proxy integration. Apply the existing verified identity
authorizer to POST, pass `notificationId` as a path parameter, and permit Gateway
to invoke the functions. Configure OPTIONS without authentication and CORS to
allow POST and the `Authorization`, `Content-Type`, and `X-User-Id` headers. The
adapters use the repository's existing `Cors` helper. Confirm the deployed stage
prefix and custom-domain mapping before sharing the exact URLs with frontend.

Restrict direct Lambda invocation to the intended deployment/infrastructure roles.
Do not expose an alternate unauthenticated route to these actions. Do not treat
JWT decoding in the application as verification of its signature.

For an AWS-console test invocation, use a real development notification ID, its
owner's internal user ID, and that user's email in this payload. These fields
simulate the Gateway event; real browsers obtain authorizer context from Gateway:

```json
{
  "httpMethod": "POST",
  "pathParameters": {"notificationId": "19801"},
  "headers": {"X-User-Id": "REAL_INTERNAL_USER_ID"},
  "requestContext": {
    "authorizer": {"claims": {"email": "real-user@example.test"}}
  },
  "body": null,
  "isBase64Encoded": false
}
```

The earlier local fixture's fake users are not real accounts and cannot pass this
identity check. Use approved development records for deployed tests.

## 4. Connect the frontend

The frontend source is not in this repository. Share the two deployed routes and
the current authenticated user's internal ID/token with its developer. A minimal
framework-independent client is provided in
`docs/examples/notification-actions.js`.

Show Accept/Deny only when both the notification type is eligible and
`actionStatus === "PENDING"`; use `notificationId` for the URL. Display the
subject/message and request number according to the existing notification UI.
Disable both buttons while either action is in flight. On success or conflict,
refresh notification details and counts using the team's existing read APIs.
In a finally block, always clear the pending state even if refresh fails.

| Response | Frontend behavior |
| --- | --- |
| 200, ACCEPTED or DENIED | Show the response message and refresh |
| 409, REQUEST_TAKEN / SAAAYAM-1323 | Explain it was claimed and refresh; do not automatically reassign |
| Other 409 | Explain it was already handled and refresh |
| 400 | Report invalid input |
| 401 | Request sign-in/token refresh |
| 403 | Report that this action is not permitted |
| 404 | Refresh because this notification is unavailable to the caller |
| Network error or 5xx | Show a retryable error and refresh before deciding the next action |

## 5. Verify before calling this complete

Test through the deployed Gateway with an authenticated user: Accept, Deny,
already-claimed requests, mismatched users, missing identity, and browser CORS.
Confirm frontend buttons refresh for the other notified user. Concurrent
PostgreSQL API testing is still pending; the existing automated races use H2.

The request-service handoff still needs its full service address and a trusted
source for the original requester's ID. Assignment retries, timeouts, and
conflicting Lead assignments must be agreed before the complete business flow is
ready. A notification response of ACCEPTED alone does not prove Lead assignment.

These additions do not provision AWS resources or modify the team's coverage gate.
