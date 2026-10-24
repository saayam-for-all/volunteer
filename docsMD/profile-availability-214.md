# Profile availability: issue #214

## Context
[#214](https://github.com/saayam-for-all/volunteer/issues/214): the webapp's Profile → Availability tab saves only to browser localStorage (`// TODO: Replace with actual API call`), because there was no backend for it.

This adds `getAvailability` and `updateAvailability`, which store a volunteer's weekly slots in `user_availability` and their timezone in `users.time_zone`. BA epic: [ba#51](https://github.com/saayam-for-all/ba/issues/51).

## Branch
`vishaakr98_feature/profile-availability_214`, cut from `test`. **PR target: `test`.**

## API
Both endpoints take their parameters in the JSON body only. `userId` is never in the path or the query string.

Headers: `Content-Type: application/json`, `Authorization: <Cognito idToken>`.

### getAvailability: `POST /availability`
```json
// request
{ "userId": "SID-00-000-002-10000" }
// response (envelope: success, statusCode, saayamCode, message, data, timestamp)
{ "data": {
    "userId": "SID-00-000-002-10000",
    "timezone": "America/Chicago",
    "slots": [
      { "id": 1, "frequency": "Weekdays", "startTime": "09:00", "endTime": "17:00" },
      { "id": 6, "frequency": "Saturday", "startTime": "22:00", "endTime": "02:00" }
    ],
    "lastUpdatedAt": "2026-10-24T01:37:28Z" } }
```
- Times are in the user's own timezone.
- If nothing is saved, the response is 200 with `slots: []` and `lastUpdatedAt: null`.

### updateAvailability: `PUT /availability`
```json
{
  "userId": "SID-00-000-002-10000",
  "timezone": "America/Chicago",
  "slots": [{ "frequency": "Weekdays", "startTime": "09:00", "endTime": "17:00" }],
  "vacationMode": false,
  "vacationStartDate": "",
  "vacationEndDate": ""
}
```
- It replaces all of the user's slots, updates `users.time_zone`, and returns the same shape as getAvailability.
- `slots: []` clears the user's availability.
- The vacation fields are accepted so the webapp can send its full payload. They are ignored: not stored, validated or returned, because there are no columns for them yet.
- The slot `id` the webapp generates (`Date.now()`) is ignored too.

### Validation (400, with a message naming the field)
| Field | Rule |
|---|---|
| `userId` | required |
| `timezone` | must be an IANA zone from `ZoneId.getAvailableZoneIds()`, e.g. `America/Chicago`. `CST`, `+05:30`, `UTC+5`, `GMT-3` and `Z` are rejected. `ZoneId.of()` would accept the fixed offsets, but they never follow daylight saving. |
| `slots` | required (may be empty), at most 20 entries |
| `slots[i].frequency` | `Everyday`, `Weekdays`, `Weekends`, or `Monday` to `Sunday` |
| `slots[i].startTime` / `endTime` | strict `HH:mm`, 24-hour (`9:00` and `24:00` are rejected); start ≠ end |

- An end before the start is an overnight slot.
- A day can have several slots, e.g. Monday 10:00–14:00 and Monday 16:00–18:00.
- **Overlapping slots are rejected** with `slots[i] and slots[j] overlap on <Day>`:
  - This also covers a single day overlapping a grouped frequency (Weekdays 09–17 with Wednesday 12–13).
  - It also covers an overnight slot running into the next day's slot, including Sunday night into Monday.
  - Slots that only touch (10–14 and 14–16) are allowed.
  - Exact duplicates are dropped, not rejected (see Storage).

### Other errors
- 404: unknown user.
- 400: missing body, or invalid JSON.
- 500: anything else.
- CORS headers are on every response, and `OPTIONS` returns 204.
- Any method other than POST, PUT or OPTIONS → 405.

## Storage
- **One row per day:**
  - Everyday is saved as 7 rows, Weekdays as Monday–Friday, Weekends as Saturday + Sunday.
  - Exact duplicates of the same day and times are dropped, e.g. Weekdays 09–17 plus Monday 09–17.
  - On read, the rows are grouped back into Everyday, Weekdays or Weekends.
  - Each returned slot's `id` is the smallest `user_availability_id` among its rows.
- **UTC:** `day_of_week`, `start_time` and `end_time` are stored in UTC.
  - Each slot is anchored on its **next occurrence** (counting today) in the user's timezone, and that real UTC instant is stored.
  - So Saturday 22:00 in America/Chicago (CDT) is stored as `Sunday 2026-10-25 03:00`.
  - An overnight end is stored on the following day.
- **Daylight saving:** a UTC offset depends on the date, so a weekly slot's UTC time shifts by an hour across a daylight-saving change.
  - Converting back uses the stored instant, so users always read back exactly what they saved.
  - Other readers of this table, such as future volunteer matching, should convert with `users.time_zone` rather than treat the stored UTC times as fixed for every week.
- **Replace in one transaction:** the delete is flushed before the inserts, so a save fully replaces the previous one.

## Changes (`src/main/java/org/sfa/volunteer/…`)
| File | What it does |
|---|---|
| `model/VolunteerUserAvailability` | Maps `user_availability` per DB wiki #32: `@ManyToOne` user, `IDENTITY` id, `LocalDateTime` (UTC) times, `last_updated_at` |
| `model/AvailabilityFrequency` | The 10 UI frequencies and their days |
| `repository/VolunteerUserAvailabilityRepository` | `findByUser_IdOrderByIdAsc`, `deleteByUser_Id`; this replaces a JPQL query that compared a `User` to a `String` |
| `util/AvailabilityTimeConverter` | Local ↔ UTC conversion and grouping rows back into frequencies |
| `dto/request/AvailabilityUserRequest`, `UpdateAvailabilityRequest`, `AvailabilitySlotRequest`, `dto/response/AvailabilityResponse`, `AvailabilitySlotResponse` | Request and response shapes |
| `service/UserService`, `impl/UserServiceImpl` | `getAvailability`, `updateAvailability`, and the validation |
| `handler/AvailabilityHandler` | One Lambda handler for API Gateway (proxy integration): POST → get, PUT → update, OPTIONS → 204, any other method → 405. Handles CORS, base64 bodies, and maps errors to status codes |
| `controller/UserController` | The same endpoints for Spring: `POST` and `PUT /0.0.1/users/availability` |
| `service/VolunteerService`, `impl/VolunteerServiceImpl`, `dto/response/VolunteerUserAvailabilityResponse` | Removed the unused, broken `get/updateVolunteerUserAvailability`, their helpers, and the DTO that only they used. They no longer compiled against the fixed entity. |
| Tests | `AvailabilityTimeConverterTest`, `UserServiceImplAvailabilityTest`, `AvailabilityHandlerTest`, `UserControllerTest` |
| `ProfileImageSchemaIntegrationTest` | Its `UserServiceImpl` constructor call now passes the right number of arguments. `test` didn't compile its tests, because #209 used 7 arguments after #207 had added an 8th. |

## Deployment checklist
- **API Gateway** `/v1/volunteer/availability`, all with Lambda proxy integration:
  - POST, PUT and OPTIONS → `org.sfa.volunteer.handler.AvailabilityHandler` (a single Lambda)
- **Lambda:**
  - Java 17, using the shaded jar from `mvn package`.
  - Environment variables:
    - `SPRING_DATASOURCE_URL` (`…?currentSchema=<region>_dev_saayam_rdbms`)
    - `SPRING_DATASOURCE_USERNAME`
    - `SPRING_DATASOURCE_PASSWORD`
  - Recommended: `JAVA_TOOL_OPTIONS=-Dspring.devtools.restart.enabled=false`.
- **Database:** `user_availability` must match the "Table after changes" in [DB wiki #32](https://github.com/saayam-for-all/database/wiki/*-Changes-to-the-Database,-Waiting-for-Microservice#32-user_availability): `last_updated_at`, `TIMESTAMP WITHOUT TIME ZONE` times. Check with `\d user_availability`.
- **Webapp:**
  - Add `"AVAILABILITY": "v1/volunteer/availability"` to `endpoints.json`.
  - In `Availability.jsx`, replace the localStorage save with `api.post(AVAILABILITY, { userId })` and `api.put(AVAILABILITY, payload)`.

## Verification
- `mvn verify` on this branch: 194 tests, 0 failures.
- **End-to-end:** the real handlers, created through their no-arg constructors as Lambda does, ran against Postgres 16 with the wiki #32 DDL loaded verbatim. All 24 checks passed:
  - preflight;
  - new user → `[]` with timezone UTC;
  - saving Weekdays 09–17 plus Saturday 22–02 in Chicago writes 6 UTC rows (weekday rows at 14:00–22:00 UTC; Saturday at Sunday 03:00–07:00) and updates `users.time_zone`;
  - a refresh returns the same local slots;
  - replacing them with Everyday 08–10 in Kolkata removes the old rows and writes 7 rows at 02:30–04:30 UTC;
  - a bad timezone, a bad frequency, `24:00`, or start = end → 400, and the DB is unchanged;
  - a missing `userId` or bad JSON → 400;
  - an unknown user → 404;
  - two Monday slots (10–14 and 16–18) → 2 rows, returned in start order;
  - overlapping Monday slots → 400, and the DB is unchanged;
  - `slots: []` clears everything.

## Follow-ups
- **Vacation mode:** needs DB columns (a DB ticket), then wire up the fields that are already accepted.
- **Two timezone writers:**
  - Open PR #211 (`yashvi/personal-info-api`, issue #43) also writes `users.time_zone` from Personal Info.
  - The webapp's Preferences tab has its own timezone picker, which saves only to Cognito `custom:timezone`.
  - Agree on one source of truth.
- **Matching:** the request service's volunteer matching doesn't read availability yet. When it does, it has to handle the daylight-saving note above.
- **Build tooling:**
  - `./mvnw` is broken because `.mvn/wrapper` is missing.
  - `spring-boot-devtools` is bundled in the Lambda jar.

## References
- Webapp `saayam-for-all/webapp@test`:
  - `src/pages/Profile/Availability.jsx`
  - `src/services/endpoints.json`
  - `src/services/volunteerServices.js`
- DB schema: [Changes to the Database, Waiting for Microservice](https://github.com/saayam-for-all/database/wiki/*-Changes-to-the-Database,-Waiting-for-Microservice), section 32.
