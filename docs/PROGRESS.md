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
- Physical device (Moto G34, Android 15, 2026-11-23, debug 5.8.0 on a test vault): fingerprint unlock through the
  biometric key; adding a fingerprint invalidated it, PassKey fell back to the screen-lock key, unlocked, and rebuilt
  the fingerprint key (next start used it); removing that fingerprint kept the key valid (Android invalidates only on
  a new enrollment or when all are removed). Device suite 34/34. No crashes.
- Moto G34 autofill in Chrome 157 (2026-11-23): Chrome sends requests to PassKey only after "Autofill using another
  service" is chosen in Chrome's settings; the form then shows only "Unlock to fill · PassKey". Found: Android never
  offers to save a new login (the authentication response carries no SaveInfo) - see 5.8.0 device findings. The
  phone's autofill service, Chrome's setting and stay-on were restored; the debug app and test vault were kept.
- Device findings fixed: A save is offered (SaveInfo on both responses); B shared-hosting suffixes; C Chrome guidance
  dialog (17 languages). Tests red → green; 298 unit, 34 instrumented; +1,000-seed hosting simulation. Emulator
  (API 28): typed login → "Save to PassKey?" → saved as an entry with a secret password → "Unlock to fill" →
  fingerprint → the login fills.
Open: the real Play 5.5.1 → 5.7.0 upgrade needs a Play internal testing track (a debug build cannot replace a
Play-signed install without uninstalling). Rollout set by the owner (100%).

## Phase 4: release 5.8.0 "Daily-use features" (branch `release/5.8.0`), in progress ⏸
Owner: 5.7.0 → 100% rollout (owner sets it in Play Console); physical checks on the Moto G34 (not yet connected).
### 4a: architecture (no user-visible change), code-complete
- Layering fixed, typed PreferencesRepository (`c823935`); domain models, entities confined to data (`ebd0d94`);
  lifecycle-aware collection (`6907fbf`); type-safe navigation for all 10 routes (`1ec2a66`); single-state MVI for
  home (`419b10b`), fields (`708bf85`), Settings (`6889cf8`); per-screen effects, UiEvents deleted (`3a46b15`).
- Tests: NavRouteTest 5, PreviewViewModelTest 10, DetailViewModelTest 7, SettingsViewModelTest 10 (Settings' first).
  227 unit tests pass. Simulation: 2 × 1,000 seeded intent sequences, 0 failures.
- Device (API 28): default suite OK (26), vault untouched; minified release walked through all routes but Onboarding;
  home/details/settings smoke (tabs, swipe → confirm → cancel, generator marks secret, dialogs) with no crash.
- Local fix: the schema folder's case-only rename needed a two-step `mv` on macOS (MigrationTester could not find it).
- Attack pass, owner chose to fix all: #1 double-tap Delete then Undo lost the entry's fields (dialog now closes
  before the write; a second tap is a no-op); #2 double-tap Save made duplicates (editor isSaving guard, both
  screens); #3 a backup picked during another job was dropped with its password unwiped (backups now queue on a
  mutex); #4 saving an edit of a vanished entry dropped it (saved as new). 5 tests red → green; 232 unit tests;
  #1 re-checked on device (double-tap Delete → Undo → entry and field back). #5 (schema folder case) is local only.

### 4b: daily-use features, code-complete
- Autofill (`e171c11`): matcher hardened against look-alike apps and sites (4 phishing tests red → green); unlock screen
  on the 5.7.0 prompt policy. Room 3→4 with TOTP and password history (`6712f0b`); backup carries authenticators
  (account now included, no duplicates on re-import). Entry screen shows codes, confirm-before-remove, history
  (`f5311c5`). 5.8.0 (47), autofill privacy text in 17 languages, AppLanguage single list, SecureModalBottomSheet guard
  on `check` (`1dbdac9`).
- Tests: 276 unit, 31 instrumented (API 28). Simulation: 6,000 seeded matcher/TOTP runs + 30 encrypted backup round
  trips, 0 failures.
- Device (API 28): real vault migrated 3→4 with data intact; locked autofill shows only "Unlock to fill" (no entry
  names); unlock → fingerprint → back to the form; cancel shows a button instead of re-prompting.
- Not taken from PR #4: Robolectric backup test (new dependency), its update-controller tests (code replaced in 5.6.0).
- 4b attack pass, owner chose all fixes: #1 every fill authenticates; #2 save updates the existing password (history
  keeps the old one); #3 trust on first use for apps (Room v5); #4 Undo restores authenticators and history (found
  while planning: entry delete cascaded the codes); #5 issuer/account capped; #6 four autofill/authenticator events.
  13 tests red → green; 289 unit, 34 instrumented. Simulation +3,000 seeds (saves, trust, delete/undo), 0 failures.
  Device: real vault v4→v5 intact; with the vault unlocked the form still shows only "Unlock to fill".

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
- [x] Before 5.7.0: fingerprint checks on a physical device (Moto G34, done 2026-11-23).
- [ ] Before 5.7.0 ships: Play 5.5.1 → 5.7.0 upgrade through an internal testing track, with sample data.
