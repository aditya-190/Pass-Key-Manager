# Privacy Policy: PassKey Password Manager

_Last updated: 2026-11-22 (app version 5.5.2)_

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

## Exports

In this version, **Export Data** writes an unencrypted file to your Downloads folder. Anyone who
gets that file can read it. Keep it somewhere safe and delete it when you no longer need it. An
encrypted backup format replaces it in the next release.

## Device backup

If Android backup is enabled on your phone, it can include PassKey's encrypted database.

## Permissions

| Permission | Why |
|---|---|
| `USE_BIOMETRIC` | Unlocking the vault with your fingerprint, face or screen lock |
| `READ_/WRITE_EXTERNAL_STORAGE` (Android 9 only) | Writing an export to Downloads |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` | Required by the Firebase and Google Play libraries above |

## Children

PassKey is not directed at children and does not knowingly collect information from them.

## Changes

Material changes are noted in the app's release notes and in this file's history.

## Contact

Questions or concerns: yrkkh.cclub@gmail.com
