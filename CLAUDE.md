# PassKey: app rules (adds to `../CLAUDE.md`; wins on conflict)

## Hard rules
- **Never read, print, grep or copy `local.properties`.** It holds the production SQLCipher `PASS_PHRASE`, the legacy key
  for every existing user's vault. Code reads it only through Gradle `BuildConfig`. Ask the owner whether it is set.
- Never log, send to analytics, or attach to a crash report any vault content: headings, field names, values, TOTP secrets,
  or backup passwords. Analytics events carry only enums or buckets.
- Never wipe app data on a device or emulator. Database or key migrations must never delete the original file until the new
  one has been verified.
- No ads and no paywall (owner decision). No network features beyond Firebase, Play in-app update and in-app review
  without the owner's approval.
- Prior work lives in closed PRs #1–#4 (`refs/pull/N/head`). Reuse it per `docs/SPEC.md`; don't replay it blindly.

## Simulation (Feature workflow step 3)
Seeded vault generator (random headings and fields in every supported script, including emoji, RTL, 10k-character values,
empty and duplicate values) run 1000+ times through:
backup encode → decode round-trip (and tampered or truncated files), legacy CSV import, Room migrations,
password analyser, generator invariants, autofill matcher, TOTP vectors.
On a device: `adb shell monkey -p com.bhardwaj.passkey --throttle 50 -s <seed> 20000`, plus repeated `am instrument`.
Report pass/fail counts and failing seeds.

## Gates
`./gradlew ktlintCheck detekt testDebugUnitTest lintDebug assembleRelease`, and `connectedDebugAndroidTest` before release.
