# Privacy Policy: PassKey Password Manager

_Last updated: 2026-11-23 (app version 5.6.0)_

## Summary

Your vault stays on your phone. PassKey has no account, no sync and no server of its own. It never
collects, transmits or stores the passwords, usernames, notes or anything else you save, and we
cannot read or recover them.

The app includes Google Firebase and Google Play libraries. **Firebase sends nothing until you
choose**, and you can change that choice at any time in **Settings › Privacy options**.

## What is sent, and only if you allow it

| You allow | Sent by | What it contains |
|---|---|---|
| Crash and performance reports | Firebase Crashlytics and Performance Monitoring | Crash stack traces, device model, Android version, app version, a Crashlytics installation identifier, startup and screen timings |
| Usage statistics | Firebase Analytics | Which screens and features are used, as counts and coarse ranges (for example "a 12–15 character password was generated"), plus a Firebase installation identifier |

Neither ever contains vault content: no entry names, field names, values or passwords. Both are
off on a new install and on upgrade until you decide. A crash that happens before you decide is
kept on the device: it is sent only if you then allow crash reports, and deleted if you decline.

PassKey does **not** use an advertising ID, and has no ads.

Data handled by these services is processed by Google under
[Google's privacy policy](https://policies.google.com/privacy).

## Always on

- **Google Play in-app updates**: the app asks Google Play whether a newer version is available.
- **Google Firebase Remote Config**: the app fetches settings for update notices and announcements. The request
  carries a Firebase installation identifier and nothing you save. It is not used for analytics.

## Backups

**Export Data** writes a backup encrypted with AES-256-GCM, using a key derived from a password you
choose, to a location you pick with the system file picker. Anyone who has both the file and its
password can read it. We keep no copy and cannot recover the password.

## Device backup

PassKey excludes its data from Android cloud backup and device-to-device transfer, so your vault is
not uploaded to your Google account. Use an encrypted backup to move to a new phone.

## Permissions

| Permission | Why |
|---|---|
| `USE_BIOMETRIC` | Unlocking the vault with your fingerprint, face or screen lock |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` | Required by the Firebase and Google Play libraries above |

## Children

PassKey is not directed at children and does not knowingly collect information from them.

## Changes

Material changes are noted in the app's release notes and in this file's history.

## Contact

Questions or concerns: yrkkh.cclub@gmail.com
