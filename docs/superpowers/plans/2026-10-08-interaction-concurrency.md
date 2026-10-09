# Interaction concurrency implementation plan — 2026-10-08

Goal: Preserve real toggle semantics while making like/collect facts and counters atomic, and defer every Redis effect to the outer transaction commit.

Scope: LikeServiceImpl, CollectServiceImpl; additive ArticleMapper methods only (preserve existing review/locking SQL); narrow ArticleLikeMapper, CollectMapper, CommentMapper methods; isolated new tests. No ArticleServiceImpl, frontend, real database, deployment, reset or commits.

Root cause: fact reads precede any target lock, writes ignore affected rows, delta counters can drift, enum indexing throws on invalid inputs, and Redis effects occur before the encompassing transaction commits.

Architecture: Lock the authoritative article first (also parent article before comment), validate existing article detail access rules under the lock, then lock/read the interaction fact. Require exactly one fact write and counter update. Derive counters from a current locking fact read, including recovery of historical drift. Register independently guarded invalidations in afterCommit (delete, rather than publish potentially reordered stale snapshots). Preserve PUBLIC/FOLLOWERS/MUTUAL/PRIVATE, author/admin access, status 0–4 and review_state unchanged. Comment parent access is validated; no new comment moderation/public policy invented.

Tech: Java, Spring transaction proxy/TransactionTemplate, MyBatis-Plus, isolated H2 MODE=MySQL, mocked Redis only.

- [x] Add failing isolated SQL/transaction tests: invalid inputs; simultaneous same-user toggles from absent/present facts; different-user counters; comment likes; zero/failing fact/counter writes; dirty counter repair; outer rollback; post-commit independent Redis failures; access rules.
- [x] Capture red Maven log before production edits.
- [x] Add narrow locking/current-read mapper methods, validate inputs/access, enforce successful writes, reconcile counters under target lock.
- [x] Move effects to independently guarded outer-commit callbacks.
- [x] Run focused then full server tests, record commands, exact counts, logs, and edited paths. Parent independently reviews.

Caveats: H2 proves transaction boundaries and lock serialization in an isolated database, not a production MySQL benchmark. Existing non-toggle writers outside this scope are not redesigned. Target lock is coarse per article by design to serialize both counts and permission/state transitions; current fact reads avoid pre-existing outer-transaction snapshots.


## Verification evidence (2026-10-08, Asia/Shanghai)

Working directory: `D:\zhixun`. All Maven commands used only the isolated test database and mocked Redis; no production database, frontend, commit/push/deploy, or remote AI request was made.

| Stage | Command | Result | Exact log |
| --- | --- | --- | --- |
| Red before production edits | `mvn -f server/pom.xml -Dtest=InteractionConcurrencyTest test` | 34 tests; 25 failures; 3 errors; 0 skipped; exit 1; duplicate-key races reproduced in article likes, comment likes, and collects | `D:\zhixun\server\target\interaction-red.log` |
| Initial green | same focused command | 34 tests; 0 failures/errors/skipped; exit 0 | `D:\zhixun\server\target\interaction-green-1.log` |
| Expanded focused suite | same focused command | 63 tests; 0 failures/errors/skipped; exit 0; finished 22:48:04 +08:00 | `D:\zhixun\server\target\interaction-focused.log` |
| Full server | `mvn -f server/pom.xml test` | 232 tests; 0 failures/errors/skipped; exit 0; finished 22:48:59 +08:00 | `D:\zhixun\server\target\interaction-full-server.log` |

Expanded-suite fixture repair: the initial optional-Redis test passed a generic AopTestUtils result directly to an overloaded ReflectionTestUtils method, causing a ClassCastException in the test. Assigning it to Object selected the intended overload; no production change was needed. The focused log above contains the subsequent successful rerun.

Counter invariants tested independently for article likes, comment likes, and collects:
- Two concurrent same-user toggles from absent: facts=0, counter=0, both requests succeed.
- Two concurrent same-user toggles from present: facts=1, counter=1, both requests succeed.
- Six different users concurrently enable: facts=6, counter=6; then concurrently disable: facts=0, counter=0.
- Negative historical count (-5) is repaired to 1 on enable and 0 on disable from the actual locked facts.
- Zero or throwing fact/counter insert/delete paths leave the original facts/counter untouched and trigger no Redis operations.
- Outer rollback: facts=0, counter=0, no Redis operations; outer commit: facts=1, counter=1, deferred invalidations run.
- Failed status/detail/scan effects do not prevent independent count/related/list/rank/feed/version effects, and do not undo committed facts.
- Public status 0–4, author/admin access, private/follower/mutual visibility, deleted targets, and unchanged review_state are exercised.

