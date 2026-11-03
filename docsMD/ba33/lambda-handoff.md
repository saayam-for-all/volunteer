# BA #33 Lambda handoff

Configure REST API Gateway proxy integration at `/v1/volunteer/onboarding/availability` with handler `org.sfa.volunteer.handler.OnboardingAvailabilityHandler`.

| Method | Contract | Authorization |
|---|---|---|
| `POST` | Read with `{ "userId": "SID-EXAMPLE" }` | Cognito authorizer |
| `PUT` | Save with the Step 4 request | Cognito authorizer |
| `OPTIONS` | Return 204 with CORS headers | Preflight |

The existing `UpdateVolunteerHandler` also accepts the Step 4 save contract on `PUT`. Keep Cognito authorization on that route so the handler receives the same verified caller context. The frontend sends local `HH:mm` slot values and an explicit `isEmergencyAvailable` boolean. [Request and response examples](design.md#public-contract)

The handlers consume `requestContext.authorizer.claims` from REST proxy events. The authorizer must provide `sub`, `email`, and a true `email_verified` claim. Restrict Lambda invocation to the authorized API Gateway execution ARN and verify the deployed route with a real account before enabling the frontend integration. HTTP API v2 events and standalone authentication remain deferred.
