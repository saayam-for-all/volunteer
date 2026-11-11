# Personal info: issue #43

## Context
[#43](https://github.com/saayam-for-all/volunteer/issues/43): the Profile → Personal Info tab needs a backend to read and update a user's personal details (date of birth, gender, address, secondary contact).

This adds `getPersonalInfo` and `updatePersonalInfo`. The read endpoint is a **POST** on purpose: #43 asks for a GET, but the team lead confirmed that `userId` goes in the request body rather than the URL.

## Branch
`KeerthiReddy_PersonalInfo_APIs`. **PR:** [#227](https://github.com/saayam-for-all/volunteer/pull/227), target `test`.

## API
Both endpoints take their parameters in the JSON body only. `userId` is never in the path or the query string.

Headers: `Content-Type: application/json`, `Authorization: <Cognito idToken>`.

### getPersonalInfo: `POST /personal-info`
```json
// request
{ "userId": "SID-00-000-000-080" }
```
```json
// response
{
  "success": true,
  "statusCode": 200,
  "saayamCode": "SAAAYAM-1200",
  "message": "Operation successful",
  "data": {
    "dateOfBirth": "03/15/1990",
    "gender": "Female",
    "addressLine1": "123 Main St",
    "addressLine2": "Apt 4",
    "city": "Tempe",
    "state": "Arizona",
    "country": "UNITED_STATES_OF_AMERICA",
    "zipCode": "85281",
    "secondaryEmail": "me@example.com",
    "secondaryPhone": "+16025550100"
  },
  "timestamp": 1794230999.068016691
}
```
- `dateOfBirth` is formatted `MM/dd/yyyy`.
- Fields that are not saved come back as `null`. A user with no secondary details gets 200 with `secondaryEmail` and `secondaryPhone` as `null`.
- `state` and `country` are returned as stored, and they follow different conventions: states are title case (`Arizona`), countries are uppercase with underscores (`UNITED_STATES_OF_AMERICA`).

### updatePersonalInfo: `PUT /personal-info`
```json
// request
{
  "userId": "SID-00-000-000-080",
  "dateOfBirth": "03/15/1990",
  "gender": "Female",
  "addressLine1": "123 Main St",
  "addressLine2": "Apt 4",
  "city": "Tempe",
  "state": "Arizona",
  "country": "UNITED_STATES_OF_AMERICA",
  "zipCode": "85281",
  "secondaryEmail": "me@example.com",
  "secondaryPhone": "+16025550100"
}
```
```json
// response
{
  "success": true,
  "statusCode": 200,
  "saayamCode": "SAAAYAM-1204",
  "message": "User Personal Info Profile updated",
  "data": { "…same shape as getPersonalInfo…" },
  "timestamp": 1794231135.975759365
}
```
- Only `userId` is required. **Omitted or `null` fields are left unchanged**, so send just the fields that changed.
- There is no way to clear a field once it is set.
- `state` and `country` are looked up by name, so they must match what is stored.
- Unknown JSON fields are ignored.

### Validation (400, with a message naming the problem)
| Field | Rule |
|---|---|
| body | required, valid JSON |
| `userId` | required, not blank |
| `dateOfBirth` | a real date in strict `MM/dd/yyyy`. `02/30/1990` and `02/29/1999` are rejected; `02/29/2000` is accepted |

### Other errors
- 404: unknown user, state or country.
- 500: anything else (internal details are hidden).
- CORS headers are on every response, and `OPTIONS` returns 204.

## Storage
- `users`: `dob`, `gender`, `addr_ln1`, `addr_ln2`, `city_name`, `zip_code`, `state_id`, `country_id`; `last_updated_at` changes on every update.
- `user_additional_details`: `secondary_email_1` and `secondary_phone_1`. The row is created on the first update that needs it.
- `countries` and `states` are the renamed tables (formerly `country` and `state`), and both use `last_updated_at`.
- **`@DynamicUpdate` on `User`** is a workaround, not a fix. `users.language_1/2/3` are `bigint` in the database but `String` in `User.java`, and a full-entity save writes them. With `@DynamicUpdate` Hibernate writes only the changed columns. Tracked as a separate issue.

## Changes (`src/main/java/org/sfa/volunteer/…`)
| File | What it does |
|---|---|
| `handler/GetPersonalInfoHandler`, `UpdatePersonalInfoHandler` | Lambda handlers for API Gateway (proxy): CORS, preflight, base64 bodies, error → status. Package-private constructor for tests; Spring starts lazily once per container |
| `dto/request/PersonalInfoRequest`, `UpdatePersonalInfoRequest`, `dto/response/PersonalInfoResponse` | Request and response shapes |
| `service/UserService`, `impl/UserServiceImpl` | `getPersonalInfoById`, `updatePersonalInfo`, and the strict `DOB_FORMAT` (`MM/dd/uuuu`, `ResolverStyle.STRICT`) |
| `model/Country`, `model/State` | Map the renamed `countries` / `states` tables and their `last_updated_at` column |
| `model/User` | `@DynamicUpdate` (see Storage) |
| `repository/StateRepository` | `findByStateName` |
| `controller/UserController` | The same endpoints for Spring: `POST` and `PUT /0.0.1/users/personal-info` |
| `application.yaml` | Hikari pool size 1; `com.amazonaws` and `com.amazonaws.serverless.proxy.spring` at `ERROR` (#222) |
| Tests | `GetPersonalInfoHandlerTest`, `UpdatePersonalInfoHandlerTest`, `UserServiceImplPersonalInfoTest` |

## Deployment checklist
- **API Gateway** `volunteers-api`, stage `dev`, Cognito authorizer, Lambda proxy integration:
  - POST `/personal-info` → `GetPersonalInfoHandler:dev`
  - PUT `/personal-info` → `UpdatePersonalInfoHandler:dev`
  - OPTIONS `/personal-info` is a Mock integration with no authorizer; its allowed methods must include `POST,PUT,OPTIONS` and its allowed headers `Content-Type,Authorization`.
  - The routes point at the `dev` **aliases**, which must point at a published version (SnapStart is on PublishedVersions), not `$LATEST`.
- **Lambda:**
  - Java 17, shaded jar from `mvn package` (about 60 MB, so upload via S3, not directly).
  - Handlers: `org.sfa.volunteer.handler.GetPersonalInfoHandler::handleRequest` and `org.sfa.volunteer.handler.UpdatePersonalInfoHandler::handleRequest`.
  - Env vars: `SPRING_DATASOURCE_URL` (`…?currentSchema=<region>_dev_saayam_rdbms`), `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
  - Recommended: `JAVA_TOOL_OPTIONS=-Dspring.devtools.restart.enabled=false`.
- **Database:** `users` needs `dob`, `gender`, `addr_ln1/2`, `city_name`, `zip_code`, `state_id`, `country_id`, `last_updated_at`; the tables `countries`, `states` and `user_additional_details` must exist with the columns the entities map. Check with `\d users` and the `information_schema` query in the runbook.

## Verification
- `mvn test`: 359 tests, 0 failures. They use mocks and H2, so they could not catch the table and column renames; the live checks below did.
- Live on dev, through API Gateway (published versions `GetPersonalInfoHandler:5`, `UpdatePersonalInfoHandler:3`):
  - POST → 200 with the user's data; the country lookup resolved.
  - PUT → 200; `city_name` was written and `last_updated_at` changed in the database.
  - Latency about 300 ms after the Hikari change.

## Follow-ups
- **Authorization:** any signed-in user can read or update any other user's personal info. The handlers should check the Cognito claims (`requestContext.authorizer.claims`), as the profile-image handlers do.
- **Type mismatch:** `users.language_1/2/3` (bigint vs String) and `last_location`. `@DynamicUpdate` does not help inserts or `updateUserPreferences`.
- **`GET /personal-info/{userId}`:** an old API Gateway route to `GetPersonalInfoHandler:dev`. The handler reads `userId` from the body only, so it always returns 400. Delete it or decide otherwise; only POST and PUT `/personal-info` are supported.
- **Update semantics:** no way to clear a field. `state` is looked up by name only, so a state name that exists in two countries would be ambiguous.
- **Status code:** the Spring `PUT` returns SAAAYAM-1202 while the Lambda returns SAAAYAM-1204.
- **#222:** redeploy the other affected Lambdas with the new jar, then rotate the credentials already written to CloudWatch and clear the old log streams.
- **DDL:** the database repo is stale (old column names, no `dob`, singular table names).

## References
- DB schema: [Changes to the Database, Waiting for Microservice](https://github.com/saayam-for-all/database/wiki/*-Changes-to-the-Database,-Waiting-for-Microservice).
