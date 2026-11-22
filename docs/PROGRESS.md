# Progress

## Phase 0: research and spec (2026-11-21) ✅
- `docs/research/{BRIEF,COMPETITORS,FEATURE_MATRIX,PAIN_POINTS}.md`, `docs/SPEC.md`, `docs/DECISIONS.md`, app `CLAUDE.md`.

## Phase 1: release 5.5.2 "Correct and honest" (branch `release/5.5.2`), code complete, waiting on owner ⏸
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

Not done: device E2E and `adb monkey`, because no emulator or device is available on this machine.

## Carried into 5.6.0
- Per-field secret keyboard (attack-pass finding #1), built on the `isSecret` column.
- A failed Android 10+ export is reported as success (`SettingsViewModel.kt:260`); replaced by the encrypted backup.

## Owner actions
- [x] 5.5.2 attack pass: fixed #2 (keep pre-consent crash reports until the user decides) and #3 (no automatic screen reporting). #1 (per-field secret keyboard) moved to 5.6.0.
- [ ] Connect a device or create an emulator so on-device E2E can run, including an upgrade from the Play 5.5.1 build with a real vault.
- [ ] Confirm `PASS_PHRASE` is set in `local.properties` before building the release (agents never read it).
- [ ] Update the Play Data-safety form for 5.5.2: optional crash logs, app interactions and diagnostics, no ad ID,
      nothing shared, vault content never collected.
- [ ] Get a native-speaker review of the machine-translated legal texts (at least hi, es, pt, de, fr).
- [ ] Share Play Console retention (D1/D7/D30), uninstall data and vitals so the success metrics in `SPEC.md` have a baseline.
- [ ] Before 5.7.0: a physical device with a fingerprint enrolled, plus the Play 5.5.1 build installed with sample data.
