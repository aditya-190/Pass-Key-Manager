#!/bin/bash
# Proves a release build opens the vault of the previous Play version (same SQLCipher key) and back.
# Usage: scripts/e2e/upgrade_check.sh <serial> <previous-version.apk> <new-version.apk> <androidTest.apk>
# Needs: a test emulator with screen-lock PIN 1234 (adb -s <serial> shell locksettings set-pin 1234).
# Never point this at a phone with real data: it adds one test entry to the vault.
# The downgrade leg only works while both versions share a Room schema. Once the device's vault has been
# migrated forward, verify upgrades on a fresh emulator instead (install OLD first, seed, then upgrade).
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
SERIAL=$1; OLD=$2; NEW=$3; TEST=$4
a() { ~/Library/Android/sdk/platform-tools/adb -s "$SERIAL" "$@"; }
a install -r "$NEW" >/dev/null && a install -r "$TEST" >/dev/null
a shell am force-stop com.bhardwaj.passkey
a shell am instrument -w -e seedVault true -e class com.bhardwaj.passkey.VaultSeedTool \
  com.bhardwaj.passkey.test/androidx.test.runner.AndroidJUnitRunner >/dev/null
bash "$DIR/check_vault.sh" "$SERIAL" seeded
a install -r -d "$OLD" >/dev/null && bash "$DIR/check_vault.sh" "$SERIAL" downgrade
a install -r "$NEW" >/dev/null && bash "$DIR/check_vault.sh" "$SERIAL" upgrade
