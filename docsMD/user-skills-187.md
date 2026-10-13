# Get profile skills: issue #187

## Context
[#187](https://github.com/saayam-for-all/volunteer/issues/187): Profile → Skills shows "Failed to load skills. Please refresh the page and try again".

**Root cause:** the webapp calls `POST v1/volunteer/profileSkills`, but `test` had no backend for it.

## Branches
1. `ramya_jahnavi_#93` (from `test`). Commit `f6fb0ee` "done changes to get the profile skills" adds the Spring endpoint and service.
2. `vishaakr98_feature/bug-detail_187` (from `test`). Takes only the get-skills part of that work, adds the Lambda handler and skill levels. **PR target: `test`.**

## API
`POST /profileSkills`. Headers: `Content-Type: application/json`, `Authorization: <Cognito idToken>`.

```json
// request
{ "userId": "SID-00-000-002-10000" }
// response (envelope: success, statusCode, saayamCode, message, data, timestamp)
{ "data": { "userId": "SID-00-000-002-10000", "skills": ["2.1"], "skillLevels": { "2.1": null } } }
```
- The webapp reads `data.skills` (category IDs from `help_categories`; e.g. `2.1` is DONATE_CLOTHES).
- A user with no skills gets 200 with `skills: []`.
- Errors:
  - 400: missing body / bad JSON / missing `userId`.
  - 404: unknown user.
  - 500: anything else.
- CORS headers are on every response, and `OPTIONS` returns 204.

## Changes (`src/main/java/org/sfa/volunteer/…`)
| File | What it does |
|---|---|
| `model/UserSkills`, `UserSkillId`, `SkillLevel` | Maps `user_skills` (key `user_id` + `cat_id`, `skill_level` enum) |
| `repository/UserSkillRepository` | `findByIdUserId` |
| `dto/request/UserSkillsRequest`, `dto/response/UserSkillsResponse` | Request / response shapes |
| `service/UserService`, `impl/UserServiceImpl.getUserSkills` | 404 if the user doesn't exist, else returns skill IDs and levels |
| `handler/GetProfileSkillsHandler` | Lambda handler for API Gateway (proxy): CORS, preflight, base64 bodies, error → status |
| `controller/UserController` | Same endpoint for Spring: `POST /0.0.1/users/profileSkills` |
| Tests | `GetProfileSkillsHandlerTest`, `UserServiceImplSkillsTest`, `UserControllerTest` |

## Deployment checklist
- **API Gateway** `/v1/volunteer/profileSkills`: POST and OPTIONS → `org.sfa.volunteer.handler.GetProfileSkillsHandler`, using Lambda proxy integration.
- **Lambda:**
  - Java 17, shaded jar from `mvn package`.
  - Env vars `SPRING_DATASOURCE_URL` (`…?currentSchema=<region>_dev_saayam_rdbms`), `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
  - Recommended: `JAVA_TOOL_OPTIONS=-Dspring.devtools.restart.enabled=false`.
- **Database:** `user_skills` must have the `skill_levels` enum and the `skill_level` column ([DB wiki #30](https://github.com/saayam-for-all/database/wiki/*-Changes-to-the-Database,-Waiting-for-Microservice#30-user_skills)). Check with `\d user_skills`.

## Verification
- `mvn verify` on `test`: 94 tests, 0 failures.
- End-to-end: the real handler (no-arg constructor, as Lambda creates it) against Postgres 16 with the wiki #30 DDL. All pass:
  - preflight;
  - no skills → `[]`;
  - saved rows → IDs and levels;
  - unknown user → 404;
  - missing `userId` → 400;
  - bad JSON → 400.

## Follow-ups
- Confirm with the DB team that wiki #30 (`skill_level`) is live in the test DB. It is still listed under "Needed Changes".
- Saving and deleting skills (PUT / DELETE `profileSkills`) are not in this change.
- `./mvnw` is broken because `.mvn/wrapper` is missing.
- `spring-boot-devtools` is bundled in the Lambda jar.

## References
- Webapp `saayam-for-all/webapp@test`:
  - `src/pages/Profile/Skills.jsx`.
  - `src/services/volunteerServices.js` (`fetchUserSkills`).
  - `src/services/endpoints.json` (`PROFILE_SKILLS`).
- DB schema: [Changes to the Database, Waiting for Microservice](https://github.com/saayam-for-all/database/wiki/*-Changes-to-the-Database,-Waiting-for-Microservice), section 30.
