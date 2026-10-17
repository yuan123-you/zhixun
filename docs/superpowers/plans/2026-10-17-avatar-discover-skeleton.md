# Avatar, Discover and Skeleton Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop unnecessary CORS avatar requests, make creator recommendation states truthful, and align loading geometry with the rendered cards.

**Architecture:** Retain native image loading for cross-origin avatars and canvas persistence only for same-origin images. Fix discovery requests at the existing API boundary and keep loading, error and empty UI separate. Reuse a small shared card geometry stylesheet and make skeleton counts explicit rather than nesting implicit lists.

**Tech Stack:** Vue 3, TypeScript, Vitest, Vue Test Utils, Vite, browser layout checks.

**Spec:** User's 2026-10-17 console attachment and screenshot in this chat. Previous pending regression tests remain part of verification.

## Constraints
- Minimal targeted changes; no production data changes, no domain blacklist and no enabling disabled seed accounts.
- Write and run failing tests before code changes.
- Do not claim that an empty eligible creator pool contains users or that failed API requests are empty successful responses.

## Tasks
- [x] Test remote avatars without CORS and preserved same-origin caching. Test stale asynchronous avatar results.
- [x] Test creator request pagination, failed initial load, retained data after failed refresh, genuine empty data and skeleton count.
- [x] Test one skeleton per requested card and page-sized initial loading. Capture real pre-fix card dimensions using the browser fixture.
- [x] Fix avatar caching and source guards; fix recommendation query parameters, error/retry UI and compact empty states.
- [x] Match skeleton layout, breakpoints and cover sizes to shared article metrics. Use the same row classes and request counts for discovery skeletons.
- [x] Run all client tests and a production build; verify mobile/desktop geometry and public images in the browser. Preserve unrelated user changes.

## Evidence collected
- User-supplied generated-avatar URL: ordinary request returns 200 image/jpeg; adding Origin: https://zhixun.iflare.cn returns 403. The fix skips the cross-origin canvas preload without blocking the hostname or disabling native image display.
- Initial new tests: 10 failures / 11 tests, covering the CORS path, multiplied skeletons, flat pagination parameters and truthful creator UI. Additional avatar race, native load-state and tag-chip tests were also observed failing before their respective fixes.
- Before: mobile placeholder 200px high versus article ~145px, and placeholder cover 120x88 versus real 60x46. Desktop skeleton width was ~32px narrower due to its extra wrapper padding.
- After: mobile skeleton, normal article and short/no-cover article all ~151.4px high with identical widths; desktop all ~175.4px. Placeholder cover equals the real cover at both breakpoints. No horizontal overflow in the tested fixture.
- The reported food and design avatar images both decoded successfully in the browser without crossorigin attributes or image errors. Real public-feed avatars also loaded without new CORS logs.
- Public discovery API returned two eligible users anonymously; the local discovery UI rendered both, 15 rank entries and 16 tag entries. Genuine empty eligible pools remain empty and use a ~152px compact state, not fabricated users.
- Production build (including vue-tsc) passed. Existing dependency PURE-comment and bundle-size warnings are non-fatal.
- One parallel full-suite run exposed an existing 2ms real-timer storage TTL test flake; storage production code/tests were left unchanged and the final full suite is checked sequentially below.
- Screenshots: `docs/verification/2026-10-17-skeleton-alignment-mobile.png` (local ignored QA artifact). Browser fixture: `client/tests/fixtures/article-layout.html`.
- No deployment, production write, account/follow action, registration or password reset was performed.

## Final verification
- `npm --prefix client test -- --fileParallelism=false`: 13 files, 144 tests passed, exit 0.
- `npm --prefix client run build`: vue-tsc and Vite production build passed, exit 0.
- `git diff --check`: passed. All modifications remain local and uncommitted.
