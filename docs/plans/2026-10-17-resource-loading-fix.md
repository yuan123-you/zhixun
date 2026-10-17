# Resource Loading Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Restore valid article images and make asset loading failures recoverable without hiding deployment outages.

**Architecture:** Keep the existing URL resolver, Vue cards and error overlay. Remove the disproven domain blacklist, reset card error state when the source changes, and route Vite preload failures into the existing user-controlled refresh UI. No automatic reload or API replay.

**Tech Stack:** Vue 3, Vite 6, Vitest, Vue Test Utils, Nginx.

**Spec:** User screenshot and console attachment supplied on 2026-10-17; scope is image loading, failed chunks/styles/API availability, and auth autocomplete warnings.

## Global Constraints
- Minimal targeted changes; do not modify unrelated services or production data.
- Write reproducing tests before implementation; verify failures then passes.
- Do not mistake Chrome LanguageDetector informational output for an application bug.

## Tasks
- [x] Add Vue SFC support to the test runner and regression tests for the real resolver, card source changes, auth fields, and error overlay.
- [x] Run the new tests and confirm the domain blacklist, stale cover errors, missing autocomplete, and swallowed preload failures reproduce.
- [x] Remove the blacklist in `client/src/composables/useResourceUrl.ts`; retain MinIO proxy rules.
- [x] In both article cards, derive a resolved cover URL; never render an empty image src; reset failed state on a new source.
- [x] Add `autocomplete="one-time-code"` and numeric input mode to email codes; identify email/QQ fields and disable arithmetic CAPTCHA autocomplete.
- [x] Listen to `vite:preloadError` in the existing error plugin and offer explicit refresh on mobile and desktop. Do not suppress dynamic-import failures as normal navigation.
- [x] Run targeted tests, full client tests, typecheck and production build; report any unrelated baseline failures accurately.

## Deployment diagnosis
Read-only production inspection ties the reported 502 responses to refused/reset localhost upstream connections during container replacement around 09:14 UTC on 2026-10-17 (17:14 Asia/Shanghai). Sample resources and feed APIs now respond successfully. This patch does not claim zero-downtime deployment and does not restart or publish production services.

## Verification evidence
- Before the fix, the focused suite failed 12 of 20 tests, exposing the blacklist, stale/missing cover fallback, missing autofill semantics and absent asset recovery UI.
- After the fix and additional preference/recovery coverage, all 23 focused regression tests pass. The full client suite passes all 130 tests across 9 files.
- `npm --prefix client run build` passed, including vue-tsc. Existing dependency PURE annotation and large bundle warnings remain non-fatal.
- In the browser, the local client used the public feed API through the Vite proxy. The first five cover images decoded successfully (naturalWidth 1368); the pre-fix production page contained zero cover image elements.
- Mobile viewport checked at 630 x 1091; document clientWidth and scrollWidth both measured 625, with no horizontal overflow. Registration and password-reset code fields expose one-time-code autocomplete and numeric input mode. No warning/error logs appeared during these checks. No registration, email-code sending or password reset was submitted.
- Screenshots are local QA artifacts under `docs/verification/2026-10-17-images-fixed*.png` (ignored by Git).
- Production was inspected read-only; no deployment, restart, database update or git push was performed.
- A concurrent, unrelated trailing-newline change in `client/src/pages/login.vue` was observed and left untouched.
