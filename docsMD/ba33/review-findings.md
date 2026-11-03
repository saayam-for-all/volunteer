# BA 33 documentation review findings

Reviewed by two independent review-only sub-agents after requirements.md and design.md were drafted. Neither reviewer changed application code, ran tests, or accessed credentials/databases. This ledger records design dispositions, not proof that implementation already exists.

## Requirements and API reviewer

| ID | Priority | Finding | Disposition in the documents |
|---|---|---|---|
| R1 | High | Completion endpoint can bypass Step 4 validity/progress guarantees. | Incorporated: requirements R9 and explicit updatecompletion/profile-stage contract; corresponding bypass tests are mandatory. |
| R2 | High | Canonical schedule cutover can hide JSON-only records or leave old readers stale. | Incorporated: named/batched reader projections, explicit migration-required canonical response, preserved legacy visibility, clearing obsolete JSON on successful shared replacements. Automatic bulk data conversion is deferred as scope item F1 below. |
| R3 | Medium | Legacy DTO/payload policy is undecided. | Incorporated: concrete canonical/legacy route matrix, HH:mm-only Step 4 contract, type-correct step validation, and explicit frontend cutover gate. |
| R4 | Medium | Stored IANA timezone prerequisite is not a BRD requirement. | Incorporated: assumption A6 labels it as a consequence of shared UTC persistence, with explicit 409 and account-remediation release gate. |
| R5 | High, recheck | Earlier-step edits and stale full-User saves can regress the newly protected progress. | Incorporated: all retained progress writers take the common row lock and use monotonic narrow state updates; profile updates lock/refresh before applying other fields. Earlier-step and concurrent stale-update acceptance tests are required. |

## Technical and data reviewer

| ID | Priority | Finding | Disposition in the documents |
|---|---|---|---|
| D1 | High | Completion, profile update and earlier-step edits can independently overwrite wizard state. | Incorporated: full stage-writer inventory; completion acknowledgement/advance policy; reject changed client stage/timestamp fields; lock/refresh profile writes; monotonic narrow updates for earlier steps and concurrent-state tests. |
| D2 | High | Proposed Spring verified-identity middleware does not exist. | Incorporated: concrete AWS serverless proxy attribute and typed claims extraction, fail-closed direct HTTP, real proxy integration acceptance test, and explicit F2/F3 topology limits. |
| D3 | High | Shared account projection could break Profile before emergency migration. | Incorporated: base versus onboarding-state projection separation; Profile does not select the new column; pre-emergency-schema regression. |
| D4 | Medium | JSON reader/data transition is still an implementation choice. | Incorporated: concrete single/page/steward projection policy and legacy-only schedule behavior; F1 remains an explicit operational follow-up. |
| D5 | Medium | Consistent-read isolation is not placed at the actual transaction boundary. | Incorporated: REPEATABLE READ on externally invoked read methods, common write lock and order, same JPA/JDBC datasource, two-connection concurrency tests. |
| D6 | Medium | Legacy routes and LocalDateTime payload compatibility are ambiguous. | Incorporated: route/DTO matrix, strict local-time contract, rejection rather than silent timestamp conversion, and staged frontend handoff. |
| D7 | Medium | Nullable-existing migration and deterministic clock seam unspecified. | Incorporated: transactional type precheck, NULL backfill, TRUE preservation, DEFAULT/NOT NULL and rerun tests; explicit injected Clock. |

## Explicit deferrals and release dependencies

These are deliberately outside the local backend implementation. The design records the code behavior that makes each boundary safe; deferral does not authorize deploying an incomplete topology or changing shared data.

| ID | Deferred work / decision | Why | Owner / unblock condition | Notification |
|---|---|---|---|---|
| F1 | Automatic bulk backfill of historical JSON-only availability. | Historical date/time provenance can be ambiguous; a blind conversion could corrupt schedules. | DB/product owner inventories affected accounts and approves validated backfill or user re-entry. Preserve legacy views and return canonical migration-required status until resolved. | User notified in commentary during this review; reiterated in final handoff. |
| F2 | Authenticated direct standalone-Spring HTTP integration. | This repository has no verified-principal bridge for that topology. The initial design uses existing API Gateway/Cognito/serverless context. | Implement and review a dedicated authentication integration if standalone HTTP deployment is required. Direct calls currently remain fail-closed. | User notified in commentary during this review; reiterated in final handoff. |
| F3 | API Gateway HTTP API v2 event support. | The chosen implementation and existing adapter are REST API event based. | Gateway owner confirms REST topology; add a separate adapter/test slice if v2 is required. | Explicitly included in final handoff to the user. |

External release decisions are not silently treated as approved: DB migration deployment (including the missing emergency column), frontend contract adoption, valid stored account timezones, provisional onboarding 20-slot bound, gateway authorizer/resource policy verification, and final PR dependency/target. They remain rollout gates even if local code/tests later pass.

## Recheck status

Initial reviews: not ready for task decomposition until the listed design decisions were settled. Both original reviewers then performed a bounded recheck. The requirements reviewer marked R1–R4 addressed and raised R5; the technical reviewer marked D2–D7 addressed and identified the same remaining D1 monotonic-progress issue. The final revision explicitly closes that gap and qualifies Profile compatibility with the intentional R14 migration guard. Both reviewers' remaining readiness conditions have been incorporated; there are no known unresolved blocking documentation findings before task planning.

Review outcome: ready for task decomposition, not authorization to code, deploy or push. F1–F3 remain explicit deferrals and all external rollout gates remain in force. Their status is not upgraded by creating tasks.md.

No application coding, DB migration execution, commits, pushes, or PR changes were performed in this documentation pass.
