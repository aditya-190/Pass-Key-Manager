# Firebase and Play Console changes for 5.8.0

Done in the browser (Claude in Chrome) once the owner gives access. Tick each item and record the date. Nothing here
needs a new build.

## Before uploading

### Firebase › Remote Config (project of `com.bhardwaj.passkey`)
Publish the parameters from `firebase/remoteconfig.template.json`, keeping its groups, with these values:

| Key | Value | Why |
|---|---|---|
| `min_supported_version_code` | `1` | Nobody is forced to update |
| `force_update_enabled` | `false` | The kill switch stays off unless needed |
| `latest_version_code` | `47` | Builds below 47 that read Remote Config (only future ones) get a soft prompt |
| `in_app_update_enabled` | `true` | Play in-app update is allowed |
| `soft_update_interval_days` | `3` | |
| `maintenance_mode` | `false` | PassKey ignores it anyway (offline vault) |
| `announcement_json` | *(empty)* | No banner |
| `privacy_url` | `https://github.com/aditya-190/Pass-Key-Manager/blob/master/Privacy%20Policy.md` | |
| `terms_url` | *(empty)* | Terms are shown in the app |
| `support_email` | the address already published in Privacy Policy.md | |
| `config_version` | `1` | |

- [x] Published 2026-11-23 as Remote Config version 1 (16 parameters in 4 groups).

### Firebase › Analytics
- [x] Custom dimensions (event scope), 2026-11-23, registered for: `category`, `is_edit`, `length_bucket`, `sensitive`, `success`,
      `count_bucket`, `score_bucket`, `language`, `existing_vault`, `reason`, `match`, `result`, `source`.
- [ ] Key events marked: `recovery_setup_completed`, `autofill_filled`, `backup_exported`. **Pending:** GA only lets you
      mark events it has received, so do this once 5.8.0 users send them.
- [x] Google signals **off**; ads personalisation **off** (all regions); granular location and device data **off**.
      Event data retention left at **2 months** (privacy choice; one setting to raise to 14 if longer explorations are needed).
- [ ] *(Later, once data arrives)* Explorations built from `docs/ANALYTICS_EVENTS.md`: activation, the 5.7.0 upgrade (`recovery_setup_shown` →
      `recovery_setup_completed` → `vault_unlocked`), and autofill (`autofill_unlock_shown` → `autofill_filled`).

### Firebase › Crashlytics
- [x] Enabled (5.5.1 (43) at 100% crash-free on 2026-11-23).
- [ ] After the owner's release build: the 5.8.0 (47) mapping file shows as uploaded.

### Play Console › App content
- [x] **Data safety** (saved 2026-11-23, sent with the 5.8.0 review):
  - Collected, **optional** (the user chooses), not shared, encrypted in transit:
    - Crash logs
    - Diagnostics
    - App interactions
  - Purpose: analytics.
  - Device or other IDs (Firebase installation ID): collected, **required**, not shared; app functionality and analytics.
  - No advertising ID.
  - No vault content, ever.
  - No account, so no account-deletion URL. Analytics data deletion as Firebase handles it.
- [x] **Advertising ID** (changed to No on 2026-11-23): the app does **not** use it (`AD_ID` is removed in the manifest).
- [ ] **Ads:** contains ads = **No**.
- [x] **Privacy policy URL:** `https://github.com/aditya-190/Pass-Key-Manager/blob/master/Privacy%20Policy.md`
- [x] **Sensitive permissions:** none to declare. Permissions are USE_BIOMETRIC and INTERNET; there's no
      QUERY_ALL_PACKAGES, only a launcher `<queries>`. The autofill service needs no declaration.

## Upload and test

### Play Console › Internal testing
**Skipped (owner's decision, 2026-11-23): 47 went straight to production.** The tester list (the owner's two accounts)
was ticked for the track, which is still paused.

- [ ] The owner uploads the signed AAB (5.8.0, versionCode 47) to **Internal testing**, with the "What's new" from
      `docs/RELEASE_5.8.0.md`.
- [ ] The upgrade check on the Moto G34:
  - Uninstall the debug test app (test data only).
  - Install production **5.5.1** from Play and add a few entries.
  - Join internal testing and update to 47.
  - Expect the privacy choices → "Set a recovery password" → screen lock → the vault opens with every entry intact.
- [ ] Kill-switch rehearsal, while only testers have 47:
  - Set `min_supported_version_code=48` and `force_update_enabled=true`.
  - Open the app and confirm the block screen.
  - **Revert to 1 / false.**
- [ ] Pre-launch report: no crashes, and no accessibility blockers.

## Release

### Play Console › Production
- [x] 2026-11-23: 47 uploaded directly to **Production at 100%** and sent for review with the listing, Data safety and
      category changes (managed publishing off, so it goes live on approval). "What's new" in all 17 languages; Play
      needs the tag `gu`, not `gu-IN`, for Gujarati. Play's pre-launch report and the kill-switch rehearsal were not run.

### Play Console › Store presence (no build needed)
- [x] Title, short and full description (en-GB default listing; AI asset declaration: don't label) from `docs/STORE_LISTING.md` (English; other languages follow with 6.0.0).
- [x] Category **Productivity**; contact email already set.
- [ ] Screenshots: unchanged until the 6.0.0 redesign.

## After release (first 7 days)
- [ ] Crashlytics: watch the crash-free users rate, and any `VaultMigration` or `DatabaseKeyManager` stack.
- [ ] Analytics: watch `vault_migration_failed` by `reason`. Any spike means setting the kill switch and stopping.
- [ ] Reply to 1–2★ reviews about "can't find add" (now a labelled Add button) and autofill ("now supported").

---

# 6.0.0 (versionCode 48)

## Before the owner builds
- [x] Version 6.0.0 (48); release notes in 17 languages in `docs/RELEASE_6.0.0.md` (Gujarati tag `gu`).
- [x] All gates and the on-device suite on the Moto G34; `release/6.0.0` merged into `master`.

## Firebase › Remote Config (publish as version 2)
| Key | Value |
|---|---|
| `latest_version_code` | `48` |
| `review_prompt_enabled` | `true` (group Reviews) |
| `review_min_days` | `3` |
| `review_cooldown_days` | `120` |
| `review_after_fills` | `3` |
| `config_version` | `2` |
- [ ] Published

## Firebase › Analytics
- [ ] Custom dimension `source` already exists (used by `review_requested`, `app_shared`).
- [ ] Key events from 5.8.0 still pending until received: `recovery_setup_completed`, `autofill_filled`, `backup_exported`.

## Play Console
- [ ] Internal testing: upload the AAB (48), What's new in 17 languages, testers list (owner's 2 accounts), resume the track.
- [ ] Real update on the Moto: 47 (Play) → 48 (internal testing); vault, theme, settings intact; tour not shown.
- [ ] Production: promote 48 at 100% after the update check (owner's standing decision), What's new in 17 languages.
- [ ] Data safety: unchanged (no new data collected; review and share events are app interactions, already declared).

