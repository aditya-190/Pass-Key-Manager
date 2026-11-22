#!/bin/bash
# Cold-starts PassKey and answers whatever unlock the gate asks for: the system PIN screen (PIN 1234) or the
# fingerprint prompt (simulated with `adb emu finger touch 1`). Prints which one was used and the result.
# Usage: scripts/e2e/unlock_once.sh <serial>   (test emulator only)
SERIAL=$1
a() { ~/Library/Android/sdk/platform-tools/adb -s "$SERIAL" "$@"; }
trace() { a logcat -d -s PassKeyLock; }
a shell input keyevent KEYCODE_HOME; sleep 1
a shell am force-stop com.bhardwaj.passkey; a shell am force-stop com.android.settings
a logcat -c
a shell am start -n com.bhardwaj.passkey/.presentation.MainActivity >/dev/null
for _ in $(seq 1 20); do trace | grep -q "gate state Authenticating" && break; sleep 1; done
sleep 2
method="none"
for _ in $(seq 1 15); do
  if a shell dumpsys activity activities | grep -m1 mResumedActivity | grep -q ConfirmLock; then
    sleep 1; a shell input text 1234; a shell input keyevent 66; method="pin"; break
  fi
  if trace | grep -q "bio slot prompt"; then
    a emu finger touch 1 >/dev/null 2>&1; method="fingerprint"; break
  fi
  sleep 1
done
for _ in $(seq 1 20); do trace | grep -qE "session unlocked|NeedsRecoveryPassword" && break; sleep 1; done
if trace | grep -q "session unlocked"; then echo "UNLOCKED via $method"; else echo "NOT UNLOCKED (method=$method)"; fi
trace | grep -E "slot" | sed 's/.*PassKeyLock: /  /'
