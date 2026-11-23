# PassKey: brief (revamp, 2026-11-21)

## Problem
People reuse weak passwords because remembering many strong ones is hard. Cloud password managers ask them to
trust a company's servers, and many people (especially in India and other mobile-first markets) don't want that,
or don't want to pay a subscription.

## Target user
- Android-only, mobile-first users with 20–200 logins (banking, UPI and wallet apps, email, social, shopping).
- Privacy-conscious; wants **no account, no cloud, no ads**.
- Not a power user, so they need a fast, obvious first run. One review said the app was "entirely too difficult to set up".
- 17 languages are already shipped (en, hi, bn, mr, gu, ta, te, ar, de, es, fr, it, ja, ko, pt, ru, zh).

## Core job to be done
"When I sign in to an app or site, give me the right password instantly, and never let me lose my passwords."
The supporting jobs are generating strong passwords, holding 2FA codes, and moving to a new phone without loss.

## Today (v5.5.1, Play US data from 2026-11-21)
| Metric | Value |
|---|---|
| Installs | 34,156 (10K+ bucket) |
| Rating | 4.16★ from 165 ratings (5★ 117, 4★ 10, 3★ 5, 2★ 10, 1★ 21) |
| Category | Tools |
| Last update | 2025-11-19 |
| Listing assets | 7 screenshots in mixed sizes, no video |
| Data safety | Says "no data collected", which is **inaccurate** because Firebase Analytics and Crashlytics ship |

Source: https://play.google.com/store/apps/details?id=com.bhardwaj.passkey&hl=en_US&gl=US

## Play Store search terms (from Play autocomplete order and competitor titles; Play does not publish volumes)
password manager · password manager free · offline password manager · password keeper · password saver ·
password vault · password safe · passkey / passkeys · 2fa authenticator · password generator · password app ·
encrypted password manager · open source password manager · password book · secure notes / autofill

## Candidate competitors
Bitwarden, Proton Pass, KeePassDX, Keepass2Android (+ Offline), SafeInCloud ("Password Manager: Safe"),
Enpass, My Passwords, Password Safe and Manager, Password Saver, One Key, and an app with the same name
("PassKey : Password Manager", `com.hardik.passkey`).

## Identity
The app name stays **PassKey** and the package stays `com.bhardwaj.passkey`. Only the Play title is reworked around
keywords (see `../SPEC.md` § ASO). This is the owner's decision from 2026-11-21.
