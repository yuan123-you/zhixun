# Private Message Commit Effects Implementation Plan

> **For agentic workers:** Execute this bounded task with test-driven-development, systematic-debugging and verification-before-completion. This worker is already parallel with disjoint exam/mall workers; do not dispatch overlapping work.

**Goal:** Persist private messages mandatorily and publish best-effort cache/notification effects only after the outer transaction commits.

**Architecture:** Keep the existing service boundary and transaction annotation. Capture immutable notification fields before registering Spring transaction synchronization; afterCommit performs independently guarded unread-cache, conversation-cache and MQ/WS work. Direct non-transactional calls retain immediate best-effort effects. AI replies retain final-result validation and their existing no-notification contract.

**Tech Stack:** Java, Spring transactions, MyBatis-Plus, Mockito, H2, Maven/JUnit.

**Spec:** Human request in this chat, October 8, 2026.

## Global Constraints
- Write only MessageServiceImpl.java, new focused Message tests and this plan.
- Preserve existing dirty changes (including AiResults.requireFinal), encryption, receiver permissions, response and AI final-output contracts.
- No frontend, other projects, commits, pushes, deployment, live DB, live AI, outbox or invented authorization.
- No applicable filesystem AGENTS.md found at D:\, D:\zhixun or recursively within server/docs; apply supplied AGENTS.md.

## Files
- Modify: D:\zhixun\server\src\main\java\com\zhixun\service\impl\MessageServiceImpl.java
- Create: D:\zhixun\server\src\test\java\com\zhixun\service\impl\MessageCommitEffectsTest.java
- Create/update: D:\zhixun\docs\superpowers\plans\2026-10-08-private-message-commit-effects.md

## Investigation
- sendMessage has @Transactional(rollbackFor = Exception.class), inserts then touches Redis and MQ/WS inline. Outer rollback can therefore leave orphan effects; Redis exceptions can abort persistence.
- Both message insertion paths ignore affected-row counts; createdAt normally comes from MyBatis insert fill, not an explicit service timestamp.
- MQ/WS rebuild payloads and read sender metadata again; Map.of and createdAt.toString can fail on null values.
- sendAIMessage is not annotated transactional and has no Redis/MQ/WS effects. Do not introduce AI network calls inside a new transaction. Its database write joins an existing caller transaction when present.

## Tasks
- [x] Write regression tests using an actual H2 transaction manager, Spring transactional service proxy, real MyBatis message mapper and mocked external dependencies.
- [x] Verify RED for outer commit/rollback ordering, committed row survival on Redis failure, zero-row insertion, timestamps and detached payload.
- [x] Add mandatory insert-count checks and explicit timestamps to both insertion paths.
- [x] Capture immutable payload and register afterCommit (immediate fallback only without an actual transaction); independently guard unread/cache updates and MQ-to-WS fallback.
- [x] Preserve and test encryption, permissions, raw response and AI final-result behavior.
- [x] Run focused Message tests and full server tests; record exact logs/counts and remaining gaps below.

## Verification evidence

All commands ran from D:\zhixun. Only the specified production file, new focused test file and this plan were edited; Maven generated its normal target artifacts. Existing AiResults changes were preserved.

| Phase | Exact command | Tests | Failures | Errors | Skipped | Exit | Finished (Asia/Shanghai) |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| RED (before production fix) | `mvn -f server/pom.xml -Dtest=MessageCommitEffectsTest,MessageAiResultTest test` | 22 | 14 | 0 | 0 | 1 | 2026-10-08 21:06:23 +08:00 |
| Initial GREEN | same focused command | 22 | 0 | 0 | 0 | 0 | 2026-10-08 21:08:13 +08:00 |
| Final focused (expanded coverage) | same focused command | 28 | 0 | 0 | 0 | 0 | 2026-10-08 21:09:27 +08:00 |
| Full server | `mvn -f server/pom.xml test` | 169 | 0 | 0 | 0 | 0 | 2026-10-08 21:10:18 +08:00 |

### Exact output paths
- RED: D:\zhixun\server\target\message-commit-red.log (30.688 s)
- Initial GREEN: D:\zhixun\server\target\message-commit-green.log (57.765 s)
- Final focused: D:\zhixun\server\target\message-commit-focused.log (34.977 s)
- Full suite: D:\zhixun\server\target\message-commit-full.log (34.199 s)
- JUnit XML: D:\zhixun\server\target\surefire-reports\TEST-com.zhixun.service.impl.MessageCommitEffectsTest.xml (26 tests, 0 failures/errors/skips)
- Existing AI-policy XML: D:\zhixun\server\target\surefire-reports\TEST-com.zhixun.service.impl.MessageAiResultTest.xml (2 tests, 0 failures/errors/skips)
- `git diff --check -- server/src/main/java/com/zhixun/service/impl/MessageServiceImpl.java` returned exit 0.

### Behavioral counters and evidence
- Outer transaction: one row is visible on its own connection, zero rows from an independent JDBC connection before commit; the MQ callback sees exactly one committed row from that independent connection.
- Committed send: exactly one MQ send, one unread increment, one unread TTL update, and both conversation-summary writes.
- Explicit outer rollback and thrown outer failure: zero persisted rows and zero Redis/MQ effects. Rollback with a broken broker also makes zero WebSocket fallback attempts.
- Redis unread, TTL, conversation, or all-Redis failure: committed row count stays one and notification still runs.
- MQ failure: exactly one WebSocket fallback after commit. Combined Redis/MQ/WS failure and serialization failure cannot escape as send failure; committed row count stays one.
- Insert count 0 or 2, absent generated ID, mapper exception and actual H2 constraint rejection: mandatory failure, no external effects.
- Payload is immutable/detached from later request, response and user metadata mutation; no user/database reload occurs after commit. Nullable sender metadata no longer suppresses delivery.
- Text storage remains encrypted; send response and notification remain plaintext. Image URLs stay unencrypted. Existing self-message, sensitive-word and mutual-follow permission rejection paths remain intact.
- AI final reply retains plain content, ai_reply type, target-user conversation ownership and AI display identity; explicit timestamp and successful single-row/key checks now apply. Existing blank/error AI final-result rejection tests still pass. AI replies continue to have no Redis/MQ/WS effects and join an existing outer rollback.

### Gaps / deliberate non-goals
- No live MySQL, Redis, RabbitMQ, WebSocket sessions or AI provider was contacted. H2 tests use the real MyBatis insert mapper and Spring REQUIRED transactional proxy; external dependencies are Mockito mocks.
- Best-effort afterCommit is not durable delivery. Process death between commit and callback, broker acceptance uncertainty, failed cache updates and lost fallback delivery remain possible; no outbox/retry/reconciliation added.
- afterCommit effects remain synchronous and may add response latency. No delivery guarantee or performance benchmark is claimed.
- AI generation remains outside a newly introduced service transaction; sendAIMessage has no new annotation. Caller-owned transactions still apply to its write. No authorization changes were introduced.
- Permission tests cover existing restrictive rejection and default allowance, not an exhaustive authorization model. Cryptography is mocked here; existing server suite is the broader regression check.
- Full-suite evidence reflects the shared working tree as it existed for this run, including unrelated pre-existing work; no changes to those files were made by this worker.

