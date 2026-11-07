#!/usr/bin/env bash
# Usage: scripts/m3-smoke.sh [screenshot-dir]   (run from the repo root)
# Emulator smoke test for the M3 migration: boots the `m3` AVD if no device is attached, installs the
# debug build, drives flows with `adb shell input` + `uiautomator dump` (nodes found by text or
# content-desc), screenshots into the given directory, fails on FATAL EXCEPTION in logcat.
# Env: SKIP_INSTALL=1 skips :app:installDebug, KEEP_EMULATOR=1 never kills the emulator.
# To add a flow: write `flow_xxx()` (return non-zero on failure) and register it at the bottom.
set -u

OUT="${1:-.superpowers/sdd/2026-11-07-m3-expressive/smoke}"
PKG=com.ivy.wallet.debug
ACTIVITY=$PKG/com.ivy.wallet.RootActivity
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.config/.android/avd}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export PATH="$PATH:$SDK/platform-tools:$SDK/emulator"
XML="${TMPDIR:-/tmp}/m3-smoke-ui.$$.xml"
EMU_PID=""
RESULTS=()
FAILED=0

cd "$(git rev-parse --show-toplevel)" || exit 2
mkdir -p "$OUT"

log() { echo "[m3-smoke] $*" >&2; }

cleanup() {
  adb shell settings put global animator_duration_scale 1 >/dev/null 2>&1
  rm -f "$XML"
  if [ -n "$EMU_PID" ] && [ -z "${KEEP_EMULATOR:-}" ]; then
    log "stopping emulator started by this script"
    adb emu kill >/dev/null 2>&1
    sleep 2
    kill "$EMU_PID" >/dev/null 2>&1
  fi
}
trap cleanup EXIT

ensure_device() {
  if adb devices | awk 'NR>1 && $2=="device"' | grep -q .; then
    log "using attached device"
    return 0
  fi
  log "booting AVD m3"
  emulator -avd m3 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swangle_indirect \
    >"$OUT/emulator.log" 2>&1 &
  EMU_PID=$!
  local i
  for i in $(seq 1 90); do
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && return 0
    sleep 3
  done
  log "emulator did not boot"
  return 1
}

# ---------- UI helpers ----------

dump() {
  adb shell uiautomator dump /sdcard/m3-ui.xml >/dev/null 2>&1
  adb pull /sdcard/m3-ui.xml "$XML" >/dev/null 2>&1
}

