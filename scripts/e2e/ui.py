"""Tiny UI driver over `uiautomator dump` for one emulator.
Usage: python3 -I scripts/e2e/ui.py <serial> <cmd> [args]
  wait <regex> [timeout]   wait until any node text/content-desc matches
  tap <regex>              tap the first matching node's centre
  has <regex>              exit 0 if present, 1 if not
  dump                     print all visible texts/descs
  text <string>            type text into the focused field
  key <keycode>            send a key event
"""
import os
import re
import subprocess
import sys
import time

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
serial, cmd, *args = sys.argv[1:]


def sh(*a):
    return subprocess.run([ADB, "-s", serial, *a], capture_output=True, text=True).stdout


def nodes():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = sh("shell", "cat", "/sdcard/ui.xml")
    out = []
    for m in re.finditer(r"<node [^>]*>", xml):
        n = m.group(0)
        text = re.search(r' text="([^"]*)"', n).group(1)
        desc = re.search(r' content-desc="([^"]*)"', n).group(1)
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if text or desc:
            x1, y1, x2, y2 = map(int, b.groups())
            out.append((text, desc, (x1 + x2) // 2, (y1 + y2) // 2))
    return out


def find(pattern):
    rx = re.compile(pattern)
    for text, desc, x, y in nodes():
        if rx.search(text) or rx.search(desc):
            return x, y
    return None


if cmd == "wait":
    deadline = time.time() + float(args[1] if len(args) > 1 else 20)
    while time.time() < deadline:
        if find(args[0]):
            print("found", args[0])
            sys.exit(0)
        time.sleep(1)
    print("TIMEOUT waiting for", args[0], "| visible:", [t or d for t, d, _, _ in nodes()])
    sys.exit(1)
elif cmd == "tap":
    pos = find(args[0])
    if not pos:
        print("NOT FOUND", args[0], "| visible:", [t or d for t, d, _, _ in nodes()])
        sys.exit(1)
    sh("shell", "input", "tap", str(pos[0]), str(pos[1]))
    print("tapped", args[0])
elif cmd == "has":
    sys.exit(0 if find(args[0]) else 1)
elif cmd == "dump":
    for t, d, x, y in nodes():
        print(repr(t), repr(d), x, y)
elif cmd == "text":
    sh("shell", "input", "text", args[0])
elif cmd == "key":
    sh("shell", "input", "keyevent", args[0])
