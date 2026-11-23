# Progress

## Phase 0: research and spec (2026-11-21) ✅
- `docs/research/{BRIEF,COMPETITORS,FEATURE_MATRIX,PAIN_POINTS}.md`, `docs/SPEC.md`, `docs/DECISIONS.md`, app `CLAUDE.md`.

## Phase 1: release 5.5.2 "Correct and honest" (branch `release/5.5.2`) ✅ code and device checks done; release needs the owner actions below
Done:
- PR #1 cherry-picked: localised onboarding without the crash trap, SecureRandom generator, 17-locale parity
  (Arabic in `locales_config`), duplicate-heading and edit-trim fixes, TalkBack labels.
- From PR #2: UI text resolved at the call site (fixes stale language below API 33), sensitive clipboard with 30s
  clear, all content descriptions localised, night theme, FLAG_SECURE pinned on sheets and dialogs, secrets kept out
  of `SavedStateHandle`, reorder in one transaction.
- New: ktlint and detekt gates; consent-gated Analytics, Crashlytics and Performance behind interfaces; no AD_ID;
  consent step plus Settings › Privacy options; splash routes once; password keyboard on the Response field;
  honest About/Privacy/Terms in 17 languages; onboarding no longer promises autofill.
- Tests: 69 unit tests (from 17), including a manifest test that pins every privacy default; simulation of 18,000 seeded cases, 0 failures.
- Gates: `ktlintCheck detekt testDebugUnitTest lintDebug assembleRelease` all green.

### Device run (2026-11-22, emulator `PassKey_API28`, API 28 = minSdk, screen-lock PIN 1234)
- Instrumented: 7/7 pass (launch flow, localisation in 17 locales, 3 migration tests). `VaultSeedTool` is opt-in.
- First-run flow by hand: onboarding → consent (both off) → decline → PIN unlock → home. Pass.
- Upgrade check (`scripts/e2e/upgrade_check.sh`): entry and secret readable in 5.5.2 → real 5.5.1 (versionCode 43,
  built from `master`) → 5.5.2. Pass, so 5.5.2 uses the same vault key as the current Play build.
- Monkey, 3 seeds × 7,000 events: 5522 and 5523 clean; **5521 crashed** after 5,475 events on Settings:
  `IllegalArgumentException: Cannot set 'scaleY' to Infinity`. Not reproducible by replay (timing dependent).
  Root cause: Material3 1.4.0 `BottomSheetScaffold.verticalScaleUp` divides by sheet height; Settings expands the
  scaffold sheet for the language picker, whose only content is a separate-window ModalBottomSheet, so the
  expanded sheet is 0 px tall and an overshooting drag gives Infinity. Pre-existing (same code in 5.5.1).
  **Fixed** (owner chose option A, `582711d`): the language picker now sits at screen level, outside the scaffold sheet.
- Monkey rerun after the fix: 6 seeds (5521 replayed from the same state, 5524–5528) × 7,000 events = 42,000 events,
  0 crashes, 0 ANRs. Vault still opens and decrypts afterwards. Monkey deleted the seeded detail (expected data churn).
- No ANRs. The API 37 emulator is shared with another session, so no results were taken from it.

## Phase 2: release 5.6.0 (branch `release/5.6.0`), in progress ⏸
Done (each gated: ktlint, detekt, unit tests, lint, build; device checks on PassKey_API28):
1. Toolchain `afacb64`: Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, KSP 2.3.14; release APK 28 → 13 MB; release build refuses
   to package without PASS_PHRASE; upgrade check against real 5.5.1 passes.
2. Encrypted backup `b4b114e`: `.pkbak` (AES-256-GCM, PBKDF2 600k), legacy CSV import, allowBackup=false; format logic in
   BackupCodec with 7 restore tests.
3. Auto-lock `ca916ba` + `93c7ef9`: LockPolicy (10 tests), SecurityViewModel prompt rules (7 tests); the lock screen prompts
   on resume and always offers Unlock; Immediately no longer locks a vault in use; vault screens restored from Recents
   after process death now lock (5.5.2 showed a secret there with no unlock). `scripts/e2e/lock_check.sh` 5/5.
