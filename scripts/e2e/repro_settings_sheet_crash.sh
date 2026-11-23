#!/bin/bash
# Reproduces the 5.5.2 monkey crash (seed 5521): "Cannot set 'scaleY' to Infinity" on Settings.
# Opens Settings › Privacy, then Change language from the expanded sheet, repeatedly; exits 1 if the app process dies.
# Usage: scripts/e2e/repro_settings_sheet_crash.sh <serial> [rounds]   (test emulator, PIN 1234)
DIR="$(cd "$(dirname "$0")" && pwd)"
SERIAL=$1; ROUNDS=${2:-15}
a() { ~/Library/Android/sdk/platform-tools/adb -s "$SERIAL" "$@"; }
ui() { python3 -I "$DIR/ui.py" "$SERIAL" "$@"; }
a shell am force-stop com.bhardwaj.passkey
a logcat -c
a shell input keyevent KEYCODE_WAKEUP
a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null
sleep 6
a shell input text 1234; a shell input keyevent 66
ui wait "^Settings$" 20 >/dev/null || { echo "could not unlock"; exit 2; }
ui tap "^Settings$" >/dev/null; sleep 2
for i in $(seq 1 "$ROUNDS"); do
  # Expand the Privacy sheet, then switch the same sheet to the language picker (zero-height content).
  ui tap "^Privacy$" >/dev/null; sleep 1.5
  ui tap "Change Language" >/dev/null; sleep 1.5
  a shell input keyevent KEYCODE_BACK; sleep 1
  a shell input keyevent KEYCODE_BACK; sleep 1
  pidof_out=$(a shell pidof com.bhardwaj.passkey)
  [ -z "$pidof_out" ] && { echo "CRASH on round $i"; a logcat -d -b crash | grep -m2 -E "Exception|scaleY"; exit 1; }
  ui has "^Settings$" && { ui tap "^Settings$" >/dev/null; sleep 1.5; }
  if ! a shell pidof com.bhardwaj.passkey >/dev/null; then
    echo "CRASH on round $i"; a logcat -d -b crash | grep -m2 -E "Exception|scaleY"; exit 1
  fi
done
echo "NO CRASH after $ROUNDS rounds"
