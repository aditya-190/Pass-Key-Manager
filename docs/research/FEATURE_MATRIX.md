# Feature matrix

Key: ✓ = stated on the 2026-11-21 Play listing; ✗ = absent; ? = not verified. Never guess: a ? stays a ? until checked.
Columns: PK = PassKey 5.5.1, BW = Bitwarden, PP = Proton Pass, KDX = KeePassDX, SIC = SafeInCloud, MyP = My Passwords,
PS = Password Safe, EN = Enpass.

| Feature | PK | BW | PP | KDX | SIC | MyP | PS | EN | Tag | Target release |
|---|---|---|---|---|---|---|---|---|---|---|
| Encrypted local vault | ✓* | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | Must | 5.7.0 (*real per-install key) |
| Biometric unlock | ✓ | ? | ? | ✓ | ? | ? | ? | ? | Must | 5.7.0 (CryptoObject-bound) |
| Auto-lock (timeout or background) | ✗ | ? | ? | ✓ | ? | ? | ✓ | ? | Must | 5.6.0 |
| Clipboard auto-clear and sensitive flag | ✗ | ? | ? | ? | ? | ? | ✓ | ? | Must | 5.5.2 |
| Screenshot blocking | ✓ | ? | ? | ? | ? | ? | ✓ | ? | Must | done |
| Autofill service | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ (Pro) | ✓ | ✓ | Must | 5.8.0 |
| Credential Manager provider (passwords) | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | Must | 6.0.0 |
| Passkey (FIDO) storage | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | Must (name collision) | 6.1.0 |
| TOTP / 2FA codes | ✗ | ? | ? | ✓ | ✓ | ✓ | ✓ | ? | Must | 5.8.0 |
| Password generator | ✓ | ? | ? | ? | ? | ? | ✓ | ? | Must | upgrade in 6.0.0 (passphrase) |
| Password health (weak, reused) | ✓ (English only) | ? | ? | ? | ✓ | ? | ? | ? | Must | fixed 5.6.0, dashboard 6.0.0 |
| Breach check (online) | ✗ | ? | ? | ? | ✓ | ? | ? | ? | Skip (owner decision) | — |
| Entry types (login, card, note, identity) | ✗ (free-form) | ? | ? | ? | ? | ? | ? | ? | Must | 6.0.0 templates |
| Password history | ✗ | ? | ? | ? | ? | ? | ✓ | ? | Delight | 5.8.0 |
| Recycle bin / undo delete | ✗ | ? | ? | ? | ? | ? | ✓ | ? | Must | Undo in 5.6.0, bin in 6.0.0 |
| Encrypted backup file | ✗ (plaintext CSV) | ? | ? | ✓ | ✓ | ✓ | ✓ | ? | Must | 5.6.0 |
| Import from other managers (CSV) | ✗ | ? | ? | ? | ? | ? | ✓ | ? | Must | 6.0.0 |
| Backup reminder | ✗ | ? | ? | ? | ? | ? | ? | ? | Delight | 6.0.0 |
| Cross-device cloud sync | ✗ | ✓ | ✓ | ✗ | ✓ | ? | ✓ | ✓ | Skip (offline positioning) | — |
| Home-screen generator widget | ✗ | ? | ? | ? | ? | ? | ✓ | ? | Delight | 6.0.0 |
| Wear OS | ✗ | ? | ? | ? | ✓ | ✓ | ✓ | ? | Skip for now | later |
| Themes / dark mode | ✓ (system only) | ? | ? | ? | ? | ? | ? | ? | Must | 6.0.0 theme picker |
| 17 languages | ✓ | ? | ? | ? | ? | ? | ? | ? | Delight (our edge) | fixed 5.5.2 |
| Open source | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | Delight | keep |
| No ads, core free forever | ✓ | ✓ | ✓ | ✓ | ? | ? | ? | ✗ (caps) | Must | keep (owner decision) |
