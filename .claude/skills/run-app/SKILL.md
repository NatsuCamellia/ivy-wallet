---
name: run-app
description: Build, install and drive the Ivy Wallet debug app on a headless Android emulator to screenshot a screen (Home, Accounts, Categories, ...) or run smoke flows, and re-record Paparazzi goldens for changed modules. Use to see a UI change working or to verify behavior in the real app.
---

# Run the app and see your change

Run everything from inside the repo. Both scripts need no running emulator: `scripts/smoke.sh` boots the `m3`
AVD headless when no device is attached and shuts it down afterwards. It builds first (`:app:assembleDebug`),
then installs with `adb install`, so expect 2 to 3 minutes per run.

## Screenshot a screen

```sh
scripts/smoke.sh shot <screen>      # prints the PNG path, e.g. build/smoke/screens/home.png
```

Screens: `home`, `accounts`, `categories`, `budgets`, `loans`, `planned-payments`, `reports`, `settings`
(`scripts/smoke.sh list` prints them). The script completes onboarding on a fresh install (offline account,
Cash and Bank accounts), opens the screen from a cold start and saves the screenshot. Read the PNG to look
at it. On failure it exits non-zero and saves `FAIL-<screen>.png` next to it. To add a screen, add a case to
`open_screen` and to `ALL_SCREENS` in the script.

## Run flows

```sh
scripts/smoke.sh                          # every flow
scripts/smoke.sh flows more_menu categories   # only these; the flow_ prefix is optional
scripts/smoke.sh list                     # flow and screen names
```

Prints `PASS`/`FAIL` per flow and fails on `FATAL EXCEPTION` in logcat. Flow screenshots and `logcat.txt` go
to `build/smoke/`. Most flows assume the default onboarding state: run `onboarding` first on a fresh device
(`scripts/smoke.sh flows onboarding more_menu`), or keep the emulator between runs.
To add a flow: write `flow_xxx()` in the script and add it to `ALL_FLOWS`.

## Environment variables

| Variable | Default | Meaning |
|----------|---------|---------|
| `OUT` | `build/smoke` | Output directory (screenshots, logs) |
| `SKIP_INSTALL=1` | unset | Skip the build and install, use what is on the device |
| `KEEP_EMULATOR=1` | unset | Do not kill an emulator the script booted |
| `AVD` | `m3` | AVD to boot (`m3-31` and `ivy_test` also exist) |
| `PKG` | `net.natsucamellia.ivywallet.debug` | App id to drive |
| `FLOWS` | all | Space separated flow names, same as the `flows` arguments |
| `GRADLE_WORKERS` | `2` | Gradle `--max-workers` of the build the script runs |

Keep the emulator and skip rebuilds when taking several screenshots:
`KEEP_EMULATOR=1 scripts/smoke.sh shot home`, then `KEEP_EMULATOR=1 SKIP_INSTALL=1 scripts/smoke.sh shot accounts`.
Kill the emulator yourself afterwards with `adb emu kill`.

## Check a demo build before installing it

```sh
./gradlew --max-workers=2 :app:assembleDemo
scripts/check-hilt-keys.sh      # every @HiltViewModel must have its generated Hilt classes
```

KSP sometimes writes no sources for a module, with no error. The build succeeds, but the screen of that module's
ViewModel crashes in the minified build with "Cannot create an instance of class ..." (the Accounts tab did, after
53db97734). The smoke suite runs the debug build, so it cannot see this. When the check fails, run
`./gradlew --max-workers=2 :<module>:kspReleaseKotlin --rerun` for the module it names and assemble again. After
`./gradlew clean`, run the build as a second command.

## Paparazzi goldens

```sh
scripts/paparazzi-record.sh            # record goldens of changed modules, list added/changed snapshot files
scripts/paparazzi-record.sh --verify   # verifyPaparazziDebug for the same modules
```

A module counts as changed when a file under it differs from `BASE` (default `HEAD`, including untracked
files; use `BASE=main` to include commits on the branch) and it has Paparazzi tests. `MODULES=":feature:home"`
overrides detection. Review the listed PNGs before committing them.
