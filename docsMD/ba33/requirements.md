# BA 33 onboarding availability requirements

Status: revised after independent requirements and technical reviews; final review recorded in review-findings.md. The user explicitly approved local implementation on 2026-10-30. Shared-database changes, commits, pushes and deployments remain unauthorized.

## Objective and ownership

Complete the backend for Become a Volunteer, Step 4 Availability. A volunteer can save and retrieve a validated weekly schedule and an independent emergency availability preference, then proceed to Review. Our working scope is backend implementation in `volunteer`; frontend and shared-database deployment remain coordinated dependencies.

Build on volunteer PR 215 rather than create another schedule store. PR 215 covers Profile Availability, not the complete onboarding flow. The user authorized using it as the local implementation foundation; merge approval and the final PR target have not been confirmed by the lead.

## Evidence and precedence

1. [BA 33](https://github.com/saayam-for-all/ba/issues/33), its issue-body August 2026 BRD, and issue-body QA checklist define the product behavior. The FRD attached in the discussion supplies detailed field mapping where consistent with that BRD.
2. The synced `test` baseline at `09b0a33a2c76f30889cd3d6f51f75a4420717745` supplies the reusable Profile implementation from PR 215 and the follow-up in PR 224. The BRD and QA checklist govern onboarding rules. [PR 215](https://github.com/saayam-for-all/volunteer/pull/215) · [PR 224](https://github.com/saayam-for-all/volunteer/pull/224)
3. [Database wiki section 32](https://github.com/saayam-for-all/database/wiki/*-Changes-to-the-Database,-Waiting-for-Microservice#32-user_availability) and the read-only schema inspection on 2026-10-30 establish the current physical availability-column shape. Older DDL examples are not an instruction to change a shared database.
4. PR 150 contains PR 145's implementation plus a follow-up commit; both remain unmerged alternatives. Their tests may inform behavior coverage, but their implementation is not merged wholesale.

## Product requirements

| ID | Requirement | Acceptance evidence |
|---|---|---|
| R1 | Accept a user ID, weekly availability slots, and an explicit boolean emergency preference for Step 4. | A valid request saves and is returned on read. |
| R2 | Each slot contains a recognized day, start time, and end time. The onboarding choices are Monday through Sunday and Everyday. | Missing/null/unknown values return HTTP 400, including null slot entries. |
| R3 | Require at least one complete slot even when emergency availability is true. | An empty or incomplete schedule cannot complete Step 4. |
| R4 | Accept minute-precision local times in strict 24-hour `HH:mm`; end must be strictly later than start. Overnight slots and equal start/end are invalid for onboarding. | Boundary and malformed-time cases return 400. |
| R5 | Allow multiple non-overlapping slots on the same day and identical times on different days. Adjacent intervals are allowed. | Monday 09:00–12:00 plus 12:00–14:00 succeeds; intersecting Monday intervals fail. |
| R6 | Expand Everyday to seven weekdays before storage and overlap checks. Exact duplicate occupied intervals count as overlaps in onboarding. | Everyday 09:00–12:00 conflicts with Monday 10:00–11:00 or Monday 09:00–12:00. |
| R7 | Store the canonical schedule in `user_availability`, with emergency preference separately in `users.is_emergency_available`. | A save/read round trip uses relational slot rows and returns the independent boolean. |
| R8 | Saving replaces the user's prior schedule, including removed slots. Reload returns the stored schedule, not session/browser cache or stale JSON fields. | Save A, replace with B, read B; no leftover A slots. |
| R9 | Slots, emergency preference, and Step 4 progress commit atomically. Failure must preserve the previous complete state. Editing any earlier step must not move a later wizard stage backwards, including concurrent stale updates. Other routes cannot independently claim Step 4 completion or overwrite its progress. | Forced failure, completion/profile-update bypass, earlier-step edit and concurrent stale-progress tests through real public interfaces. |
| R10 | Profile Availability behavior from PR 215 remains supported: empty-schedule clearing, overnight schedules, timezone handling, grouping and duplicate de-duplication are unchanged, except the explicit historical-data migration guard in R14. | Existing Profile contract tests plus targeted shared-storage and intentional migration-guard regressions pass. |
| R11 | Only a caller authorized for the target account can read or change onboarding availability. Caller identity comes from a verified server-side authentication context, not submitted user ID or arbitrary client headers. | Missing/forged identity and another account fail without persistence. |
| R12 | Spring and Lambda adapters return consistent actual HTTP statuses and application envelopes: 200 success, 400 invalid input, 403 unauthorized target/context, 404 missing account/volunteer, 409 account-state/migration precondition, 503 missing onboarding schema, 500 unexpected failure. | Adapter tests assert both status and body; error responses contain no credentials or full payloads. |
| R13 | Storage compatibility must preserve entered local times while reusing PR 215's UTC persistence. No new timezone scheduling control is added. Requiring a valid stored IANA timezone is architectural assumption A6, not a BRD requirement. | Non-UTC round trip and an explicit 409 for unremediated account timezone; no silent timezone guess. |
| R14 | Existing volunteer and steward availability reads must not return stale JSON after a relational save or silently hide historical JSON-only schedules. | Batch relational projections, visible legacy migration indication, and canonical-read 409 for unmigrated JSON-only schedules. |

## Scope boundaries

Included: backend save/read contract, validation, emergency preference, atomic progress update, consistent API errors, tests, and a proposed additive SQL migration plus deployment notes.

Excluded: frontend implementation, modifying or deploying shared database schema, pushing/merging PRs, automatic historical JSON backfill, calendar sync, holidays, recurring exceptions, notification/matching changes, vacation mode, infrastructure redesign, direct standalone-Spring JWT authentication, and a global repair of all legacy user entity mappings.

Onboarding reuses internal timezone conversion solely for storage compatibility. That does not extend the product scope to timezone-aware scheduling controls. Existing Profile-originated overnight schedules must be returned faithfully on onboarding read; a subsequent onboarding save must satisfy onboarding rules rather than silently truncate or delete those intervals.

## Assumptions and unresolved external decisions

| ID | Decision / assumption | Consequence |
|---|---|---|
| A1 | Backend ownership is the user's working scope; lead has not yet specified the final handoff. | Keep frontend work as a documented integration contract. |
| A2 | The issue-body BRD's mandatory-slot and overlap rules take precedence over older open questions. Adjacent intervals are interpreted as non-overlapping. | Record these choices for product review; do not inherit Profile's empty/overnight semantics. |
| A3 | The synced `test` baseline includes the Profile Availability foundation from PR 215. The eventual PR target still needs lead confirmation. | Document the pinned base and keep agent changes local. |
| A4 | Use the existing Profile limit of 20 input slots as a proposed operational bound for onboarding, not a BRD-mandated limit. | Explicit product confirmation is still required before release; tests make the bound visible. |
| A5 | Emergency edits are self-service. No new administrative override policy is introduced. | Privileged editing beyond the established identity contract requires a separate decision. |
| A6 | Reusing PR 215's UTC representation requires a valid stored IANA timezone, although the onboarding FRD excludes timezone-aware scheduling. | Before rollout, owners remediate missing/invalid account zones through the existing profile flow; until then onboarding returns 409 without modifying data. |
| A7 | Authorized Spring traffic arrives through the existing AWS serverless proxy with verified Cognito context. Standalone HTTP has no verified identity integration today. | Direct unauthenticated Spring calls fail closed; authorized local tests invoke the real proxy with synthetic authorizer events. Standalone JWT integration is deferred explicitly. |

The configured development schema currently has timestamp-without-time-zone slot fields and `last_updated_at`, but no emergency column. This is a deployment dependency, not a reason to write to the shared DB during development. Tests use isolated PostgreSQL and synthetic data.

## Completion gates

1. All R1–R14 behaviors trace to implementation tasks and behavioral tests.
2. The schema migration is reviewed and applied by the authorized DB owner before the new feature is enabled in a shared environment.
3. Frontend owners adopt the documented `HH:mm` request contract; current ISO timestamp submission is not silently reinterpreted.
4. Production identity propagation and API Gateway routing are verified; an unverified public Spring route is not accepted as complete.
5. No unresolved high-severity review findings. Every review finding is incorporated or recorded as deferred with the user notified.
6. Application coding starts only after explicit user authorization. All coding sub-agents use the TDD skill and one behavioral red–green–refactor cycle at a time.
7. Historical JSON-only schedules are inventoried by the data owner before rollout. They remain visible through legacy reads, and canonical reads return an explicit migration-required response. No automatic conversion of ambiguous ISO/local historical values is approved here; any bulk backfill requires a separately validated migration.
