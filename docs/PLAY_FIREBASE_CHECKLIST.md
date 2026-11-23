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

- [ ] Published

### Firebase › Analytics
- [ ] Custom dimensions (event scope) registered for: `category`, `is_edit`, `length_bucket`, `sensitive`, `success`,
      `count_bucket`, `score_bucket`, `language`, `existing_vault`, `reason`, `match`, `result`, `source`.
- [ ] Key events marked: `recovery_setup_completed`, `autofill_filled`, `backup_exported`.
- [ ] Google signals **off**; ads personalisation **off**; data retention 14 months.
- [ ] Explorations built from `docs/ANALYTICS_EVENTS.md`: activation, the 5.7.0 upgrade (`recovery_setup_shown` →
      `recovery_setup_completed` → `vault_unlocked`), and autofill (`autofill_unlock_shown` → `autofill_filled`).

### Firebase › Crashlytics
- [ ] Enabled, with velocity alerts on.
- [ ] After the owner's release build: the 5.8.0 (47) mapping file shows as uploaded.

### Play Console › App content
- [ ] **Data safety:**
  - Collected, **optional** (the user chooses), not shared, encrypted in transit:
    - Crash logs
    - Diagnostics
    - App interactions
  - Purpose: analytics and app functionality.
  - No advertising ID.
  - No vault content, ever.
  - No account, so no account-deletion URL. Analytics data deletion as Firebase handles it.
- [ ] **Advertising ID:** the app does **not** use it (`AD_ID` is removed in the manifest).
- [ ] **Ads:** contains ads = **No**.
- [ ] **Privacy policy URL:** `https://github.com/aditya-190/Pass-Key-Manager/blob/master/Privacy%20Policy.md`
- [ ] **Sensitive permissions:** none to declare. Permissions are USE_BIOMETRIC and INTERNET; there's no
      QUERY_ALL_PACKAGES, only a launcher `<queries>`. The autofill service needs no declaration.

## Upload and test

### Play Console › Internal testing
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
- [ ] Promote 47 from internal testing to **Production at 100%** (the owner's decision), with "What's new" in all 17
      languages.

### Play Console › Store presence (no build needed)
- [ ] Title, short and full description from `docs/STORE_LISTING.md` (English; other languages follow with 6.0.0).
- [ ] Category **Productivity**; contact email as above.
- [ ] Screenshots: unchanged until the 6.0.0 redesign.

## After release (first 7 days)
- [ ] Crashlytics: watch the crash-free users rate, and any `VaultMigration` or `DatabaseKeyManager` stack.
- [ ] Analytics: watch `vault_migration_failed` by `reason`. Any spike means setting the kill switch and stopping.
- [ ] Reply to 1–2★ reviews about "can't find add" (now a labelled Add button) and autofill ("now supported").
