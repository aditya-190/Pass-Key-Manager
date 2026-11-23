# Analytics events

Source of truth: `app/src/main/java/com/bhardwaj/passkey/analytics/AppEvents.kt`. `EventValidatorTest` checks
every factory against Firebase limits; `AppEventsTest` and `VaultAnalyticsTest` check that no vault content leaks.

**Rule:** an event says *that* something happened, never *what*. Params are enum names, booleans or coarse buckets.
Nothing is sent unless the user allowed "Usage statistics" (`ConsentApplier`; the manifest default is off).

| Event | Params | Fired when | Since |
|---|---|---|---|
| `screen_view` | `screen_name`, `screen_class`: the route name with arguments stripped | Every navigation destination | 5.5.2 |
| `consent_updated` | `analytics` (bool), `crash` (bool) | The consent screen is saved | 5.5.2 |
| `onboarding_page_view` | `page` (1-based) | An onboarding page is shown | 5.5.2 |
| `onboarding_skipped` | `page` (1-based) | Skip is tapped | 5.5.2 |
| `tutorial_complete` | — | The last onboarding page's Next | 5.5.2 |
| `entry_created` | `category`: banks / mails / apps / others | A new entry (heading) is saved; renames don't count | 5.5.2 |
| `field_saved` | `is_edit` (bool) | A detail field is saved | 5.5.2 |
| `password_generated` | `length_bucket`: 0, 1-7, 8-11, 12-15, 16-23, 24+ | The generator button | 5.5.2 |
| `value_copied` | `sensitive` (bool) | Long-press copy (a heading is false, a value is true) | 5.5.2 |
| `vault_unlocked` | — | Biometric or device-credential success | 5.5.2 |
| `unlock_failed` | — | Authentication error or cancel | 5.5.2 |
| `backup_exported` | `count_bucket` | Defined; wired in 5.6.0 with the encrypted backup | 5.6.0 |
| `backup_imported` | `success`, `count_bucket` | Defined; wired in 5.6.0 | 5.6.0 |
| `safety_check_viewed` | `score_bucket`: 0-49, 50-79, 80-100 | Defined; wired in 5.6.0 | 5.6.0 |
| `language_changed` | `language` (BCP-47 tag) | Defined; wired in 5.6.0 | 5.6.0 |

User property: `consent_analytics` (`true` / `false`).

## Key funnels to build in the Firebase console
- Activation: `tutorial_complete` → `consent_updated` → `vault_unlocked` → first `entry_created` → first `field_saved`.
- Habit: users with `vault_unlocked` on 3+ distinct days in their first 7 (the D7 proxy).
- Value moments: `password_generated` and `value_copied{sensitive=true}` per active user.

## Studio baseline events (5.6.0, `analytics/BaselineEvents.kt`)
`remote_config_fetch{success,duration_ms}`, `force_update_shown{version_code}`, `force_update_clicked`,
`soft_update_shown{version_code}`, `soft_update_dismissed{version_code}`, `maintenance_shown`,
`in_app_update_started{update_type}`, `in_app_update_result{update_type,result}`,
`announcement_shown{id,style}`, `announcement_clicked{id,target}`, `announcement_dismissed{id}`,
`about_action{action}`, `legal_viewed{document}`, `legal_opened_online{document}`.
All validated by the template's EventValidator tests; none carry vault content.
