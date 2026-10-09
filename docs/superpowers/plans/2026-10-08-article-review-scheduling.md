# Article review / scheduling separation implementation plan

> Execute locally using test-driven-development and verification-before-completion. Read-only review follows requesting-code-review; no frontend edits, commits, deployment or live service calls.

**Goal:** Sensitive content pending review cannot publish merely because its date is due. Both audit entry points grant current-content permission, while future approved content waits for its date.
**Architecture:** Add a server-owned review_state (UNVERIFIED/REQUIRED/CLEARED/APPROVED) independent of the existing public status codes 0–4. A focused policy shared by creation/edit/draft/audit controls permission vs date. Lock existing articles for content, review and related full-row mutations; the scheduler checks review eligibility again in guarded SQL.
**Tech Stack:** Java 17 / Spring / MyBatis-Plus / MySQL; JUnit/Mockito and isolated H2.
**Spec:** User confirmed on 2026-10-08 that sensitive pending-review content requires approval even with a scheduled date; CONTEXT.md defines the distinction.

## Constraints
- No new frontend status code. Expose review_state as additional metadata; frontend presentation remains deferred.
- New safe content may be auto-cleared under the current rule, but date alone never clears a required/unknown review hold.
- Creation with a future date must wait, not publish early. Human approval of future content means APPROVED + existing PENDING until due.
- An existing required hold is not removed merely by editing away a word; changes to approved editorial content invalidate approval. Metadata-only edits do not silently erase content or approvals.
- Screening/filter exceptions never become a clean result. No writes or effects if the screening process cannot establish a result.
- Audit and edit operations serialize on the article row; scheduler guarded update requires CLEARED/APPROVED and latest due/nondeleted PENDING state.
- Legacy existing rows default UNVERIFIED; do not fabricate prior approval from dates/statuses/logs. Existing published history is not bulk-unpublished; new publication paths fail closed for unknown pending review.
- Manual additive migration before deployment. Do not change the old migration runner's incomplete registry or accidentally execute skipped historic scripts.

### Task 1: Regression scenarios before implementation
Files: server/src/test/java/com/zhixun/service/impl/ArticleReviewSchedulingTest.java; ArticleReviewSqlTest.java; AdminAuditSchedulingTest.java; review schema tests.
- [x] Reproduce sensitive due content, future clean create, future approval, draft bypass, edit invalidation and detector failure through existing public entry points.
- [x] Reproduce both distinct admin audit paths and wrong approval notifications.
- [x] Use real isolated SQL/transaction tests to prove required/unknown rows never schedule and current-content changes invalidate approval.

### Task 2: State policy, locks and persistence
Files: entity/Article.java; service/ArticleReviewPolicy.java; mapper/ArticleMapper.java; service/impl/ArticleServiceImpl.java; AdminServiceImpl.java; ArticleVO/ArticleDetailVO metadata; initial schema.sql and db/01-schema.sql; root db/migrations/2026-10-08-article-review-state.sql.
- [x] Add review_state and fail-closed metadata defaults. Reuse existing public status codes.
- [x] Apply one policy at create/edit/publishDraft/approval/rejection; preserve queued dates and mark current approval.
- [x] Lock before mutating current content/review/full-row state. Check affected-row results and explicitly clear rejection text in review SQL.
- [x] Filter genuine review queue separately from cleared/approved waiting-for-date rows; keep notification text accurate.
- [x] Update scheduler's selection AND SQL predicate. No side effects on skipped/rolled-back writes.

### Task 3: Verify / deployment boundaries
- [x] Adapt only valid existing scheduling fixtures to explicit CLEARED permission; retain negative unknown-history coverage.
- [x] Run focused and full regression and diff checks; get bounded read-only review.
- [x] Document stop-old-schedulers / schema-first rollout, trustworthy historic recovery, no production changes performed and remaining broader workflows.


## Execution evidence (2026-10-08)

- Baseline: 112 passing tests before this round.
- Initial red: 16 policy/audit failures; real SQL suite 7 cases / 5 failures; schema test failed before migration existed.
- Initial acceptance: 58 passing; concurrency SQL suite: 8 passing.
- Review findings reproduced: 4 failing cases covering unknown draft holds, image index removal and alternate audit integration; targeted recheck: 65 passing.
- Bounded read-only review found scheduled image restoration missing; a new regression failed specifically on syncImagesByArticle before the one-line correction.
- Final full verification: 143 tests, 0 failures/errors/skips, BUILD SUCCESS at 20:29:13 +08:00. Details are recorded in the round-4 report and server/target/article-review-final-verified.log.
- Scoped tracked-source git diff --check passed. Existing unrelated frontend/configuration/document changes were preserved, not modified by this round.
- Manual schema-first deployment instructions are in 2026-10-08-article-review-deployment.md. No live migration, external provider call, deployment, commit or push was executed.
