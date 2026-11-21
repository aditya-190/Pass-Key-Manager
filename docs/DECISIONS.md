# Decisions

| Date | Decision | Why |
|---|---|---|
| 2026-11-21 | Reuse closed PRs #1–#4 (refs/pull/N/head, based on f92cd08), split into smaller releases | They already fix most findings and are tested; they were too big to review and predated the studio baseline |
| 2026-11-21 | The production `PASS_PHRASE` lives only in `local.properties` (git-ignored). Agents never read that file | It is the legacy key for every existing user's vault |
| 2026-11-21 | Free, no ads, no paywall | Trust is the product; "no ads" is the top praise in 5★ reviews |
| 2026-11-21 | Keep the "PassKey" brand; change the Play title to a keyword-led one; move to Productivity | Keeps 34K installs and existing ratings; the brand alone loses to the "passkeys" term |
| 2026-11-21 | Keep Firebase Analytics and Crashlytics, but consent-gated, no ad ID, no PII, no vault content; the listing says "vault never leaves your phone", not "no internet" | Owner wants analytics; honesty in the Data-safety form and the listing |
| 2026-11-21 | Rejected: HIBP breach check and WorkManager auto-backup. Approved: backup-reminder notification and Credential Manager provider | Owner decision on opt-in features |
| 2026-11-21 | Passkey (FIDO) storage planned for 6.1.0 | Makes the name match what searchers expect; answers the "passkeys not working" reviews |