### Exact authored paths

- `D:\zhixun\server\src\main\java\com\zhixun\service\impl\LikeServiceImpl.java`
- `D:\zhixun\server\src\main\java\com\zhixun\service\impl\CollectServiceImpl.java`
- `D:\zhixun\server\src\main\java\com\zhixun\mapper\ArticleMapper.java` — only saveLikeCount, saveCollectCount, countInteractionFollow added by this worker; the pre-existing selectForUpdate/saveReviewDecision/publishScheduledIfEligible SQL remains unchanged.
- `D:\zhixun\server\src\main\java\com\zhixun\mapper\ArticleLikeMapper.java`
- `D:\zhixun\server\src\main\java\com\zhixun\mapper\CollectMapper.java`
- `D:\zhixun\server\src\main\java\com\zhixun\mapper\CommentMapper.java`
- `D:\zhixun\server\src\test\java\com\zhixun\service\impl\InteractionConcurrencyTest.java`
- `D:\zhixun\docs\superpowers\plans\2026-10-08-interaction-concurrency.md`

### Parent review notes

No ArticleServiceImpl edit was necessary. ArticleMapper already had uncommitted review/locking methods on arrival; do not attribute or revert those changes as part of this worker. Services now inject the existing SecurityUtil to preserve the existing admin exception rather than introducing public-only interaction rules. groupName behavior remains unchanged. The original implementation loaded locked fact IDs in Java; this is superseded by the scalar-count follow-up below. Exact repair/current-read intent remains unchanged, without adding a new storage abstraction or schema migration. H2 does not validate every MySQL isolation/index-gap behavior or performance characteristic; a real DB experiment was explicitly forbidden. Independent parent review/verification remains pending.


## Scalar count performance follow-up — parent review

Goal: eliminate the per-toggle transfer/materialization of every fact ID in Java while keeping exact online reconciliation and CURRENT reads under an already-established outer repeatable-read snapshot.

- [x] Add bounded popular-target fixtures requiring a primitive scalar mapper result, plus actual separate-connection transaction ordering with a stale plain-count negative control.
- [x] Capture failure against the existing List<Long> implementation before changing production code.
- [x] Replace only the two fact-count mapper methods and the two service call sites with a locked-derived scalar count. Keep target locks, exact counter reconciliation, affected-row checks, cache semantics and durability unchanged.
- [x] Execute stale-snapshot and zero-counter-write rollback tests without relaxing truth assertions; rerun focused and full server suites. The H2 insertion current-read failures remain unresolved, not green.
- [x] Record SQL rationale, exact follow-up files/logs/counts and remaining target MySQL verification.


### Scalar SQL and primary vendor rationale

The like mapper now returns primitive `long`:

```sql
SELECT /*+ NO_MERGE(locked_facts) */ COUNT(*)
FROM (
  SELECT id FROM cms_like
  WHERE target_id = ? AND target_type = ? FOR UPDATE
) AS locked_facts;
```

Collect uses the same shape with `cms_collect WHERE article_id = ?`. The inner locking clause is intentional, not just an outer aggregate lock; `NO_MERGE` is a MySQL 8 optimizer hint to keep the locking query block separate from aggregation. The authoritative target lock and counter affected-row checks are unchanged. This removes O(N) JDBC result rows, MyBatis Long allocations, list storage, and Java/network transfer per reconciliation: one scalar result crosses the mapper boundary. Exact count still requires O(N) work in the database over matching facts; derived materialization can use database temporary storage. It is NOT claimed to be an O(1) database operation or a measured production benchmark. Existing schema has `(target_id,target_type)` and `(article_id)` interaction indexes; the target MySQL plan and hint acceptance still need verification.

Primary vendor pages retrieved directly on 2026-10-08 (web search/open returned no usable source text, so direct HTTPS retrieval was used):
- MySQL 8 locking reads: https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html — documents latest-data reads for SELECT FOR UPDATE and the requirement for an explicit locking clause in nested query blocks.
- Derived table optimization: https://dev.mysql.com/doc/refman/8.0/en/derived-table-optimization.html — documents NO_MERGE as a way to prevent merging of a derived query block.
- Optimizer hints: https://dev.mysql.com/doc/refman/8.0/en/optimizer-hints.html — documents the table-level MERGE/NO_MERGE hint syntax.
- H2 transaction isolation: https://h2database.com/html/advanced.html#transaction_isolation — H2 is not a substitute for testing InnoDB's implementation. Actual fixture observations below take precedence over assuming cross-vendor equivalence.

