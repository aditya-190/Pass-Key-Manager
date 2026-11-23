# Pain points

These were mined on 2026-11-21 from about 300 recent 1–2★ reviews across 9 offline password managers, plus all 33 written reviews of PassKey.
Sources: the Play listing pages in `COMPETITORS.md` and Play's public review data. Reviews are paraphrased.
Themes are ranked by frequency × severity.

| # | Theme | Seen in | Example (paraphrased) | Our fix | Release |
|---|---|---|---|---|---|
| 1 | **Locked out or data lost after an update, restore or new phone** | 5+ apps (My Passwords, Password Safe, One Key, desmundyeng, Keepass2Android) | "the restore lost every password"; "the backup file won't open" | An encrypted, self-contained `.pkbak` file. Verify-backup. An automatic rescue backup before any migration. A migration that never destroys the old file. A backup reminder and a "new phone" guide. | 5.6.0 / 5.7.0 / 6.0.0 |
| 2 | **Bait paywalls and hidden item caps** | Enpass, desmundyeng, SafeInCloud, My Passwords | "only 25 items free"; "lifetime licence charged again" | Free forever with no ads (owner decision), stated plainly in the listing | all |
| 3 | **Autofill missing, unreliable or re-asking for the password** | Enpass, Keepass2Android, Password Safe, One Key, **PassKey** (onboarding promises autofill but there's no service) | "autofill never shows up" | AutofillService (5.8.0), then a Credential Manager provider (6.0.0). Unlock once per session. | 5.8.0 / 6.0.0 |
| 4 | **"Offline" apps that quietly use the network** | One Key, Keepass2Android Offline, **PassKey** (Firebase undisclosed) | "they added full network access" | Honest text and Data-safety form, consent-gated analytics, no ad ID, the vault never leaves the device | 5.5.2 |
| 5 | **Confusing first run / can't find Add** | My Passwords, KeePassDX, **PassKey** ("there is no option to add more usernames") | "too difficult to set up" | Extended "Add password" FAB, an empty state with a call to action, templates, a 3-page skippable onboarding | 5.6.0 / 6.0.0 |
| 6 | **Passkey confusion** | **PassKey** ("passkeys is not working") | People searching for FIDO passkeys find us | Keyword title now; real passkey storage in 6.1.0 | 6.1.0 |
| 7 | **Forced redesigns with no way back** | Enpass | "opens to a dashboard instead of my list" | Home stays the list. The dashboard is a tab. The redesign keeps the familiar structure. | 6.0.0 |
| 8 | **Clipboard and keyboard bugs** | One Key, My Passwords | phantom "Copied" toasts; numeric keypad for a password field | `EXTRA_IS_SENSITIVE`, timed clear, `KeyboardType.Password` | 5.5.2 |
| 9 | **Auto-lock missing or too aggressive** | KeePassDX, **PassKey** (no re-lock at all) | "relocks every time I switch apps" | Configurable timeout using `elapsedRealtime`, which is immune to clock changes | 5.6.0 |
| 10 | **Developer never answers** | many | "support emails ignored" | Reply to every 1–2★ review. Contact link in About. | ongoing |
| 11 | **No master password** | **PassKey** | "expected a master password" | A recovery password slot (Argon2id) in the per-install key design | 5.7.0 |
| 12 | **No encrypted backup or light/dark choice** | **PassKey** | "encrypted backup would be safer; no theme choice" | `.pkbak` backup (5.6.0), theme picker (6.0.0) | 5.6.0 / 6.0.0 |

## What 5★ reviews praise (don't regress)
"No ads", "simple", "works offline", "perfect". Keep the app fast, simple and ad-free.

## Our wedge (the 3 fixes that make switching worth it)
1. **You can't lose your passwords.** Encrypted backups you control, verified, with a reminder, plus migrations that are
   designed never to destroy data.
2. **Full features, free, with no ads or caps.** Autofill, 2FA codes and a health check are free for everyone.
3. **Honestly offline.** The vault never leaves the phone, analytics are opt-in, and the code is open source to prove it.