4. Backup hardening `f29c93b` (owner chose findings 1+2): decompression capped at 64 MB, PBKDF2 cost capped at 2M.
5. isSecret `3877824` (PR #2 c8ede1e, Room 2→3): analysis works in every locale; a real 5.5.1 vault migrates on device.
6. Secret switch `999524d`: per-field switch drives the keyboard and isSecret (5.5.2 finding #1); fixes PR #2's
   never-reset wasGenerated flag.
7. isSecret in backups `721795c` (finding #1).
8. Studio baseline `b2ba381`: Remote Config, force update + maintenance overlays (UpdateHost), announcement banner,
   About / Terms / Privacy / Licences screens replacing the Settings sheet; 52 strings × 17 languages.
9. UX fixes `b204f5b`: labelled Add buttons + empty-state CTA, back buttons, confirm-then-undo delete, inline save errors
   that keep typed input; fixed an unlabeled-FAB TalkBack bug found on device.
Unit tests: 179, all passing. Instrumented: 10/10 on API 28. All gates green incl. release build.

10. Remote policy `0cc129f`: maintenance mode ignored for an offline vault; announcement routes allowlisted.
11. Release prep `31c747c`: version 5.6.0 (45); empty home shows illustration + text only (owner); Privacy Policy.md updated.

### 5.6.0 health report (2026-11-23)
| Area | Status | Evidence |
|---|---|---|
| Unit tests | 🟢 | 190 passing (17 at the start of 5.5.2) |
| Integration / E2E | 🟢 | 10/10 instrumented on API 28; lock_check 5/5; real Play 5.5.1 vault migrates (schema 2→3) and decrypts |
| Simulation | 🟢 | 17,370 seeded cases (Release560SimulationTest) + 18,000 from 5.5.2 |
| Monkey | 🟠 | Skipped by owner partway: seed 5601 stopped at 243 events by a launcher ANR (not PassKey), 5602 partial; 0 PassKey crashes |
| Security | 🟠 | Open Blocker: shared build-time DB key (scheduled 5.7.0). Accepted Lows: keyword false positives, Home during PIN prompt, Undo lost on lock |
| Lint / ktlint / detekt | 🟢 | All clean; 16 pre-existing detekt findings baselined |
| Performance | 🟠 | Not profiled yet (6.0.0 adds baseline profile + macrobenchmark) |
| Size | 🟢 | Release APK 14 MB (28 MB in 5.5.1) |
**Overall: 🟠 shippable** once the owner actions below are done; the DB-key Blocker is 5.7.0's job.


## Phase 3: release 5.7.0 "Per-install key" (branch `release/5.7.0`), in progress ⏸
Owner decisions: PBKDF2 recovery slot (no argon2kt), mandatory one-time recovery password.
- PR #3 ported (`e9e1a4a`, `f4b6c66`) and integrated with the 5.6.0 lock work (`847c88a`): UnlockPromptPolicy (6 tests)
  restores prompt-on-resume / no-loop rules the PR's gate had lost; PBKDF2 replaces Argon2 with reference-vector tests.
- **Blocker found and fixed** (`7eeb92a`): Android 9/10 crashed on every unlock of a re-keyed vault
  (DEVICE_CREDENTIAL alone is rejected below API 30). CredentialAuthenticators (3 tests) + a crash guard.
- Device verification on API 28 (`8a5c2fc`): legacy vault → re-key → PIN unlock ×2; lock removed → recovery password
  (wrong refused, right opens); lock re-added → slots rebuilt → PIN alone; fingerprint enrolled → BIO slot → fingerprint
  unlock through CryptoObject. Default device suite OK (26), vault untouched.
- PR #3's destructive device tests made opt-in; running them was blocked by the safety classifier (deletes app data).
- Attack pass, owner chose #1 and #2: recovery password now 10+ characters, not "weak", with a strength meter
  (PasswordStrength + RecoveryPasswordTest, 5 tests, red → green); upgrade-funnel events recovery_setup_shown /
  recovery_setup_completed / vault_migration_failed. New strings translated into 16 locales (machine, pending review).
- Destructive VaultMigrationTest / KeyStoreWrapperTest skipped by owner decision (they stay opt-in).
- Simulation: 5,000 seeded random passwords (all scripts, empty, 10k chars) + 5,000 repeat/run cases: 0 failures;
  10k-character value measured in under 1 ms. Gates green: ktlint, detekt, 200 unit tests, lint, release build.
Open: invalidation on enrollment change (needs a physical device), staged rollout.

## Carried into 5.6.0
- Per-field secret keyboard (attack-pass finding #1), built on the `isSecret` column.
- A failed Android 10+ export is reported as success (`SettingsViewModel.kt:260`); replaced by the encrypted backup.

## Owner actions for 5.6.0
- [ ] Play Data-safety form: crash logs, diagnostics and app interactions are optional (consent); Remote Config request
      with installation ID; no ad ID; nothing shared; vault content never collected; data encrypted in transit.
- [ ] Native-speaker review of the machine-translated legal and About texts (17 languages).
- [ ] Staged rollout (e.g. 10% → 50% → 100%) watching Crashlytics for users who opted in.

## Owner actions
- [x] 5.5.2 attack pass: fixed #2 (keep pre-consent crash reports until the user decides) and #3 (no automatic screen reporting). #1 (per-field secret keyboard) moved to 5.6.0.
- [x] Emulators created (`PassKey_API28`, `PassKey_API37`); device run done (see above).
- [x] Settings sheet crash: option A applied and verified.
- [ ] Confirm `PASS_PHRASE` is set in `local.properties` before building the release (agents never read it).
- [ ] Update the Play Data-safety form for 5.5.2: optional crash logs, app interactions and diagnostics, no ad ID,
      nothing shared, vault content never collected.
- [ ] Get a native-speaker review of the machine-translated legal texts (at least hi, es, pt, de, fr).
- [ ] Share Play Console retention (D1/D7/D30), uninstall data and vitals so the success metrics in `SPEC.md` have a baseline.
- [ ] Before 5.7.0: a physical device with a fingerprint enrolled, plus the Play 5.5.1 build installed with sample data.