### Actual H2 RR ordering: do not confuse rollback with current-read success

All new ordering tests use real Spring transactions on separate H2 connections/threads, not stubbed count results. The worker Future completes only after its proxied toggle transaction commits. Outer transaction isolation is REPEATABLE_READ.

1. Established snapshot, concurrent deletion: ordinary count remains 1 after the other transaction removes the fact and commits (actual committed truth=0). The locking-derived scalar raises H2 SQLSTATE 40001 rather than returning current 0. The explicit H2 test checks that rejection/rollback, committed facts=0 and counter=0, no outer cache effects, then exact recovery in a fresh transaction. The initial candidate success assertion failed; it was not represented as successful MySQL proof.
2. Established snapshot, concurrent insertion: ordinary count remains 1 after the other user adds a fact and commits (actual committed truth=2). H2's locking-derived scalar ALSO returns stale 1. The regression still requires scalar=2 and uses assertAll to also exercise the real proxied outer toggle: its response and authoritative counter are 2 where the truthful requirement is 3. All three truth assertions remain failures; their expectations were NOT changed to make H2 green. This is a known failing test and blocks signoff under the requested H2 proof.
3. Bounded 256-fact popular-target tests require a primitive scalar mapper result, exclude another target's record, and keep exact toggle counts 257 then 256.
4. Historical drift zero-counter-write tests preserve original facts=1, old counter=42, and no Redis effects when the counter write reports zero; the next successful toggle reconciles facts=2/counter=2. Existing same-user/different-user/rollback/failed-write checks remain intact.

The target runtime is MySQL 8.0 (`D:\zhixun\docker-compose.yml`); H2-specific snapshot reset, isolation changes, mapper emulation, transaction retry/redesign, desired-state semantics, and cache/offline reconciliation were NOT added to force these tests green.

### Follow-up exact files (only these six were edited in this review round)

- `D:\zhixun\server\src\main\java\com\zhixun\mapper\ArticleLikeMapper.java`
- `D:\zhixun\server\src\main\java\com\zhixun\mapper\CollectMapper.java`
- `D:\zhixun\server\src\main\java\com\zhixun\service\impl\LikeServiceImpl.java` — scalar count call site only.
- `D:\zhixun\server\src\main\java\com\zhixun\service\impl\CollectServiceImpl.java` — scalar count call site only.
- `D:\zhixun\server\src\test\java\com\zhixun\service\impl\InteractionConcurrencyTest.java`
- `D:\zhixun\docs\superpowers\plans\2026-10-08-interaction-concurrency.md`

ArticleMapper, CommentMapper and ArticleServiceImpl were not edited in this follow-up. Prior dirty review/status SQL and all unrelated worker changes remain untouched.

### Target MySQL remaining verification (requires approval for an isolated MySQL fixture)

No real database was contacted. Before parent signoff, replay both insertion/deletion orderings on isolated target-version MySQL 8/InnoDB with RR: A starts and establishes a plain fact snapshot, B performs and commits its proxied toggle, A locks the target row, the scalar must see current truth (2 for insertion / 0 for deletion), and A's subsequent toggle must leave the authoritative counter exactly equal to committed facts. Also rerun zero/throwing counter writes and outer rollback, inspect EXPLAIN/SHOW WARNINGS for the NO_MERGE hint, confirm target indexes and bounded one-row JDBC results, and assess derived-temp work for a representative popular target. Do not treat the H2 rejection case or documentation alone as this runtime verification. No limits or weaker truth assertions should substitute for it.


### Follow-up verification logs and status

