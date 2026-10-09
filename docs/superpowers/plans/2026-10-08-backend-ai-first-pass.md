# Zhixun backend and AI remediation — first-pass implementation plan

> Execute locally task-by-task with test-driven-development and verification-before-completion. No delegation is authorized for this request.

**Goal:** Establish a repeatable backend baseline and repair the confirmed first-pass issue without changing frontend code.

**Architecture:** Preserve existing service interfaces and database contracts. Write a failing regression test before a minimal fix; broaden investigation separately rather than replacing the application.

**Tech Stack:** Java 17 / Spring Boot / MyBatis / MySQL / OpenSearch

**Spec:** User request on 2026-10-08: cover backend, database business logic and AI business; do not modify frontend.

## Global constraints
- Preserve all pre-existing uncommitted changes; no reset, overwrite, commit, push or deployment.
- No production database writes or migrations in this pass.
- Business-rule ambiguities must be confirmed with the user before implementing.
- Mock external services for regression tests; do not spend AI tokens or call production endpoints.
- Passing existing unit tests is not evidence that end-to-end business is correct.

### Task 1: Resolve the existing compilation blocker after user confirmation
Files involved: server/src/main/java/com/zhixun/vo/CommentVO.java; server/src/main/java/com/zhixun/service/impl/AdminServiceImpl.java.
Evidence: mvn -B test fails at AdminServiceImpl:554 calling setArticleTitle. Local uncommitted changes removed the articleTitle field.
- [x] Run baseline compilation and record the failure.
- [ ] Confirm whether the title field must be retained or removed; do not reverse the user's uncommitted change without confirmation.
- [ ] Add a contract regression test for the agreed response shape and make the smallest compatible fix.
- [ ] Run mvn -B test successfully before claiming verification.

### Follow-up investigation (not yet diagnosed or implemented)
Verify text/image AI failures, retry/cancellation, upstream response validation, AI errors stored as content, article visibility/moderation, interaction count consistency and search synchronization. Maintain current frontend contracts unless a rule change is explicitly agreed.

## Later resolution (2026-10-08)
The user confirmed retention of CommentVO.articleTitle. Restored its contract and compile/test feedback; see 2026-10-08-comment-contract-and-business-chains.md. Earlier blocking notes are historical, not the current state.
