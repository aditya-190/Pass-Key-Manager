#!/bin/bash
# On-device auto-lock checks (test emulator with screen-lock PIN 1234, default 30 s auto-lock).
# Usage: scripts/e2e/lock_check.sh <serial>
DIR="$(cd "$(dirname "$0")" && pwd)"
SERIAL=$1
a() { ~/Library/Android/sdk/platform-tools/adb -s "$SERIAL" "$@"; }
ui() { python3 -I "$DIR/ui.py" "$SERIAL" "$@"; }
locked() { a shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" | grep -qE "ConfirmLock|ConfirmDeviceCredential|BiometricPrompt|security_page" || ui has "Welcome to PassKey Manager"; }
home_visible() { ui has "^Settings$"; }
unlock() { sleep 2; a shell input text 1234; a shell input keyevent 66; ui wait "^Settings$|^Add detail$" 20 >/dev/null; }
open_fresh() {
  a shell am force-stop com.bhardwaj.passkey
  a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null
  sleep 6
  unlock
}
result() { if [ "$1" = "$2" ]; then echo "PASS $3"; else echo "FAIL $3 (expected $2, got $1)"; fi; }

a shell input keyevent KEYCODE_WAKEUP
open_fresh
home_visible && echo "setup: unlocked" || echo "setup: NOT unlocked"

# 1. Short trip to the background (< 30 s) keeps the vault open.
a shell input keyevent KEYCODE_HOME; sleep 5
a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null; sleep 3
result "$(home_visible && echo open || echo locked)" open "back after 5 s stays unlocked"

# 2. Longer than the 30 s timeout locks.
a shell input keyevent KEYCODE_HOME; sleep 35
a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null; sleep 4
result "$(home_visible && echo open || echo locked)" locked "back after 35 s is locked"
unlock
result "$(home_visible && echo open || echo locked)" open "PIN unlocks again (no lock loop)"

# 3. Process death while a vault screen is open: the restored screen must not open unlocked.
ui tap "^E2E Upgrade Bank$" >/dev/null 2>&1 || ui tap "Banks" >/dev/null
sleep 2
a shell input keyevent KEYCODE_HOME; sleep 2
a shell am kill com.bhardwaj.passkey; sleep 2
a shell pidof com.bhardwaj.passkey >/dev/null && echo "note: process still alive"
a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null; sleep 6
result "$(ui has '^Add detail$|^Settings$' && echo open || echo locked)" locked "restored after process death is locked"
unlock
result "$(home_visible && echo open || echo locked)" open "PIN unlocks after process death"