- Before production edits: `mvn -f server/pom.xml -Dtest=InteractionConcurrencyTest#reconciliationReturnsOneScalarForBoundedPopularTargetFixture+currentReconciliationSeesCommittedToggleAfterOuterRepeatableReadSnapshot test` — 6 tests / 6 failures / 0 errors / 0 skipped, exit 1; scalar mapper boundary missing. Log: `D:\zhixun\server\target\interaction-scalar-red.log`.
- Initial scalar candidate: same selected test command — 6 tests / 3 failures / 0 errors / 0 skipped, exit 1; bounded scalar tests pass, deletion snapshot success assumption fails with H2 SQLSTATE 40001. Log: `D:\zhixun\server\target\interaction-scalar-green.log` (historical filename; despite its name this run FAILED).
- Isolated insertion stale-snapshot probe: `mvn -f server/pom.xml -Dtest=InteractionConcurrencyTest#outerRepeatableReadBeforeAnotherUserInsertReconcilesCommittedFacts test` — 3 tests / 3 failures / 0 errors / 0 skipped, exit 1, scalar expected 2 / observed 1. Log: `D:\zhixun\server\target\interaction-scalar-insertion.log`.
- Final focused command: `mvn -f server/pom.xml -Dtest=InteractionConcurrencyTest test`. 75 tests / 3 failures / 0 errors / 0 skipped; exit 1; finished 2026-10-08 23:10:25 +08:00. All three failures are the retained insertion RR regression, one per target kind. Log: `D:\zhixun\server\target\interaction-scalar-focused-final.log`.
- Final full-server command: `mvn -f server/pom.xml test`. 244 tests / 3 failures / 0 errors / 0 skipped; exit 1; finished 2026-10-08 23:10:58 +08:00. Same three failures; remaining 241 tests passed. Log: `D:\zhixun\server\target\interaction-scalar-full-server.log`.

Status: NOT ready for parent signoff. The scalar mapper shape/performance boundary has been changed, but current-read equivalence on an already-established H2 fact snapshot is not achieved. The failing insertion regression is retained, no isolation downgrade or simulated read is used, and no truthful expected counter is reduced. Approval for a disposable isolated target-MySQL fixture (never production) or explicit parent direction is needed before claiming target runtime verification; no such database was started or contacted in this round.


## Disposable target-engine authorization: environment check — 2026-10-09

The user authorized a fresh, uniquely owned local MySQL 8 fixture only if Docker CLI/daemon and an available image are usable. Production/existing databases and containers, real credentials, host business volumes/config/.env, installing/starting Docker Desktop, and global configuration changes remain prohibited.

Read-only checks:
- Docker CLI found: `D:\Docker\resources\bin\docker.exe`, client version 29.7.2, configured context `desktop-linux`.
- `docker version --format '{{json .}}'`: exit 1; client available, Server=null. Connection fails because named pipe `//./pipe/dockerDesktopLinuxEngine` does not exist.
- `docker image ls --format '{{.Repository}}:{{.Tag}} {{.ID}}' mysql`: exit 1 for the same missing daemon pipe. MySQL 8 image availability is UNKNOWN, not confirmed absent.
- Exact captured output/exit codes and local observation time: `D:\zhixun\server\target\interaction-mysql-environment-check.log`.

No Docker Desktop start/install, context change, image pull, existing-container/database query, fixture container creation, readiness polling, restart, or cleanup command was performed. Owned container ID=NONE; owned label=NONE; no fixture resources exist to clean up. No actual MySQL mapper/proxy test ran, and candidate SQL correctness remains UNVERIFIED on the target engine. The prior H2 stale-snapshot truth/counter regression remains intact; it was not skipped, weakened, or treated as MySQL validation.

This turn changes only this plan document (plus the ignored environment-check log under server/target); production mapper/service/test sources are unchanged. A parent rerun requires an already usable Docker daemon and the narrowly authorized disposable fixture; this worker will not start Docker Desktop or redirect verification to any existing database. No isolated-fixture script/profile is claimed to be validated because target-engine execution could not begin.


## Bounded native discovery and H2 locking-shape comparison — 2026-10-09

### Already installed native MySQL found (read-only)

- PATH resolved `mysqld.exe` to `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe`; bounded checks used only known vendor roots and their immediate bin directories, not database data/config/.env or credential files.
- `mysqld --no-defaults --version` returned MySQL Community Server 8.0.45 for Win64; exit 0. File version 8.0.45.0. No server was launched by this version-only command, and no existing server was queried.
- Evidence: `D:\zhixun\server\target\interaction-native-mysql-discovery.log`.

### Proposed native fixture — awaiting approval before initialization/launch

The existing binary makes target-engine verification possible without Docker startup, installation, or an existing database connection. Proposed ownership boundaries:

