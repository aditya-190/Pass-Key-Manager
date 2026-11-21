# Progress

## Phase 0: research and spec (2026-11-21) ✅
- `docs/research/{BRIEF,COMPETITORS,FEATURE_MATRIX,PAIN_POINTS}.md`, `docs/SPEC.md`, `docs/DECISIONS.md`, app `CLAUDE.md`.
- Audits of code, security, UI/UX and accessibility done; findings are folded into the release plan in `SPEC.md`.

## Next: Phase 1, release 5.5.2 "Correct and honest"
Not started.

## Owner actions
- [ ] Share Play Console retention (D1/D7/D30), uninstall data, Android vitals (crash and ANR), and Firebase Analytics,
      either as an export or by letting Claude read them in Chrome. This baselines the success metrics in `SPEC.md`.
- [ ] Before 5.7.0: a physical device with a fingerprint enrolled, plus the Play 5.5.1 build installed with sample data.
- [ ] Confirm `PASS_PHRASE` is present in `local.properties` before any release build (agents never read it).
- [ ] Update the Play Data-safety form now. It currently says "no data collected", but Firebase ships in 5.5.1.