# find_node <text|desc|any|class> <exact|sub> <needle> [index]  ->  "cx cy x1 y1 x2 y2"
find_node() {
  python3 -I - "$XML" "$@" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, attr, match, needle = sys.argv[1:5]
idx = int(sys.argv[5]) if len(sys.argv) > 5 else 0
hits = []
for n in ET.parse(path).iter('node'):
    vals = {'text': [n.get('text')], 'desc': [n.get('content-desc')], 'class': [n.get('class')]}
    vals['any'] = vals['text'] + vals['desc']
    for v in vals[attr]:
        if v and (v == needle if match == 'exact' else needle.lower() in v.lower()):
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
            hits.append((x1, y1, x2, y2))
            break
if idx >= len(hits):
    sys.exit(1)
x1, y1, x2, y2 = hits[idx]
print((x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2)
PY
}

# find_icon <bottom|topright|tabright>: unnamed clickable (content-desc "icon", or the right
# bottom-bar tab, which has no label until selected) by screen region
find_icon() {
  python3 -I - "$XML" "$1" "$(screen_height)" "$(screen_width)" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, region, height, width = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
best = None
for n in ET.parse(path).iter('node'):
    if region == 'tabright':
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        if n.get('clickable') == 'true' and y1 >= height * 0.9 and x1 >= width * 0.5 and x2 - x1 > width * 0.25:
            best = (x1, y1, x2, y2)
        continue
    if n.get('clickable') != 'true' or n.get('content-desc') != 'icon' or n.get('class') != 'android.widget.ImageView':
        continue
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    if region == 'bottom' and y1 >= height * 0.9:
        best = best or (x1, y1, x2, y2)
    if region == 'topright' and y2 <= height * 0.2 and (best is None or x1 > best[0]):
        best = (x1, y1, x2, y2)
if not best:
    sys.exit(1)
x1, y1, x2, y2 = best
print((x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2)
PY
}

# bottom edge of the lowest clickable node that is fully on screen (the pinned action row)
lowest_clickable_bottom() {
  python3 -I - "$XML" "$(screen_height)" <<'PY'
import re, sys, xml.etree.ElementTree as ET
height = int(sys.argv[2])
best = 0
for n in ET.parse(sys.argv[1]).iter('node'):
    if n.get('clickable') != 'true':
        continue
    y2 = int(re.findall(r'\d+', n.get('bounds'))[3])
    if y2 <= height:
        best = max(best, y2)
print(best)
PY
}

screen_width() { adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1; }

screen_height() { adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2; }

has() { find_node "$@" >/dev/null 2>&1; }

# wait_for <text|desc|any> <exact|sub> <needle> [timeout-seconds]
wait_for() {
  local attr=$1 match=$2 needle=$3 timeout=${4:-15} i
  for i in $(seq 1 "$timeout"); do
    dump
    has "$attr" "$match" "$needle" && return 0
    sleep 1
  done
  log "timeout waiting for $attr/$match '$needle'"
  return 1
}

wait_gone() {
  local attr=$1 match=$2 needle=$3 timeout=${4:-10} i
  for i in $(seq 1 "$timeout"); do
    dump
    has "$attr" "$match" "$needle" || return 0
    sleep 1
  done
  log "'$needle' still present"
  return 1
}

tap_xy() { adb shell input tap "$1" "$2" </dev/null; }

# tap_node <text|desc|any> <exact|sub> <needle> [index]: dumps, finds, taps the centre
tap_node() {
  local pos
  dump
  pos=$(find_node "$@") || { log "node not found: $*"; return 1; }
  tap_xy $pos
}

tap_icon() {
  local pos
  dump
  pos=$(find_icon "$1") || { log "icon not found: $1"; return 1; }
  tap_xy $pos
}

shot() { adb exec-out screencap -p >"$OUT/$1.png"; }

back() { adb shell input keyevent KEYCODE_BACK; sleep 1.5; }

fresh_start() {
  adb shell am force-stop $PKG
  adb shell am start -n $ACTIVITY >/dev/null
}

go_home() {
  fresh_start
  wait_for text exact "Home" 20
}

nav_bar_top() {
  adb shell dumpsys window | grep -m1 'type=navigationBars' | grep -oE 'frame=\[0,[0-9]+\]' | grep -oE '[0-9]+' | tail -1
}

ime_top() {
  adb shell dumpsys window | grep -m1 'type=ime frame=.*visible=true' | grep -oE 'frame=\[0,[0-9]+\]' | grep -oE '[0-9]+' | tail -1
}

crashed() { adb logcat -d | grep -q "FATAL EXCEPTION"; }

# ---------- flows ----------

# Cold start through onboarding (offline account, skip accounts and categories) to Home.
flow_onboarding() {
  adb shell pm clear $PKG >/dev/null
  adb logcat -c
  adb shell am start -n $ACTIVITY >/dev/null
  wait_for text exact "I accept and agree" 30 || return 1
  # the four unchecked boxes are the only clickable CheckBox nodes
  python3 -I - "$XML" >"$XML.boxes" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    if n.get('class') == 'android.widget.CheckBox':
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  while read -r x y; do tap_xy "$x" "$y"; done <"$XML.boxes"
  rm -f "$XML.boxes"
  sleep 1
  tap_node text exact "I accept and agree" || return 1
  wait_for text exact "Offline account" || return 1
  tap_node text exact "Offline account" || return 1
  wait_for text exact "Start fresh" || return 1
  tap_node text exact "Start fresh" || return 1
  wait_for text exact "Set currency" || return 1
  tap_node text exact "Set" || return 1
  wait_for text exact "Add accounts" || return 1
  tap_node text exact "Skip" || return 1
  wait_for text exact "Add categories" || return 1
  tap_node text exact "Skip" || return 1
  wait_for text exact "Home" 20 || return 1
  shot onboarding-home
  crashed && return 1
  return 0
}

open_account_edit_sheet() {
  go_home || return 1
  tap_icon tabright || return 1
  wait_for text exact "Total Balance" || return 1
  tap_node text exact "Cash" || return 1
  wait_for text exact "Edit" || return 1
  tap_node text exact "Edit" || return 1
  wait_for text exact "Edit account" || return 1
}

# Edit account -> Amount -> Calculator, then one back per level, exactly one level closing each time.
flow_nested_back() {
  open_account_edit_sheet || return 1
  shot nested-1-account
  tap_node text exact "ENTER ACCOUNT BALANCE" || return 1
  tap_node text exact "0.00 USD" || return 1
  wait_for text exact "Enter" || return 1
  shot nested-2-amount
  tap_icon bottom || return 1
  wait_for text exact "Set" || return 1
  has text exact "÷" || { log "calculator keypad missing"; return 1; }
  shot nested-3-calculator

  back
  dump
  has text exact "Enter" && ! has text exact "÷" || { log "back 1: expected only the Amount sheet on top"; return 1; }
  shot nested-4-after-back-1
  back
  dump
  has text exact "Edit account" && ! has text exact "Enter" || { log "back 2: expected only the Account sheet on top"; return 1; }
  shot nested-5-after-back-2
  back
  dump
  has text exact "Edit" && ! has text exact "Edit account" || { log "back 3: expected the account screen"; return 1; }
  has text exact "Cash" || { log "back 3: account screen missing"; return 1; }
  shot nested-6-after-back-3
  crashed && return 1
  return 0
}

# Sheet with a text field: focus it, keyboard up, the pinned action row must sit above the keyboard.
flow_keyboard_sheet() {
  open_account_edit_sheet || return 1
  tap_node class exact android.widget.EditText || return 1
  local ime="" i
  for i in 1 2 3 4 5 6 7 8; do
    sleep 1
    ime=$(ime_top)
    [ "${ime:-0}" -gt 0 ] && break
  done
  [ "${ime:-0}" -gt 0 ] || { log "keyboard did not show"; return 1; }
  sleep 1
  shot keyboard-sheet
  dump
  local row
  row=$(lowest_clickable_bottom)
  log "action row bottom=$row ime top=$ime"
  [ "$row" -le "$ime" ] || { log "action row is under the keyboard"; return 1; }
  has desc exact "Close" || { log "close button missing"; return 1; }
  adb shell input keyevent KEYCODE_BACK
  sleep 1
  back
  crashed && return 1
  return 0
}

# Sheet without keyboard: the action row must end above the navigation bar.
flow_navbar_clearance() {
  open_account_edit_sheet || return 1
  sleep 1
  shot navbar-sheet
  dump
  local row nav
  row=$(lowest_clickable_bottom)
  nav=$(nav_bar_top)
  log "action row bottom=$row navbar top=$nav"
  [ "$row" -le "$nav" ] || { log "action row overlaps the navigation bar"; return 1; }
  crashed && return 1
  return 0
}

# Start closing a sheet, drag it back up while it is still hiding (cancels the hide), tap Close
# again: the sheet must close and exactly one level goes away.
flow_cancelled_hide() {
  open_account_edit_sheet || return 1
  adb shell settings put global animator_duration_scale 10
  local w h attempt cancelled=""
  w=$(screen_width); h=$(screen_height)
  for attempt in 1 2 3 4 5 6; do
    dump
    pos=$(find_node desc exact "Close") || { log "close button missing"; adb shell settings put global animator_duration_scale 1; return 1; }
    set -- $pos
    adb shell "input tap $1 $2; input swipe $((w / 2)) $((h * 9 / 10)) $((w / 2)) $((h * 6 / 10)) ${SWIPE_MS:-250}" </dev/null
    sleep 3
    dump
    if has text exact "Edit account"; then
      cancelled=$attempt
      break
    fi
    log "attempt $attempt: sheet closed before the drag, reopening"
    wait_for text exact "Edit" || break
    tap_node text exact "Edit" || break
    wait_for text exact "Edit account" || break
  done
  adb shell settings put global animator_duration_scale 1
  [ -n "$cancelled" ] || { log "could not interrupt the hide"; return 1; }
  log "hide cancelled on attempt $cancelled"
  shot cancelled-hide-1-sheet-back
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Edit account" || return 1
  sleep 1
  dump
  has text exact "Edit" && has text exact "Cash" || { log "expected the account screen after the second close"; return 1; }
  shot cancelled-hide-2-closed
  crashed && return 1
  return 0
}

# ---------- runner ----------

run_flow() {
  local name=$1
  if "$name"; then
    echo "PASS $name"
    RESULTS+=("PASS $name")
  else
    echo "FAIL $name"
    RESULTS+=("FAIL $name")
    shot "FAIL-$name" 2>/dev/null
    FAILED=1
  fi
}

ensure_device || exit 2
if [ -z "${SKIP_INSTALL:-}" ]; then
  log "installing debug build"
  ./gradlew --console=plain --max-workers=3 :app:installDebug >"$OUT/install.log" 2>&1 || { log "installDebug failed, see $OUT/install.log"; exit 2; }
fi
adb shell settings put global animator_duration_scale 1 >/dev/null 2>&1

for flow in ${FLOWS:-flow_onboarding flow_nested_back flow_keyboard_sheet flow_navbar_clearance flow_cancelled_hide}; do
  run_flow "$flow"
done

adb logcat -d >"$OUT/logcat.txt" 2>&1
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  echo "FAIL logcat: FATAL EXCEPTION found (see $OUT/logcat.txt)"
  FAILED=1
else
  echo "PASS logcat: no FATAL EXCEPTION"
fi

echo "---"
printf '%s\n' "${RESULTS[@]}"
exit $FAILED