1. Create a fresh UUID directory only under `D:\zhixun\server\target\interaction-mysql-native-<UUID>` with a fixture-ID marker; record the canonical path and ownership manifest. Initialize a fresh datadir using the installed binary with `--no-defaults`, an explicit basedir, and that owned datadir only. Never use the installed default datadir or option files.
2. Generate new random dummy root/test credentials and use a fixture-owned startup init file to set them before test connections. Credentials are never taken from business files or written to shared source/output. All generated files/logs remain inside the owned fixture directory.
3. Start one hidden child process (`Start-Process -WindowStyle Hidden`) with `--no-defaults`, an explicitly selected random 127.0.0.1 port, loopback binding, MySQL X disabled, and owned pid/error/init paths. Record exact PID, start time, executable and command line/datadir. No Windows service/global setup or configuration changes.
4. Observe initialization for at most 90 seconds and readiness for at most 60 seconds; verify the listening port belongs to the owned PID before any SQL connection. A timeout does not authorize a restart. Do not connect to any other endpoint or existing server.
5. Run test-only MySQL fixture configuration with the actual production mapper interfaces and Spring transaction proxies; independent outer RR snapshot / other committed toggle / current fact-count and subsequent counter-truth assertions for insertion and deletion, plus zero/throwing writes and outer rollback. No production-code seam, isolation downgrade, REQUIRES_NEW, or fake current read is proposed.
6. On terminal success/failure/readiness timeout, stop only the verified owned PID/process, then remove only the validated UUID fixture directory/marker beneath the fixed target root. Record cleanup. No wildcard process kills, service stops, existing-volume operations or cross-shell destructive commands.

No initialization, server-process launch, fixture datadir, native SQL connection, or cleanup occurred in this turn. User approval for this native process proposal is still required.

### H2 comparison: exact original phantom-insertion ordering

New bounded probe: `D:\zhixun\server\src\test\java\com\zhixun\service\impl\InteractionLockingReadProbeTest.java`. It composes the existing isolated fixture (no inherited duplication of the main tests), uses its actual mapper/proxy toggle for the other connection, and compares native SQL shapes without production mapper/service edits.

For every independently initialized case: one fact exists; A enters RR and reads plain COUNT=1; B uses a separate connection and real proxied toggle to insert another user's fact, checks facts=2/counter=2, and commits; Future completion proves B's COMMIT occurred; A's plain COUNT remains 1, then the tested locking SQL executes. A's CURRENT-read expected result remains **2**. After A exits, fresh reads again assert committed facts=2/counter=2 and no outer Redis effects. No sleeps, read simulations, isolation changes, or arbitrary interaction caps.

| Tested shape | Article like | Collect | Comment like |
| --- | --- | --- | --- |
| OLD top-level SELECT id ... FOR UPDATE, native JDBC list size (test only) | 1, expected 2 | 1, expected 2 | 1, expected 2 |
| Current mapper locked-derived scalar COUNT | 1, expected 2 | 1, expected 2 | 1, expected 2 |
| Outer simple target-row SELECT FOR UPDATE with locked-derived scalar count projection | 1, expected 2 | 1, expected 2 | 1, expected 2 |
| Direct aggregate SELECT COUNT(*) ... FOR UPDATE | SQLSTATE 90145 | SQLSTATE 90145 | SQLSTATE 90145 |

Projection example (article likes):

```sql
SELECT (
  SELECT /*+ NO_MERGE(locked_facts) */ COUNT(*)
  FROM (SELECT id FROM cms_like WHERE target_id=10 AND target_type=1 FOR UPDATE) AS locked_facts
) AS fact_count
FROM cms_article WHERE id=10 AND deleted_at IS NULL FOR UPDATE;
```

H2 rejects the direct aggregate locking form as FOR UPDATE not allowed for grouped select. These unsupported paths remain errors, not successful count assertions. The old top-level locking read exhibits the same stale insertion view as the optimization in this exact fixture: the evidence now shows that the stale view is not exclusive to introducing the derived scalar query. It does NOT prove a universal claim about all possible H2 locking reads, or prove any shape's behavior on MySQL.

No tested portable shape established current truth in this RR phantom case. Production SQL is not changed based on these failed probes; the existing 2/3-fact truth/counter assertions remain intact. The overall task remains active and the branch is NOT signed off.

Initial probe command: `mvn -f server/pom.xml -Dtest=InteractionLockingReadProbeTest test` — 12 tests, 9 assertion failures, 3 unsupported-SQL errors, 0 skipped; exit 1; finished 2026-10-09 01:07:23 +08:00. Exact shapes and observations: `D:\zhixun\server\target\interaction-locking-shape-probe.log`.

Combined focused rerun: `mvn -f server/pom.xml -Dtest=InteractionConcurrencyTest,InteractionLockingReadProbeTest test` — 87 tests, 12 failures, 3 errors, 0 skipped, exit 1; finished 2026-10-09 01:09:34 +08:00. The existing 72 passing cases remain passing; its original 3 insertion RR failures remain, plus 9 new stale-read assertions and 3 new unsupported aggregate SQL errors. Log: `D:\zhixun\server\target\interaction-locking-comparison-focused.log`.

