# PassKey: revamp spec (v5.5.2 → v6.0.0)

## Positioning
**The offline password vault that's actually pleasant to use: autofill, 2FA codes and encrypted backups, free with no ads,
and your vault never leaves your phone.**

## Principles
- Safety before features, and features before polish (owner decision, 2026-11-21).
- No ads and no paywall. The whole app stays free.
- The vault never leaves the device. Analytics and crash reports are opt-in, contain no PII, and never touch vault content.
- No network features beyond Firebase, Play in-app update and in-app review. The breach check was **rejected**.
- Every release is small, reviewable and shippable on its own. Data-touching releases ship alone, on a staged rollout.
- **No UI redesign (owner decision, 2026-11-23).** The current look stays; users chose it for its simplicity. New
  features arrive only as Settings rows, sheet options or pager pages in the existing style, and must be safe on update.
- As few Play updates as possible: one per milestone (5.8.0 carried 5.5.2–5.8.0).

## Release plan
| Release | Theme | Source | Risk |
|---|---|---|---|
| 5.5.2 | Correct and honest: locale fixes, SecureRandom generator, sensitive clipboard, `allowBackup=false`, honest privacy text, consent-gated analytics and crash reporting, no AD_ID, detekt/ktlint | PR #1 + a slice of PR #2 + studio-baseline | Low |
| 5.6.0 | Hardened plus studio baseline: toolchain, encrypted `.pkbak` backup, auto-lock, lock on process-death restore, Remote Config, force update, banner, About/Legal, visible Add button, undo delete, input kept on errors | rest of PR #2 + template | Medium |
| 5.7.0 | Per-install vault key (BIO/CRED/REC slots), safe migration, rescue export | PR #3 | **High**: ships alone, staged |
| 5.8.0 | 4a MVI and type-safe navigation (no UI change); 4b AutofillService, TOTP, password history | PR #4 | Medium |
| 6.0.0 | **Same UI, more features (re-scoped 2026-11-23):** optional feature tour (first run + Settings), verify-backup and an in-app backup reminder, passphrase generator, search by field name, app shortcuts, old-password check, theme mode, smart in-app review prompt, Share PassKey, Credential Manager provider | new | Medium |
| 6.1.0 | Passkey (FIDO) storage through Credential Manager | new | **Parked** (owner, 2026-11-23): high risk |

## Screens
Unchanged (owner decision, 2026-11-23): Splash → Onboarding (3 intro pages + 6 feature-tour pages, Skip on each) →
Consent → Recovery setup → Unlock → Home (category list, search, Add) → Entry detail. Settings gains rows only
(Feature tour; later Verify backup, Import from another app, Theme, Share PassKey).

## Data model
- `preview`, `details` (`isSecret` 5.6.0, `previewId` index 5.8.0), `totp` and `detail_history` (5.8.0),
  `autofill_trust_table` (Room v5, 5.8.0).
- 6.0.0 needs no Room change; new state lives in DataStore (last backup time, review-prompt pacing, theme mode).
- Any later migration is additive and covered by a `MigrationTestHelper` test, with a rescue backup taken before it runs.

## Offline behaviour
Everything works offline. Remote Config falls back to its in-code defaults. Analytics events are queued by Firebase or dropped.

## Monetisation
None. Free, with no ads (owner decision). Revisit only with the owner, and never by gating security features.

## ASO
- Done (5.8.0 submission): title `PassKey: Offline Password Safe`, the new short and full description (en-GB),
  category Productivity.
- Next: listings localised in all 17 app languages; 8 benefit screenshots **of the current UI** with localised
  captions and a feature graphic; reply to every 1–2★ review; a smart in-app review prompt (6.0.0); a store listing
  experiment once the new graphics exist. Never "passkeys" until FIDO storage ships.

## Success metrics (need Play Console and Firebase access to baseline them)
- Rating from 4.16 to 4.5 or more. 1★ share from 13% to under 5%.
- D1/D7/D30 retention: baseline TBD, aiming for +50% D7.
- Weekly active users using autofill or TOTP (the daily-use signal).
- Crash-free users at 99.5% or more. ANR rate under 0.2%.
- Share of users with a backup made in the last 30 days.
