# PassKey: revamp spec (v5.5.2 → v6.1.0)

## Positioning
**The offline password vault that's actually pleasant to use: autofill, 2FA codes and encrypted backups, free with no ads,
and your vault never leaves your phone.**

## Principles
- Safety before features, and features before polish (owner decision, 2026-11-21).
- No ads and no paywall. The whole app stays free.
- The vault never leaves the device. Analytics and crash reports are opt-in, contain no PII, and never touch vault content.
- No network features beyond Firebase, Play in-app update and in-app review. The breach check was **rejected**.
- Every release is small, reviewable and shippable on its own. Data-touching releases ship alone, on a staged rollout.

## Release plan
| Release | Theme | Source | Risk |
|---|---|---|---|
| 5.5.2 | Correct and honest: locale fixes, SecureRandom generator, sensitive clipboard, `allowBackup=false`, honest privacy text, consent-gated analytics and crash reporting, no AD_ID, detekt/ktlint | PR #1 + a slice of PR #2 + studio-baseline | Low |
| 5.6.0 | Hardened plus studio baseline: toolchain, encrypted `.pkbak` backup, auto-lock, lock on process-death restore, Remote Config, force update, banner, About/Legal, visible Add button, undo delete, input kept on errors | rest of PR #2 + template | Medium |
| 5.7.0 | Per-install vault key (BIO/CRED/REC slots), safe migration, rescue export | PR #3 | **High**: ships alone, staged |
| 5.8.0 | 4a MVI and type-safe navigation (no UI change); 4b AutofillService, TOTP, password history | PR #4 | Medium |
| 6.0.0 | Redesign (palette, icon, type, components), entry templates, global search, favourites, generator tab, security dashboard, backup confidence, CSV import from other managers, recycle bin, Glance widget and QS tile, Credential Manager provider, accessibility and performance pass | new | Medium |
| 6.1.0 | Passkey (FIDO) storage through Credential Manager | new | High: own spec plus security audit |

## Screens (target, 6.0.0)
Splash (system API only) → Onboarding (3 pages + consent) → Recovery setup (5.7.0+) → Unlock →
Home [tabs: Vault · Generator · Health · Settings]. From Vault: Entry detail (fields, TOTP, history), Add/Edit (template picker).
From Settings: Backup & restore, Import, Auto-lock, Theme, Language, Privacy options, About, Terms, Privacy, Recycle bin.

## Data model (target)
- `preview`: id, heading, category, sequence, **template, favorite, createdAt, updatedAt, lastUsedAt, deletedAt** (6.0.0)
- `details`: id, previewId (indexed, 5.8.0), question, answer, sequence, **isSecret** (5.6.0), **fieldType** (6.0.0)
- `totp` (5.8.0), `detail_history` (5.8.0)
- Every migration is additive and covered by a `MigrationTestHelper` test, with a rescue backup taken before it runs.

## Offline behaviour
Everything works offline. Remote Config falls back to its in-code defaults. Analytics events are queued by Firebase or dropped.

## Monetisation
None. Free, with no ads (owner decision). Revisit only with the owner, and never by gating security features.

## ASO
See the Phase 6 section of `/Users/bhardwaj/.claude/plans/this-is-my-project-crystalline-boot.md`.
- Title: `PassKey: Offline Password Safe`.
- Category moves from Tools to Productivity.
- Full description to be rewritten with 4,000 honest characters.
- 8 benefit screenshots plus a promo video.

## Success metrics (need Play Console and Firebase access to baseline them)
- Rating from 4.16 to 4.5 or more. 1★ share from 13% to under 5%.
- D1/D7/D30 retention: baseline TBD, aiming for +50% D7.
- Weekly active users using autofill or TOTP (the daily-use signal).
- Crash-free users at 99.5% or more. ANR rate under 0.2%.
- Share of users with a backup made in the last 30 days.
