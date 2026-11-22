#!/bin/bash
# Usage: check_vault.sh <serial> <label>
# Cold-starts PassKey, answers consent if asked, unlocks with the test PIN, and checks the seeded entry
# and its secret are visible. Prints PASS/FAIL lines.
S="$(cd "$(dirname "$0")" && pwd)"
SERIAL=$1; LABEL=$2
a() { ~/Library/Android/sdk/platform-tools/adb -s "$SERIAL" "$@"; }
ui() { python3 -I "$S/ui.py" "$SERIAL" "$@"; }
VERSION=$(a shell dumpsys package com.bhardwaj.passkey | grep -m1 versionName | tr -d ' \r')
a shell am force-stop com.bhardwaj.passkey
a shell input keyevent KEYCODE_WAKEUP
a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null
sleep 4
if ui has "^Decline all$"; then echo "[$LABEL] consent asked"; ui tap "^Decline all$" >/dev/null; sleep 3; fi
# System credential screen: type the PIN.
sleep 2
a shell input text 1234; a shell input keyevent 66
if ui wait "^E2E Upgrade Bank$" 20 >/dev/null; then
  echo "PASS [$LABEL $VERSION] entry visible after unlock"
else
  echo "FAIL [$LABEL $VERSION] entry not visible"; ui dump; exit 1
fi
ui tap "^E2E Upgrade Bank$" >/dev/null
if ui wait "Upgrade-Check-42" 15 >/dev/null; then
  echo "PASS [$LABEL $VERSION] secret decrypted and shown"
else
  echo "FAIL [$LABEL $VERSION] secret not shown"; ui dump; exit 1
fi