### Exact authored files in this turn

- `D:\zhixun\server\src\test\java\com\zhixun\service\impl\InteractionLockingReadProbeTest.java` (new bounded diagnostic tests).
- `D:\zhixun\docs\superpowers\plans\2026-10-08-interaction-concurrency.md` (discovery, proposal and comparison evidence).

Ignored output logs only: native discovery, locking-shape probe, and combined focused log under `D:\zhixun\server\target`. Production services/mappers, the existing InteractionConcurrencyTest assertions, ArticleServiceImpl, frontend, and unrelated dirty files were not edited.


## Approved native fixture attempt — checkpoint, not target-engine signoff

The user approved the UUID-owned installed-MySQL fixture, with 90-second initialization and 60-second readiness budgets that cannot be reset, no timeout restart, and no new status tools after the readiness stop checkpoint.

The combined harness-writing/provision/verification/cleanup command was rejected by the command runner before execution; no file or fixture was created by that attempt. The subsequent narrowed lifecycle steps used only native PowerShell, without any deletion stage. No full reusable harness or vendor-test class is claimed to exist or be validated.

### Owned identity

- Fixture ID: `ee75e2bf-cb9f-40b4-86d2-2e839060cb35`.
- Directory: `D:\zhixun\server\target\interaction-mysql-native-ee75e2bf-cb9f-40b4-86d2-2e839060cb35`.
- Datadir: same directory plus `\data`.
- Binary: `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe`.
- Initialization PID: 30396; start UTC `2026-10-08T17:24:36.251609Z`; elapsed 7,324 ms; exit 0. Fresh data initialized successfully with `--no-defaults --initialize-insecure` and explicit owned datadir/basedir.
- Server PID: 5840; start UTC `2026-10-08T17:25:37.0528122Z` (local +08:00: 2026-10-09 01:25:37.0528122).
- Chosen port: 40895; launch arguments specify 127.0.0.1 binding, MySQL X OFF, skip name resolution, no binary logging, and only owned pid/error/bootstrap paths. Listener ownership was NOT verified, so no SQL connection was made.
- Schema name: `interaction_ee75e2bfcb9f40b486d22e839060cb35` (generated for this fixture). Only generated dummy root/test credentials were written in fixture-owned startup/test files; none were printed or taken from existing configuration.
- Initialization and server were launched using `Start-Process -WindowStyle Hidden`, with no Windows service/global configuration change.

### Readiness failure and stop boundary

The initial readiness loop observed the owned process live, then aborted with a file-sharing exception while opening its owned `mysql.err.log` via ReadAllText. A subsequent artifact/checkpoint command mistakenly converted UTC owner metadata through local DateTime parsing and calculated an invalid deadline (09:26 UTC rather than the recorded 17:26 UTC). That command took its budget-stop branch BEFORE any further process/listener/log read. This was an observer implementation failure, not proof that mysqld itself failed to become ready.

The saved deadline has been corrected as an artifact only using timezone-aware parsing of the already recorded start value. No readiness budget was restarted, and no process/listener/server-log/status/SQL observation occurred after the stop checkpoint. Stage is `ReadinessStoppedUnverified`: last observed live=true; current liveness is NOT freshly verified. No native SQL test ran, no target-engine success is claimed, and no diagnostic auto-discovery/assertion change was made.

### Preserved checkpoint and resources

- Non-secret owner checkpoint: `D:\zhixun\server\target\interaction-mysql-native-owner.json`.
- Same owner record and UUID marker in the exact owned fixture directory (`owner.json`, `owner.marker`).
- The owned process locator (PID + exact start time + binary + datadir + port) and all fixture files are preserved. No cleanup, Stop-Process, restart, recursive removal, or existing-server access was performed after readiness stopped.
- Explicit owner-only cleanup/direction is needed before any further process observation or deletion. Any future recursive removal must resolve this exact directory under `D:\zhixun\server\target`, verify the marker and ownership, and use native PowerShell with LiteralPath; never a wildcard process kill or unchecked computed/cross-shell delete.

Only this plan and ignored fixture/checkpoint artifacts changed in this attempt. Production mapper/service code and every H2 truth/probe assertion remain unchanged. Supported-MySQL mapper/proxy RR insertion/deletion/rollback verification is still NOT achieved; the overall task remains active and the unsafe branch is NOT signed off.
