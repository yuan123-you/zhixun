# Article commit effects implementation plan

> Implement locally via test-driven-development. No frontend/schema/deployment changes or live external calls.

**Goal:** Search/feed/cache/broker effects must not announce uncommitted/rolled-back article changes. Scheduled publication cannot overwrite concurrent rejection, deletion or rescheduling.
**Architecture:** Defer existing external effects until the outer article transaction commits, with independent failure isolation. Use a narrow conditional UPDATE for the existing due-PENDING scheduling rule, checking latest database state without rewriting stale article fields.
**Tech Stack:** Java 17 / Spring transaction synchronization / MyBatis-Plus / JUnit / isolated H2.
**Spec:** Current ArticleService public write paths and existing scheduling predicate. This plan preserves that predicate, not an assertion that its approval policy is correct.

## Constraints
- Approval-versus-scheduling ambiguity remains a user decision. No new content state, approval flag, queue technology or auto-approval policy.
- Preserve role checks and existing notification JSON. Deferral does not provide durable retries, ordering or exactly-once delivery.
- Capture article IDs/author IDs/timestamps rather than mutable caller objects in callbacks.
- External failures after commit must not turn a completed write into failure, and one failure must not prevent independent effects.

### Task 1: Reproducing tests
Files: server/src/test/java/com/zhixun/service/impl/ArticleCommitEffectsTest.java; ArticleScheduledTransactionTest.java; server/pom.xml (test-only H2).
- [x] Reproduce effects before commit and after rollback through existing public write methods.
- [x] Reproduce stale scheduled write overriding rejection/rescheduling/deletion and zero-row writes announcing publication.
- [x] Verify actual mapper/transaction behavior using isolated H2 and no real search, Redis or broker.

### Task 2: Targeted integration
Files: server/src/main/java/com/zhixun/service/impl/ArticleServiceImpl.java; mapper/ArticleMapper.java.
- [x] Queue existing search/feed/cache/broker effects for successful outer commit across create/update/delete/status/scheduled/draft/visibility/cleanup paths.
- [x] Replace full stale-entity scheduled update with guarded status-only SQL; zero rows means skip.
- [x] Run full regression and review. Record remaining approval ambiguity and production database/sync-recovery work.

## Execution evidence / remaining acceptance
- Comment contract: original compilation failure reproduced; restoration and 3 new contract tests passed. First restored full baseline: 43 tests passed.
- AI contract regressions: 23 tests / 20 initial failures, then all passed. Article callback regressions: 19 tests / 18 initial failures, then review strengthened the cache and metadata cases. Initial SQL suite: 10 tests / 6 failures; required metadata rollback regressions failed before fixing. Initial socket suite: 3 membership failures.
- Final scoped AI/socket/pool suite: 32 tests passed. Final article/SQL suite: 33 tests passed. Final current-worktree full suite: 112 tests passed, 0 failures/errors.
- This pass adds 68 tests including title coverage; the current tree also includes 4 independently added CredentialSafetyTest cases, preserved rather than attributed to this work.
- Reviewer findings were reproduced and repaired; bounded read-only follow-ups found no remaining important issues in their reviewed scopes.
- [ ] Confirm approval-versus-scheduling rules; do not treat legacy PENDING as approved.
- [ ] Verify target MySQL/Redis/OpenSearch/RabbitMQ and real model behavior in isolated end-to-end acceptance.
- [ ] Durable jobs/retry/outbox, frontend async-failure UX and broader policy/whole-platform refactoring remain unfinished.
