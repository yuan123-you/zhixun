# Chat Media Reliability Review and Release Plan

**Goal:** Audit and fix the reported spurious chat requests and directly related resource-loading failures, then push and deploy only the client to the existing production site.

**Scope:** ChatBubble, imageCache, resource URL resolution, chat media parsing and file preview lifecycle. Preserve existing public URLs, MinIO proxy formats, backend authentication, data and unrelated services. Findings outside this scope are reported rather than silently refactored.

## Verification-driven tasks
- [x] Retain the regression for text/AI/file/voice messages accidentally fetched as images.
- [x] Reproduce image-cache source-change races, invalid response caching and permanent failed-fetch memoization. Apply minimal fixes after red tests; preserve cache hits and request deduplication.
- [x] Reproduce invalid MinIO console and blob URL handling; verify chat helpers do not undo resolver rejection or fetch malformed JSON as resources.
- [x] Reproduce file-preview stale text responses and listener lifecycle leaks; fix only demonstrated failures.
- [x] Run full client tests/build, adjacent admin/config tests and tracked-secret hygiene checks; review the final diff.
- [x] Push the verified release to origin/master by fast-forward only after checking for intervening upstream commits.
- [x] Verify the current production checkout/site and preserve rollback image; build/restart only the client, then check health, public HTML/assets/API and deployed revision. Do not run the broad all-services update script.

## Constraints
- Tests before production edits for every confirmed new bug.
- Never print credentials, commit local secrets, reset remote work, delete data volumes, or restart unrelated applications.
- Record exact validation evidence and unresolved limitations before claiming release success.

## Pre-release evidence (2026-10-17, Asia/Shanghai)
- Client: 214 tests in 18 files pass; production build (vue-tsc + Vite) passes.
- Backend: fresh Maven test reports contain 253 tests, 0 failures, 0 errors, 12 skipped; 241 executed tests pass. The skips require additional database conditions and are not claimed as covered.
- Admin: production build (vue-tsc + Vite) passes. Its configured test suite has no test files, and there is no typecheck script; the build performs its typecheck.
- Tracked-secret hygiene: all 9 tests pass.
- Production configuration: 4/5 tests pass. The existing CORS assertion expects only the glint hostname but the unchanged HEAD configuration also allows the source IP. This mismatch predates this release and is not changed in a chat-only patch.
- Browser QA uses the actual Vue components, not stubs: original text/AI inputs cause zero resource fetches; the valid inline image renders; malformed voice/file/image resources do not load; TXT preview loads successfully and closes. No warning/error browser logs were observed in this harness.
- Confirmed production is the documented Oracle host, with zhixun.iflare.cn as an enabled site alias. Its checkout is clean at cffe41f before deployment. Only the client image/container is in release scope.
- Known baseline warnings: dependency PURE comments, large frontend chunks, and duplicate host Nginx TLS protocol options on unrelated sites. None is suppressed or claimed fixed.
- Authenticated live-chat end-to-end testing was not performed: the browser session is logged out. The original request bug is covered through the real component/cache regression tests and a browser harness without sending production messages.

## Release evidence
- Application commit: 226016d911a204c5138d2adf19516ae2c1e203b4, pushed to origin/master and built on production.
- Client deployment completed on 2026-10-17 at 22:24 Asia/Shanghai. Running image: zhixun-production-client:226016d; health: healthy.
- Before/after container IDs show only zhixun-prod-client was replaced. Backend, admin, infrastructure and unrelated sites retained their original container IDs.
- Rollback image retained as zhixun-production-client:rollback-before-226016d (previous running image: cffe41f).
- Release logs and container snapshots: /srv/zhixun/deployment/releases/226016d-20261017T142358Z-{build.log,before.txt,after.txt}.
- Public /zhixun/ returns 200 with no-cache/no-store HTML. All five entry/preload/style assets and the dynamically imported VoiceRecordingBar-BtuOdhHT.js chat chunk return 200; their SHA-256 hashes match files inside the new running image.
- Public categories, recommended feed and hot feed return HTTP 200 with business code 200.
- Post-release browser verification: homepage renders, the first six article/avatar images decode successfully, group navigation renders the logged-out group page and correctly requires login for protected access, and no warning/error console logs were observed.
- QA screenshot: docs/verification/2026-10-17-chat-media-qa.png; public homepage screenshot: docs/verification/2026-10-17-chat-media-production.png (local ignored artifacts).
- This final documentation-only commit records deployment; it does not change the application image or require any further container restart.
