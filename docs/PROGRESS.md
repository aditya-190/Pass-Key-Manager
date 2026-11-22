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
Unit tests: 121, all passing. Instrumented: 10/10 on API 28.

Next: studio baseline (Remote Config, force update,
banner, About/Legal, LaunchCoordinator), UX fixes (visible Add, back buttons, undo delete, keep typed input), then the
5.6.0 privacy text rewrite + translations and the version bump.

## Carried into 5.6.0
- Per-field secret keyboard (attack-pass finding #1), built on the `isSecret` column.
- A failed Android 10+ export is reported as success (`SettingsViewModel.kt:260`); replaced by the encrypted backup.

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
