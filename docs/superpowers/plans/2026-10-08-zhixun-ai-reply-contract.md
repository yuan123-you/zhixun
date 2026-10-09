# Zhixun AI reply contract implementation plan

> Implement locally with test-driven-development and verification-before-completion; bounded read-only review via requesting-code-review. No frontend edits, live DB/model/broker calls or deployment.

**Goal:** Only validated final AI results may be reported as successful or saved as replies. Group AI jobs use managed resources and cannot persist after the group/member eligibility is lost.
**Architecture:** A shared AI result contract is used by HTTP and message consumers. Extract a group reply module behind the existing GroupService entry point, with an injected bounded executor and conditional database INSERT requiring a normal group and requesting membership. Use existing CHAT success and requester-only ERROR websocket payloads; no fabricated chat errors.
**Tech Stack:** Java 17 / Spring / MyBatis-Plus / JUnit / Mockito / isolated H2.
**Spec:** Existing authenticated endpoints, group isMember rule, ERROR websocket protocol and API VO fields. Earlier user request covers backend/DB/AI, frontend unchanged.

## Constraints
- Existing group/message JSON remains compatible. Empty/error/null results cannot be normal content; a reasoning-only response is not a final answer.
- Image generation currently returns a static placeholder and is not implemented. Fail explicitly rather than claim a real image exists; real image-provider integration is not claimed in this pass.
- Do not change ban/mute/user quota policies. Existing basic group-normal/member rule is checked before queueing, before provider work and by the final write.
- Executor defaults 2 core / 4 max / 32 queued, operator-configurable resource limits, not billing or per-user business quotas. Rejection uses the existing 503 business error contract.
- Queued work is not durable; ERROR is an existing protocol but current frontend loading UX is not redesigned.

### Task 1: Reproduce reply false-success and invalid persistence
Files: server/src/test/java/com/zhixun/service/impl/GroupAiDeliveryTest.java; MessageAiResultTest.java; controller/AiResultContractTest.java; service/impl/AiProviderFinalResultTest.java.
- [x] Drive existing APIs using a controlled mock asynchronous dispatch seam and fake provider; reproduce error/blank/invalid eligibility persistence and placeholder image success.
- [x] Verify negative tests fail against current implementations before patching.

### Task 2: Shared outcomes and group task module
Files: server/src/main/java/com/zhixun/service/ai/AiResults.java; GroupAiReplies.java; config/GroupAiTaskConfig.java; mapper/GroupMessageMapper.java; service/impl/GroupServiceImpl.java; MessageServiceImpl.java; AIServiceImpl.java; controller/AIController.java; websocket/GroupChatWebSocketHandler.java.
- [x] Add final-text validation, error results mapped to existing 503 behavior and no empty fallback message.
- [x] Replace common-pool fire-and-forget with injected configurable bounded execution.
- [x] Validate current eligibility and use conditional INSERT before emitting CHAT. Suppress persistence/success if final INSERT affects zero rows.
- [x] Send generic failure to requester sessions only via existing ERROR format; isolate websocket failure from persistence result.
- [x] Verify real conditional mapper writes in isolated H2, role changes and queue rejection. No new database table or migration.
- [x] Run full server regression; document real provider/database/UX limitations.

## Execution evidence / remaining acceptance
- Comment contract: original compilation failure reproduced; restoration and 3 new contract tests passed. First restored full baseline: 43 tests passed.
- AI contract regressions: 23 tests / 20 initial failures, then all passed. Article callback regressions: 19 tests / 18 initial failures, then review strengthened the cache and metadata cases. Initial SQL suite: 10 tests / 6 failures; required metadata rollback regressions failed before fixing. Initial socket suite: 3 membership failures.
- Final scoped AI/socket/pool suite: 32 tests passed. Final article/SQL suite: 33 tests passed. Final current-worktree full suite: 112 tests passed, 0 failures/errors.
- This pass adds 68 tests including title coverage; the current tree also includes 4 independently added CredentialSafetyTest cases, preserved rather than attributed to this work.
- Reviewer findings were reproduced and repaired; bounded read-only follow-ups found no remaining important issues in their reviewed scopes.
- [ ] Confirm approval-versus-scheduling rules; do not treat legacy PENDING as approved.
- [ ] Verify target MySQL/Redis/OpenSearch/RabbitMQ and real model behavior in isolated end-to-end acceptance.
- [ ] Durable jobs/retry/outbox, frontend async-failure UX and broader policy/whole-platform refactoring remain unfinished.
