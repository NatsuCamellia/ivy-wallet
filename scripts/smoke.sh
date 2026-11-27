#!/usr/bin/env bash
# Emulator smoke tests and screenshots (run from anywhere inside the repo).
#
#   scripts/smoke.sh                      run every flow
#   scripts/smoke.sh flows [name...]      run the named flows (with or without the flow_ prefix), or all
#   scripts/smoke.sh list                 list flow names and screen names
#   scripts/smoke.sh shot <screen>        build, install, open <screen>, save build/smoke/screens/<screen>.png
#
# Boots the `m3` AVD headless if no device is attached, installs the debug build, drives the app with
# `adb shell input` + `uiautomator dump` (nodes found by text or content-desc), fails on FATAL EXCEPTION.
# Env: OUT (output dir, default build/smoke), SKIP_INSTALL=1 (skip :app:installDebug), KEEP_EMULATOR=1 (never
# kill the emulator), AVD (default m3), PKG (default net.natsucamellia.ivywallet.debug; the demo build is
# installed by hand, then use SKIP_INSTALL=1), FLOWS (space separated flow names, same as `flows`),
# GRADLE_WORKERS (Gradle --max-workers of the build, default 2).
# To add a flow: write `flow_xxx()` (return non-zero on failure) and add it to ALL_FLOWS at the bottom.
# To add a screen: add a case to `open_screen` and to ALL_SCREENS.
set -u

REPO_ROOT="$(git rev-parse --show-toplevel)" || exit 2
cd "$REPO_ROOT" || exit 2
MODE=flows
SCREEN=""
case "${1:-}" in
  shot) MODE=shot; SCREEN="${2:-}"; shift 2 2>/dev/null || shift $# ;;
  flows) shift ;;
  list) MODE=list; shift ;;
  -h | --help) sed -n '2,17p' "$0"; exit 0 ;;
esac
FLOW_ARGS=("$@")

OUT="${OUT:-build/smoke}"
PKG="${PKG:-net.natsucamellia.ivywallet.debug}"
AVD="${AVD:-m3}"
ACTIVITY=$PKG/com.ivy.wallet.RootActivity
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.config/.android/avd}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export PATH="$PATH:$SDK/platform-tools:$SDK/emulator"
XML="${TMPDIR:-/tmp}/smoke-ui.$$.xml"
EMU_PID=""
NIGHT_PREV=""
RESULTS=()
FAILED=0

mkdir -p "$OUT"

log() { echo "[smoke] $*" >&2; }

cleanup() {
  adb shell settings put global animator_duration_scale 1 >/dev/null 2>&1
  [ -n "$NIGHT_PREV" ] && adb shell cmd uimode night "$NIGHT_PREV" >/dev/null 2>&1
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
  log "booting AVD $AVD"
  emulator -avd "$AVD" -no-window -no-audio -no-snapshot -no-boot-anim -gpu swangle_indirect \
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

DUMP_FAILED=0

# Fresh UI tree or failure: never leaves a stale tree behind; a failure also fails the running flow.
dump() {
  local attempt
  rm -f "$XML"
  for attempt in 1 2 3; do
    adb shell rm -f /sdcard/smoke-ui.xml >/dev/null 2>&1
    if adb shell uiautomator dump /sdcard/smoke-ui.xml >/dev/null 2>&1 \
      && adb pull /sdcard/smoke-ui.xml "$XML" >/dev/null 2>&1 && [ -s "$XML" ]; then
      return 0
    fi
    sleep 1
  done
  rm -f "$XML"
  log "uiautomator dump failed"
  DUMP_FAILED=1
  return 1
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

# find_icon <bottom|topright>: unnamed clickable (content-desc "icon") by screen region
find_icon() {
  python3 -I - "$XML" "$1" "$(screen_height)" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, region, height = sys.argv[1], sys.argv[2], int(sys.argv[3])
best = None
for n in ET.parse(path).iter('node'):
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

# find_tab <label>: the navigation bar item label (text in the bottom 10% of the screen)
find_tab() {
  python3 -I - "$XML" "$1" "$(screen_height)" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, label, height = sys.argv[1], sys.argv[2], int(sys.argv[3])
for n in ET.parse(path).iter('node'):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    if n.get('text') == label and y1 >= height * 0.9:
        print((x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2)
        sys.exit(0)
sys.exit(1)
PY
}

# close_button <dump>: "cx cy x1 y1 x2 y2" of the clickable node around the Close icon (pinned action row)
close_button() {
  python3 -I - "$XML" <<'PY'
import re, sys, xml.etree.ElementTree as ET
def box(n):
    return tuple(map(int, re.findall(r'\d+', n.get('bounds'))))
nodes = list(ET.parse(sys.argv[1]).iter('node'))
icons = [box(n) for n in nodes if n.get('content-desc') == 'Close']
if not icons:
    sys.exit(1)
ix1, iy1, ix2, iy2 = icons[0]
cx, cy = (ix1 + ix2) // 2, (iy1 + iy2) // 2
best = None
for n in nodes:
    if n.get('clickable') != 'true':
        continue
    x1, y1, x2, y2 = box(n)
    if x1 <= cx <= x2 and y1 <= cy <= y2 and (best is None or (x2 - x1) * (y2 - y1) < (best[2] - best[0]) * (best[3] - best[1])):
        best = (x1, y1, x2, y2)
if not best:
    best = (ix1, iy1, ix2, iy2)
x1, y1, x2, y2 = best
print((x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2)
PY
}

screen_width() { adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1; }

screen_height() { adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2; }

has() { find_node "$@" >/dev/null 2>&1; }

# has_clickable <text>: a clickable node that is, or contains, a node whose text is exactly <text> (a button, not a label with the same words)
has_clickable() {
  python3 -I - "$XML" "$1" <<'PY'
import sys, xml.etree.ElementTree as ET
path, text = sys.argv[1:3]
def found(n):
    return any(m.get('text') == text for m in n.iter('node'))
sys.exit(0 if any(n.get('clickable') == 'true' and found(n) for n in ET.parse(path).iter('node')) else 1)
PY
}

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

# wait_stable <text|desc|any> <exact|sub> <needle> [timeout-seconds]: returns once the node has the same bounds in two
# consecutive dumps (it is found and no longer moving, for example while the keyboard is still hiding)
wait_stable() {
  local attr=$1 match=$2 needle=$3 timeout=${4:-10} i prev="" cur=""
  for i in $(seq 1 "$timeout"); do
    dump || return 1
    cur=$(find_node "$attr" "$match" "$needle") || cur=""
    [ -n "$cur" ] && [ "$cur" = "$prev" ] && return 0
    prev=$cur
  done
  log "'$needle' never settled"
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

tap_tab() {
  local pos
  dump
  pos=$(find_tab "$1") || { log "tab not found: $1"; return 1; }
  tap_xy $pos
}

# The Home FAB is the toggle button named "Add" (it is named "Close" while the menu is open)
open_fab_menu() {
  tap_node desc exact "Add" || return 1
  wait_for text exact "Add expense" 5
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
  adb shell dumpsys window | grep -m1 -E 'type=(navigationBars|ITYPE_NAVIGATION_BAR) ' | grep -oE 'frame=\[0,[0-9]+\]' | grep -oE '[0-9]+' | tail -1
}

ime_top() {
  adb shell dumpsys window | grep -m1 -E 'type=(ime|ITYPE_IME) frame=.*visible=true' | grep -oE 'frame=\[0,[0-9]+\]' | grep -oE '[0-9]+' | tail -1
}

crashed() { adb logcat -d | grep -q "FATAL EXCEPTION"; }

# bar_state <title text> <action content-desc>: "collapsed" when the large app bar title shares the row of its
# action button, "expanded" when it sits below it (the title is looked up in the top 40% of the screen, which
# excludes a tab label with the same text)
bar_state() {
  python3 -I - "$XML" "$1" "$2" "$(screen_height)" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, title, action, height = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
def centre(n):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    return (y1 + y2) // 2
titles = [centre(n) for n in ET.parse(path).iter('node') if n.get('text') == title and centre(n) < height * 0.4]
actions = [centre(n) for n in ET.parse(path).iter('node') if n.get('content-desc') == action]
if not titles or not actions:
    sys.exit(1)
print('collapsed' if abs(titles[0] - actions[0]) < 40 else 'expanded')
PY
}

# collapse_bar: drags the list up twice so the large app bar collapses
collapse_bar() {
  local w h
  w=$(screen_width); h=$(screen_height)
  adb shell input swipe $((w / 2)) $((h * 75 / 100)) $((w / 2)) $((h * 35 / 100)) 250 </dev/null
  sleep 0.5
  adb shell input swipe $((w / 2)) $((h * 75 / 100)) $((w / 2)) $((h * 35 / 100)) 250 </dev/null
  sleep 1
}

# check_bar <expected> <title> <action> <label>: dumps and fails the flow unless the bar is in the expected state
check_bar() {
  local got
  dump || return 1
  got=$(bar_state "$2" "$3") || { log "$4: title '$2' or action '$3' not found"; return 1; }
  [ "$got" = "$1" ] || { log "$4: app bar is $got, expected $1"; return 1; }
}

# home_title: the Period shown as the Home title (the bar title), from a fresh dump
home_title() { dump >/dev/null && home_period_label; }

# node_cy <text> -> y of the node's centre
node_cy() { find_node text exact "$1" | cut -d' ' -f2; }

# ---------- flows ----------

# Cold start through onboarding (offline account, skip accounts and categories) to Home.
flow_onboarding() {
  adb shell pm clear $PKG >/dev/null
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
  shot onboarding-login
  tap_node text exact "Offline account" || return 1
  wait_for text exact "Start fresh" || return 1
  has text exact "Import backup file" || { log "type step: import option missing"; return 1; }
  shot onboarding-type
  tap_node text exact "Start fresh" || return 1
  wait_for text exact "Set currency" || return 1
  has text exact "Pre-selected" || { log "currency step: pre-selected label missing"; return 1; }
  shot onboarding-currency
  # back from the currency step returns to the type step, and forward again
  tap_node desc exact "Back" || return 1
  wait_for text exact "Start fresh" || { log "currency back did not reach the type step"; return 1; }
  tap_node text exact "Start fresh" || return 1
  wait_for text exact "Set currency" || return 1
  tap_node text exact "Set" || return 1
  wait_for text exact "Add accounts" || return 1
  # a suggested account chip becomes a list row, the chip is gone and Next replaces Skip
  tap_node text exact "Cash" || return 1
  wait_for text exact "Next" || { log "accounts step: Next missing after adding a suggestion"; return 1; }
  has text exact "Cash" || { log "accounts step: added account row missing"; return 1; }
  has text exact "Skip" && { log "accounts step: Skip still shown with an account"; return 1; }
  find_node text exact "Cash" 1 >/dev/null 2>&1 && { log "accounts step: suggestion chip still shown"; return 1; }
  shot onboarding-accounts-added
  # the account flows later need two accounts: add the Bank suggestion as well
  tap_node text exact "Bank" || return 1
  sleep 1
  tap_node text exact "Next" || return 1
  wait_for text exact "Add categories" || return 1
  tap_node text exact "Skip" || return 1
  wait_for text exact "Home" 20 || return 1
  shot onboarding-home
  crashed && return 1
  return 0
}

open_account_edit_sheet() {
  go_home || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  tap_node text exact "Cash" || return 1
  wait_for desc exact "Edit" || return 1
  tap_node desc exact "Edit" || return 1
  wait_for text exact "Edit account" || return 1
}

# Edit account -> Amount -> Calculator, then one back per level, exactly one level closing each time.
flow_nested_back() {
  open_account_edit_sheet || return 1
  shot nested-1-account
  tap_node any sub "Enter account balance" || return 1
  wait_for text exact "Enter" || return 1
  shot nested-2-amount
  tap_node desc exact "Calculator" || return 1
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
  has desc exact "Edit" && ! has text exact "Edit account" || { log "back 3: expected the account screen"; return 1; }
  has text exact "Cash" || { log "back 3: account screen missing"; return 1; }
  shot nested-6-after-back-3
  crashed && return 1
  return 0
}

# Calculator sheet: type an expression, Close, reopen: it must start from the current amount again.
flow_calculator_reopen() {
  open_account_edit_sheet || return 1
  tap_node any sub "Enter account balance" || return 1
  wait_for text exact "Enter" || return 1
  tap_node desc exact "Calculator" || return 1
  wait_for text exact "Set" || return 1
  tap_node text exact "2" || return 1
  tap_node text exact "+" || return 1
  tap_node text exact "2" || return 1
  dump
  has text exact "2+2" || { log "expression 2+2 not shown"; return 1; }
  shot calc-reopen-1-typed
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Set" || return 1
  wait_for text exact "Enter" || return 1
  tap_node desc exact "Calculator" || return 1
  wait_for text exact "Set" || return 1
  has text exact "2+2" && { log "stale expression 2+2 after reopen"; return 1; }
  shot calc-reopen-2-fresh
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
  local close
  close=$(close_button) || { log "close button missing"; return 1; }
  set -- $close
  log "close button bottom=$6 ime top=$ime"
  [ "$6" -le "$ime" ] || { log "close button is under the keyboard"; return 1; }
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
  local close nav
  close=$(close_button) || { log "close button missing"; return 1; }
  nav=$(nav_bar_top)
  set -- $close
  log "close button bottom=$6 navbar top=$nav"
  [ -n "$nav" ] && [ "$6" -le "$nav" ] || { log "close button overlaps the navigation bar"; return 1; }
  crashed && return 1
  return 0
}

# close_top_settled: Close button top once two consecutive dumps agree (animations finished)
close_top_settled() {
  local prev="" cur i
  for i in 1 2 3 4 5 6 7 8; do
    dump || return 1
    cur=$(close_button | awk '{print $4}')
    [ -n "$cur" ] && [ "$cur" = "$prev" ] && { echo "$cur"; return 0; }
    prev=$cur
  done
  return 1
}

# raw_ok <file>: non-empty raw screencap of exactly header (16 bytes) + width * height * 4
raw_ok() {
  [ -s "$1" ] && [ "$(stat -c %s "$1")" -eq $((w * h * 4 + 16)) ]
}

# sheet_top <raw-screencap> <width> <height>: y of the sheet's top edge, scanning down the screen
# centre from 15% height for the first light pixel. Assumes a light-mode, bright sheet over a darker
# or saturated page (the run is pinned to light mode); callers treat a >= 20 px move as "hide started".
sheet_top() {
  python3 -I - "$1" "$2" "$3" <<'PY'
import sys
data = open(sys.argv[1], 'rb').read()
w, h = int(sys.argv[2]), int(sys.argv[3])
hdr = len(data) - w * h * 4
for y in range(int(h * 0.15), h):
    p = hdr + (y * w + w // 2) * 4
    if data[p] > 225 and data[p + 1] > 225 and data[p + 2] > 225:
        print(y)
        sys.exit(0)
print(-1)
PY
}

# Start closing a sheet, drag it back up while it is still hiding (cancels the hide), tap Close
# again: the sheet must close and exactly one level goes away.
# An attempt only counts when (1) the hide demonstrably started before the drag (the sheet top in a
# screenshot taken between the tap and the drag is lower than in the pre-tap one; a dump is too slow
# to observe this, the sheet is already gone by then) and (2) afterwards the sheet is back at
# the exact settled pre-tap position (Close button top unchanged).
# The hide lasts about 60 ms * ANIM_SCALE (measured: scale 20 -> 1.2 s, 100 -> 6 s). The swipe starts about
# 0.6 s after the tap (the screencap alone takes 0.3-0.5 s on a loaded host) and must touch the sheet, which
# sinks below the swipe's start (90% of the screen) near the end of the hide, so scale 20 was a coin flip on a
# slow host; 50 leaves a 3 s hide.
flow_cancelled_hide() {
  open_account_edit_sheet || return 1
  adb shell settings put global animator_duration_scale "${ANIM_SCALE:-50}"
  local w h attempt cancelled="" by1 pos after top0 top1 pulled
  w=$(screen_width); h=$(screen_height)
  for attempt in 1 2 3 4 5 6; do
    by1=$(close_top_settled) || { log "sheet never settled"; break; }
    dump || break
    pos=$(close_button) || { log "close button missing"; break; }
    set -- $pos
    rm -f "$OUT/.pre.raw" "$OUT/.mid.raw"
    adb shell rm -f /sdcard/m3-mid.raw >/dev/null 2>&1
    adb exec-out screencap >"$OUT/.pre.raw"
    adb shell "input tap $1 $2; sleep 0.2; screencap /sdcard/m3-mid.raw; input swipe $((w / 2)) $((h * 9 / 10)) $((w / 2)) $((h * 6 / 10)) ${SWIPE_MS:-250}" </dev/null
    pulled=1
    adb pull /sdcard/m3-mid.raw "$OUT/.mid.raw" >/dev/null 2>&1 || pulled=0
    top0="" top1=""
    if [ "$pulled" = 1 ] && raw_ok "$OUT/.pre.raw" && raw_ok "$OUT/.mid.raw"; then
      top0=$(sheet_top "$OUT/.pre.raw" "$w" "$h")
      top1=$(sheet_top "$OUT/.mid.raw" "$w" "$h")
    fi
    sleep 4
    dump || break
    if ! [[ $top0 =~ ^[0-9]+$ && $top1 =~ ^[0-9]+$ ]]; then
      log "attempt $attempt: no valid screenshots (top '$top0' -> '$top1'), not counted"
    elif [ $((top1 - top0)) -lt 20 ]; then
      log "attempt $attempt: sheet top $top0 -> $top1 before the drag, the hide never started"
    elif after=$(close_button) && [ "$(echo "$after" | awk '{print $4}')" -eq "$by1" ] && has text exact "Edit account"; then
      cancelled=$attempt
      break
    else
      log "attempt $attempt: hide started (sheet top $top0 -> $top1) but the sheet is not back at y=$by1"
    fi
    sleep 4
    dump || break
    if has text exact "Edit account"; then
      adb shell input keyevent KEYCODE_BACK
      sleep 3
    fi
    wait_for desc exact "Edit" || break
    tap_node desc exact "Edit" || break
    wait_for text exact "Edit account" || break
  done
  adb shell settings put global animator_duration_scale 1
  rm -f "$OUT/.pre.raw" "$OUT/.mid.raw"
  adb shell rm -f /sdcard/m3-mid.raw >/dev/null 2>&1
  [ -n "$cancelled" ] || { log "could not interrupt the hide"; return 1; }
  log "hide cancelled on attempt $cancelled (started, then sheet back at y=$by1)"
  shot cancelled-hide-1-sheet-back
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Edit account" || return 1
  sleep 1
  dump || return 1
  has desc exact "Edit" && has text exact "Cash" || { log "expected the account screen after the second close"; return 1; }
  shot cancelled-hide-2-closed
  crashed && return 1
  return 0
}

# y of the first node with this exact text, from the current dump
text_y() { find_node text exact "$1" | awk '{print $2}'; }

open_reorder_sheet() {
  go_home || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  tap_node desc exact "Reorder" || return 1
  wait_for text exact "Done" || return 1
}

# drag_first_below_second: long drag from the first row's handle ("Reorder" desc nodes: the handles) past
# the second row
drag_first_below_second() {
  local h1 h2
  dump || return 1
  h1=$(find_node desc exact "Reorder" 0) && h2=$(find_node desc exact "Reorder" 1) || { log "drag handles missing"; return 1; }
  set -- $h1
  adb shell input swipe "$1" "$2" "$1" $(($(echo $h2 | awk '{print $2}') + 80)) 900 </dev/null
  sleep 1.5
}

# Reorder sheet: swiping down must not dismiss it, a drag reorders the rows, Close discards the new
# order, Done keeps it.
flow_reorder() {
  open_reorder_sheet || return 1
  shot reorder-1-open
  local w h
  w=$(screen_width); h=$(screen_height)
  adb shell input swipe $((w / 2)) $((h * 72 / 100)) $((w / 2)) $((h * 95 / 100)) 300 </dev/null
  sleep 1.5
  dump || return 1
  has text exact "Done" && has text exact "Cash" || { log "sheet went away on swipe down"; return 1; }
  shot reorder-2-after-swipe
  drag_first_below_second || return 1
  dump || return 1
  [ "$(text_y Bank)" -lt "$(text_y Cash)" ] || { log "drag did not move Cash below Bank"; return 1; }
  shot reorder-3-dragged
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Done" || return 1
  dump || return 1
  [ "$(text_y Cash)" -lt "$(text_y Bank)" ] || { log "Close should have kept the old order"; return 1; }
  tap_node desc exact "Reorder" || return 1
  wait_for text exact "Done" || return 1
  drag_first_below_second || return 1
  tap_node text exact "Done" || return 1
  wait_gone text exact "Done" || return 1
  dump || return 1
  [ "$(text_y Bank)" -lt "$(text_y Cash)" ] || { log "Done should have kept the new order"; return 1; }
  shot reorder-4-saved
  tap_node desc exact "Reorder" || return 1
  wait_for text exact "Done" || return 1
  drag_first_below_second || return 1
  tap_node text exact "Done" || return 1
  wait_gone text exact "Done" || return 1
  crashed && return 1
  return 0
}

# home_period_label: text of the period button on Home (a month name, or "Mon, YYYY"), from the current dump
home_period_label() {
  python3 -I - "$XML" "$(screen_height)" <<'PY'
import re, sys, xml.etree.ElementTree as ET
months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
          "November", "December"]
path, height = sys.argv[1], int(sys.argv[2])
for n in ET.parse(path).iter('node'):
    text = n.get('text') or ''
    m = re.match(r'^([A-Za-z]+)(, \d{4})?$', text)
    if not m or not any(name == m.group(1) or name[:3] == m.group(1) for name in months):
        continue
    if int(re.findall(r'\d+', n.get('bounds'))[3]) < height * 0.35:
        print(text)
        sys.exit(0)
sys.exit(1)
PY
}

# next_month <chip|label> <current Home label> <year>: the chip text (or the Home label afterwards) for the month
# after the current one. The chip row starts at the current month, so the next chip is on screen; December
# continues into next year's chip.
next_month() {
  python3 -I - "$@" <<'PY'
import sys
months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
          "November", "December"]
kind, label, year = sys.argv[1], sys.argv[2], int(sys.argv[3])
i = months.index(label)
if i == 11:
    print("January, %d" % (year + 1) if kind == "chip" else "Jan, %d" % (year + 1))
else:
    print(months[i + 1])
PY
}

# Home period button -> Choose period sheet -> pick the next month -> Set: the Home label must change to it.
flow_choose_period() {
  go_home || return 1
  dump || return 1
  local label year chip expect label_after
  label=$(home_period_label) || { log "period label not found on Home"; return 1; }
  year=$(adb shell date +%Y | tr -d '\r')
  chip=$(next_month chip "$label" "$year") || return 1
  expect=$(next_month label "$label" "$year") || return 1
  log "Home period '$label', picking chip '$chip', expecting '$expect'"
  tap_node text exact "$label" || return 1
  wait_for text exact "Choose month" || return 1
  has text exact "Set" || { log "Set button missing"; return 1; }
  shot choose-period-1-open
  tap_node text exact "$chip" || return 1
  sleep 1
  shot choose-period-2-picked
  tap_node text exact "Set" || return 1
  wait_gone text exact "Choose month" || return 1
  sleep 1
  dump || return 1
  label_after=$(home_period_label) || { log "period label missing after Set"; return 1; }
  [ "$label_after" = "$expect" ] || { log "Home period is '$label_after', expected '$expect'"; return 1; }
  shot choose-period-3-home
  crashed && return 1
  return 0
}

# Regression for the crash StringIndexOutOfBoundsException (begin 0, end 3) on Home: a month of another year is titled with the
# first three characters of its name, and zh-TW names are two characters ("一月"). Switches the app language to zh-TW, picks
# January of next year (a chip that exists all year) in the Choose month sheet, Set: Home must show "一月, <year>" and not
# crash. The language is restored afterwards, and the picked period is forgotten with the next fresh start.
flow_period_other_year() {
  local rc=0
  adb shell cmd locale set-app-locales $PKG --user 0 --locales zh-TW >/dev/null 2>&1 || { log "could not set the app language"; return 1; }
  period_other_year_body || rc=$?
  adb shell am force-stop $PKG
  adb shell cmd locale set-app-locales $PKG --user 0 --locales en-US >/dev/null 2>&1
  return $rc
}

period_other_year_body() {
  fresh_start
  wait_for text exact "首頁" 20 || { log "Home not shown in zh-TW"; return 1; }
  local months=(一月 二月 三月 四月 五月 六月 七月 八月 九月 十月 十一月 十二月) year month chip i w h
  year=$(adb shell date +%Y | tr -d '\r')
  month=$(adb shell date +%-m | tr -d '\r')
  chip="一月, $((year + 1))"
  tap_node text exact "${months[month - 1]}" || return 1
  wait_for text exact "選擇月份" || return 1
  w=$(screen_width); h=$(screen_height)
  for i in $(seq 1 6); do
    has text exact "$chip" && break
    adb shell input swipe $((w * 85 / 100)) $((h * 35 / 100)) $((w * 15 / 100)) $((h * 35 / 100)) 300 </dev/null
    sleep 1
    dump || return 1
  done
  has text exact "$chip" || { log "chip '$chip' not found in the sheet"; return 1; }
  shot period-other-year-1-open
  tap_node text exact "$chip" || return 1
  sleep 1
  tap_node text exact "設定" || return 1
  wait_gone text exact "選擇月份" || return 1
  sleep 1
  crashed && { log "crash after choosing '$chip'"; return 1; }
  dump || return 1
  has text exact "$chip" || { log "Home period is not '$chip'"; return 1; }
  shot period-other-year-2-home
  return 0
}

# created_date_label: the "Mon D" text of the Created on row, from the current dump
created_date_label() {
  python3 -I - "$XML" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    text = n.get('text') or ''
    if re.match(r'^[A-Z][a-z]{2} \d{1,2}$', text):
        print(text)
        sys.exit(0)
sys.exit(1)
PY
}

# Add expense -> Created on: the date dialog opens on today, Cancel leaves the date, a picked day and OK change
# it; the time dialog opens and Cancel leaves the time.
flow_date_picker() {
  go_home || return 1
  open_fab_menu || return 1
  tap_node text exact "Add expense" || return 1
  wait_for text exact "Enter" || return 1
  tap_node desc exact "Close" || return 1
  wait_for text exact "Created on" || return 1
  local before today other other_label after
  before=$(created_date_label) || { log "created date label not found"; return 1; }
  today=$(adb shell date +%-d | tr -d '\r')
  other=15
  [ "$today" = 15 ] && other=16
  other_label="$(adb shell date +%b | tr -d '\r') $other"
  log "created on '$before', will pick day $other ('$other_label')"
  tap_node text exact "$before" || return 1
  wait_for text exact "Select date" || return 1
  has text exact "OK" && has text exact "Cancel" || { log "OK or Cancel missing"; return 1; }
  has any sub "Today, " || { log "today not marked in the dialog"; return 1; }
  shot date-picker-1-open
  tap_node text exact "Cancel" || return 1
  wait_gone text exact "Select date" || return 1
  dump || return 1
  after=$(created_date_label) || return 1
  [ "$after" = "$before" ] || { log "Cancel changed the date to '$after'"; return 1; }
  tap_node text exact "$before" || return 1
  wait_for text exact "Select date" || return 1
  tap_node any sub ", $(adb shell date +%B | tr -d '\r') $other," || return 1
  sleep 1
  shot date-picker-2-picked
  tap_node text exact "OK" || return 1
  wait_gone text exact "Select date" || return 1
  dump || return 1
  after=$(created_date_label) || return 1
  [ "$after" = "$other_label" ] || { log "date is '$after', expected '$other_label'"; return 1; }
  shot date-picker-3-changed
  tap_node any sub " AM" 0 || tap_node any sub " PM" 0 || return 1
  wait_for text exact "Select Time" || return 1
  shot date-picker-4-time
  tap_node text exact "Cancel" || return 1
  wait_gone text exact "OK" || return 1
  crashed && return 1
  return 0
}

# Home period -> Choose period -> From: Cancel keeps "Add date", a picked day and OK fill it in.
flow_period_date_picker() {
  go_home || return 1
  dump || return 1
  local label
  label=$(home_period_label) || { log "period label not found on Home"; return 1; }
  tap_node text exact "$label" || return 1
  wait_for text exact "Choose month" || return 1
  tap_node text exact "Add date" 0 || return 1
  wait_for text exact "Select date" || return 1
  tap_node text exact "Cancel" || return 1
  wait_gone text exact "Select date" || return 1
  dump || return 1
  has text exact "Add date" 1 || { log "Cancel filled in a date"; return 1; }
  tap_node text exact "Add date" 0 || return 1
  wait_for text exact "Select date" || return 1
  tap_node any sub ", $(adb shell date +%B | tr -d '\r') 15," || return 1
  tap_node text exact "OK" || return 1
  wait_gone text exact "Select date" || return 1
  dump || return 1
  has text exact "Add date" 0 && ! has text exact "Add date" 1 || { log "OK did not fill in the From date"; return 1; }
  shot period-date-picker
  crashed && return 1
  return 0
}

# Home FAB: tap opens the menu, back or a scrim tap collapses it (still on Home), reopen, Add expense opens the edit screen;
# the Categories tab shows its own add button.
flow_fab_menu() {
  go_home || return 1
  has desc exact "Add" || { log "FAB missing on Home"; return 1; }
  shot fab-1-collapsed
  open_fab_menu || return 1
  has text exact "Add expense" && has text exact "Add income" && has text exact "Transfer" && has text exact "Add planned payment" || { log "menu items missing"; return 1; }
  has desc exact "Close" || { log "FAB should read Close while expanded"; return 1; }
  local ys
  ys=$(for t in "Transfer" "Add expense" "Add income" "Add planned payment"; do find_node text exact "$t" | cut -d' ' -f2; done | tr '\n' ' ')
  [ "$(echo $ys | tr ' ' '\n' | sort -n | tr '\n' ' ')" = "$ys" ] || { log "menu order wrong (y: $ys), expected Transfer, Add expense, Add income, Add planned payment top to bottom"; return 1; }
  shot fab-2-expanded
  back
  wait_gone text exact "Add expense" || { log "back did not collapse the menu"; return 1; }
  has text exact "Home" && has desc exact "Add" || { log "back left the main screen"; return 1; }
  shot fab-3-collapsed-by-back
  open_fab_menu || return 1
  # a tap on the scrim, above the menu items, collapses the menu and stays on Home
  tap_xy $(($(screen_width) / 2)) $(($(screen_height) / 4))
  wait_gone text exact "Add expense" || { log "tap on the scrim did not collapse the menu"; return 1; }
  has text exact "Home" && has desc exact "Add" || { log "scrim tap left the main screen"; return 1; }
  open_fab_menu || return 1
  tap_node text exact "Add expense" || return 1
  wait_for text exact "Enter" || return 1
  tap_node desc exact "Close" || return 1
  wait_for text exact "Created on" || return 1
  shot fab-4-edit-transaction
  back
  wait_for text exact "Home" || return 1
  wait_for desc exact "Add" || return 1
  # Menu open, switch to Accounts: the menu must not stay armed. One Back on Accounts is handled by the app (it
  # returns to the Home tab); it must not be swallowed by a stale menu, and Home must show the menu collapsed.
  open_fab_menu || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  has desc exact "Add" && { log "Home FAB shown on Accounts"; return 1; }
  back
  dump || return 1
  has text exact "Counted in total" && { log "Back was swallowed on Accounts (stale menu state)"; return 1; }
  has desc exact "Add" && ! has text exact "Add expense" || { log "Home should show the collapsed menu"; return 1; }
  shot fab-5-home-after-accounts
  tap_tab Categories || return 1
  wait_for desc exact "Add category" || { log "Categories tab did not show"; return 1; }
  has desc exact "Add" && { log "Home FAB shown on Categories"; return 1; }
  shot fab-6-categories-tab
  crashed && return 1
  return 0
}

# Leaves a transaction behind (the account balance changes), so it runs last in the default list.
# Home FAB -> Add expense: enter an amount in the amount sheet, pick a category, the bottom bar must stay above the
# keyboard, Add saves and the row shows on Home.
flow_add_expense() {
  go_home || return 1
  open_fab_menu || return 1
  tap_node text exact "Add expense" || return 1
  wait_for text exact "Enter" || return 1
  tap_node text exact "1" || return 1
  tap_node text exact "2" || return 1
  tap_node text exact "5" || return 1
  shot add-expense-1-amount
  tap_node text exact "Enter" || return 1
  wait_for text exact "Choose category" || return 1
  tap_node text exact "Groceries" || return 1
  wait_for text exact "Add expense" || return 1
  wait_for text exact "125.00 USD" || { log "amount missing from the bottom bar"; return 1; }
  local ime="" i
  for i in 1 2 3 4 5 6 7 8; do
    sleep 1
    ime=$(ime_top)
    [ "${ime:-0}" -gt 0 ] && break
  done
  [ "${ime:-0}" -gt 0 ] || { log "title field did not take focus after the category"; return 1; }
  sleep 1
  shot add-expense-2-keyboard
  dump
  local bar
  bar=$(find_node text exact "125.00 USD") || return 1
  set -- $bar
  log "amount bottom=$6 ime top=$ime"
  [ "$6" -le "$ime" ] || { log "bottom bar is under the keyboard"; return 1; }
  adb shell input text smokecoffee
  sleep 1
  tap_node text exact "Add" || return 1
  wait_for text exact "-125.00 USD" || { log "saved row missing on Home"; return 1; }
  shot add-expense-3-home
  crashed && return 1
  return 0
}

# ensure_seed: the flows that read the smokecoffee expense of flow_add_expense call this first; it runs that flow when
# Home does not list the expense yet, so they also pass on their own from a fresh install
ensure_seed() {
  go_home || return 1
  wait_for text exact "smokecoffee" 4 && return 0
  log "seeding the smokecoffee expense"
  flow_add_expense || return 1
  go_home
}

# Accounts -> Cash -> the Expenses card opens the pie chart: title, back, add, period buttons and the seeded Groceries
# row; Previous shows the empty state and Next brings the row back; the row opens the category transactions; back twice
# returns to the account. Seeds the Groceries expense through flow_add_expense when Home lacks it.
flow_piechart() {
  ensure_seed || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  tap_node text exact "Cash" || return 1
  wait_for desc exact "Edit" || return 1
  tap_node text exact "Expenses" || return 1
  wait_for desc exact "Add expense" || { log "pie chart screen did not open"; return 1; }
  has desc exact "Back" && has desc exact "Next" && has desc exact "Previous" || { log "pie chart controls missing"; return 1; }
  wait_for text exact "Groceries" || { log "category row missing: run flow_add_expense first"; return 1; }
  shot piechart-1
  tap_node desc exact "Previous" || return 1
  wait_for text exact "No transactions" || { log "empty state missing for the previous month"; return 1; }
  shot piechart-2-empty
  tap_node desc exact "Next" || return 1
  wait_for text exact "Groceries" || { log "row missing after Next"; return 1; }
  tap_node text exact "Groceries" || return 1
  wait_for desc exact "Edit" || { log "category transactions did not open"; return 1; }
  shot piechart-3-category
  back
  wait_for desc exact "Add expense" || { log "back from the category should return to the pie chart"; return 1; }
  back
  wait_for desc exact "Edit" || { log "back from the pie chart should return to the account"; return 1; }
  has text exact "Cash" || { log "expected the account screen"; return 1; }
  crashed && return 1
  return 0
}

# Edit account -> Amount sheet -> type a number, Enter: one press closes the amount sheet (it must not be recreated
# when the value changes), the account sheet stays open with the new amount, Back closes only the account sheet.
flow_amount_sheet_close() {
  open_account_edit_sheet || return 1
  tap_node any sub "Enter account balance" || return 1
  wait_for text exact "Enter" || return 1
  tap_node text exact "7" || return 1
  tap_node text exact "Enter" || return 1
  wait_gone text exact "Enter" 5 || { log "one Enter press did not close the amount sheet"; return 1; }
  has text exact "Edit account" || { log "account sheet closed with the amount sheet"; return 1; }
  has any sub "7.00" || { log "new amount missing from the account sheet"; return 1; }
  shot amount-close-1-account
  back
  wait_gone text exact "Edit account" 5 || { log "Back did not close the account sheet"; return 1; }
  has text exact "Enter" && { log "amount sheet reappeared"; return 1; }
  crashed && return 1
  return 0
}

# Home more button: opens the More sheet, an item navigates (and back returns to the sheet), Close dismisses it,
# the Savings goal row opens the Buffer sheet on top and back closes only that one.
flow_more_menu() {
  go_home || return 1
  has desc exact "More" && has desc exact "Search transactions" || { log "more or search button missing on Home"; return 1; }
  shot more-1-home
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  has text exact "Settings" && has text exact "Loans" || { log "menu items missing"; return 1; }
  has text exact "Search transactions..." && { log "Search moved to the Home top bar, the More sheet must not list it"; return 1; }
  shot more-2-sheet
  tap_node text exact "Settings" || return 1
  wait_for text exact "About & Support" || return 1
  back
  wait_for text exact "Quick access" || { log "back from Settings should return to the open More sheet"; return 1; }
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Quick access" || { log "Close did not dismiss the sheet"; return 1; }
  has desc exact "More" || { log "Home missing after Close"; return 1; }
  tap_node desc exact "More" || return 1
  wait_for text exact "Savings goal" || return 1
  tap_node text exact "Savings goal" || return 1
  wait_for text exact "Save" || return 1
  shot more-3-buffer
  back
  wait_gone text exact "Save" || { log "back did not close the Buffer sheet"; return 1; }
  has text exact "Quick access" || { log "back closed the More sheet too"; return 1; }
  back
  wait_gone text exact "Quick access" || { log "back did not close the More sheet"; return 1; }
  shot more-4-closed
  crashed && return 1
  return 0
}

# period_label_anywhere: text of the first period button (a month name, or "Mon, YYYY") in the current dump
period_label_anywhere() {
  python3 -I - "$XML" <<'PY'
import re, sys, xml.etree.ElementTree as ET
months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
          "November", "December"]
for n in ET.parse(sys.argv[1]).iter('node'):
    text = n.get('text') or ''
    m = re.match(r'^([A-Za-z]+)(, \d{4})?$', text)
    if m and any(name == m.group(1) or name[:3] == m.group(1) for name in months):
        print(text)
        sys.exit(0)
sys.exit(1)
PY
}

# Accounts -> Cash: the detail screen shows the name, balance, income and expense cards with their add buttons and the
# period row; Next and Previous change the period and come back, Edit opens the account sheet and Close dismisses it,
# back returns to Accounts.
flow_account_detail() {
  go_home || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  tap_node text exact "Cash" || return 1
  wait_for desc exact "Edit" || return 1
  has desc exact "Delete" && has text exact "Add income" && has text exact "Add expense" || { log "detail actions missing"; return 1; }
  has text exact "Cash" || { log "account name missing"; return 1; }
  shot account-detail-1
  local before after
  before=$(period_label_anywhere) || { log "period label not found"; return 1; }
  tap_node desc exact "Next" || return 1
  sleep 1
  dump || return 1
  after=$(period_label_anywhere) || { log "period label missing after Next"; return 1; }
  [ "$after" != "$before" ] || { log "Next did not change the period ('$before')"; return 1; }
  shot account-detail-2-next
  tap_node desc exact "Previous" || return 1
  sleep 1
  dump || return 1
  after=$(period_label_anywhere) || { log "period label missing after Previous"; return 1; }
  [ "$after" = "$before" ] || { log "Previous gave '$after', expected '$before'"; return 1; }
  tap_node desc exact "Edit" || return 1
  wait_for text exact "Edit account" || return 1
  shot account-detail-3-edit
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Edit account" || return 1
  has desc exact "Edit" && has text exact "Cash" || { log "expected the account screen after Close"; return 1; }
  back
  wait_for text exact "Counted in total" || { log "back did not return to Accounts"; return 1; }
  crashed && return 1
  return 0
}

# db_edit seed|clear|pay_upcoming|pay_clear|planned_seed|planned_clear: edits the app database of the debuggable build through run-as while the app is stopped. `seed` adds
# 30 "Scroll row" transactions per day of the current month to the account Cash (every third one a transfer to Bank, some
# with a description), `clear` removes them again; `pay_upcoming` and `pay_clear` belong to flow_pay_planned, `planned_seed` and `planned_clear` to flow_planned_scroll. Needs the app to have an offline account with Cash and Bank.
db_edit() {
  local mode=$1 tmp f now tz
  tmp=$(mktemp -d) || return 1
  now=$(adb shell date +%s | tr -d '\r')
  tz=$(adb shell date +%z | tr -d '\r')
  adb shell am force-stop $PKG
  for f in ivywallet.db ivywallet.db-wal ivywallet.db-shm; do
    adb exec-out run-as $PKG cat databases/$f >"$tmp/$f" 2>/dev/null
    [ -s "$tmp/$f" ] || rm -f "$tmp/$f"
  done
  [ -f "$tmp/ivywallet.db" ] || { log "db_edit: no database, is $PKG a debuggable build?"; rm -rf "$tmp"; return 1; }
  python3 -I - "$tmp/ivywallet.db" "$mode" "$now" "$tz" <<'PY' || { rm -rf "$tmp"; return 1; }
import datetime, sqlite3, sys, uuid
path, mode, now, tz = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
offset = datetime.timedelta(hours=int(tz[:3]), minutes=int(tz[0] + tz[3:5]))
db = sqlite3.connect(path)
if mode == "clear":
    db.execute("DELETE FROM transactions WHERE title = 'Scroll row'")
elif mode == "pay_upcoming":
    # planned payments are created due now, which is neither Overdue (before today) nor Upcoming (after now): move the
    # first occurrence of the ones named payover* two days back and of payup* an hour ahead (a recurring rule has later
    # occurrences as well)
    db.execute("UPDATE transactions SET dueDate = ? WHERE title LIKE 'payover%' AND dueDate IS NOT NULL AND dueDate <= ?",
               ((now - 2 * 86400) * 1000, now * 1000))
    db.execute("UPDATE transactions SET dueDate = ? WHERE title LIKE 'payup%' AND dueDate IS NOT NULL AND dueDate <= ?",
               ((now + 3600) * 1000, now * 1000))
elif mode == "planned_seed":
    # 100 recurring and 100 one-time planned payments with a category and an account chip, for flow_planned_scroll
    ids = dict(db.execute("SELECT name, id FROM accounts WHERE name IN ('Cash', 'Bank')").fetchall())
    cat = str(uuid.uuid4())
    db.execute("INSERT INTO categories (name, color, icon, orderNum, isSynced, isDeleted, id) VALUES ('Plan seed', -16711936, NULL, 99.0, 0, 0, ?)", (cat,))
    start = (now + 3 * 86400) * 1000
    rows = []
    for i in range(200):
        one_time = i % 2
        rows.append((
            start + i * 3600000, None if one_time else 1, None if one_time else "MONTH", one_time, "EXPENSE" if i % 3 else "INCOME",
            ids["Cash"] if i % 4 else ids["Bank"], float(i + 1), cat, "Planned row %d" % i,
            "A longer description" if i % 5 == 0 else None, 0, 0, str(uuid.uuid4()),
        ))
    db.executemany(
        "INSERT INTO planned_payment_rules (startDate, intervalN, intervalType, oneTime, type, accountId, amount, categoryId, title,"
        " description, isSynced, isDeleted, id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows)
elif mode == "planned_clear":
    db.execute("DELETE FROM planned_payment_rules WHERE title LIKE 'Planned row %'")
    db.execute("DELETE FROM categories WHERE name = 'Plan seed'")
elif mode == "pay_clear":
    db.execute("DELETE FROM transactions WHERE title LIKE 'payup%' OR title LIKE 'payover%'")
else:
    ids = dict(db.execute("SELECT name, id FROM accounts WHERE name IN ('Cash', 'Bank')").fetchall())
    local_now = datetime.datetime.fromtimestamp(now, datetime.timezone(offset))
    rows = []
    for day in range(1, local_now.day + 1):
        midnight = local_now.replace(day=day, hour=0, minute=0, second=0, microsecond=0)
        span = (local_now - midnight).total_seconds() if day == local_now.day else 86399
        for i in range(30):
            millis = int((midnight.timestamp() + span * i / 30) * 1000)
            transfer = i % 3 == 2
            rows.append((
                ids["Cash"], "TRANSFER" if transfer else "EXPENSE", float(i + 1), ids["Bank"] if transfer else None,
                "Scroll row", "A longer description that wraps onto a second line of the row" if i % 2 else None,
                millis, 0, 0, str(uuid.uuid4()),
            ))
    db.executemany(
        "INSERT INTO transactions (accountId, type, amount, toAccountId, title, description, dateTime, isSynced, isDeleted, id)"
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows)
db.commit()
db.execute("PRAGMA wal_checkpoint(TRUNCATE)")
db.close()
PY
  adb shell "run-as $PKG sh -c 'rm -f databases/ivywallet.db-wal databases/ivywallet.db-shm; cat > databases/ivywallet.db'" <"$tmp/ivywallet.db"
  local rc=$?
  rm -rf "$tmp"
  return $rc
}

# Regression for the crash "LayoutNode not found in RectList" (Compose 1.12) when an account's long transaction list scrolls:
# M3 SegmentedListItem read baselines of the chips in its rows while measuring. Seeds a month of transactions into Cash, opens
# Accounts -> Cash, flings the list down and up, and fails when the app dies. Removes the seeded transactions afterwards.
flow_account_scroll() {
  db_edit seed || return 1
  local rc=0
  account_scroll_body || rc=$?
  db_edit clear || rc=1
  return $rc
}

account_scroll_body() {
  go_home || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  tap_node text exact "Cash" || return 1
  wait_for text sub "Scroll row" 20 || { log "seeded rows missing on the account screen"; return 1; }
  local w h i
  w=$(screen_width); h=$(screen_height)
  for i in $(seq 1 16); do
    if [ $((i % 4)) = 0 ]; then
      adb shell input swipe $((w / 2)) $((h * 25 / 100)) $((w / 2)) $((h * 80 / 100)) 120 </dev/null
    else
      adb shell input swipe $((w / 2)) $((h * 80 / 100)) $((w / 2)) $((h * 25 / 100)) 120 </dev/null
    fi
    sleep 0.6
    crashed && { log "crash while scrolling (swipe $i)"; return 1; }
  done
  shot account-scroll
  dump || return 1
  has desc exact "Delete" && has text sub "Scroll row" || { log "account screen not on screen after scrolling"; return 1; }
  back
  wait_for text exact "Counted in total" || { log "back did not return to Accounts"; return 1; }
  return 0
}

# Regression for the same crash as flow_account_scroll ("LayoutNode not found in RectList") on the Planned payments list,
# whose rows put category and account chips in a SegmentedListItem. Seeds 200 planned payments (Cash and Bank), opens
# More -> Planned payments, flings the list down and up, and fails when the app dies. Removes the seeded rows afterwards.
flow_planned_scroll() {
  db_edit planned_seed || return 1
  local rc=0
  planned_scroll_body || rc=$?
  adb shell am force-stop $PKG
  db_edit planned_clear || rc=1
  return $rc
}

planned_scroll_body() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Planned payments" || return 1
  wait_for text sub "Planned row" 20 || { log "seeded planned payments missing from the list"; return 1; }
  local w h i
  w=$(screen_width); h=$(screen_height)
  for i in $(seq 1 40); do
    if [ $((i % 4)) = 0 ]; then
      adb shell input swipe $((w / 2)) $((h * 25 / 100)) $((w / 2)) $((h * 80 / 100)) 90 </dev/null
    else
      adb shell input swipe $((w / 2)) $((h * 80 / 100)) $((w / 2)) $((h * 25 / 100)) 90 </dev/null
    fi
    sleep 0.6
    crashed && { log "crash while scrolling the planned payments (swipe $i)"; return 1; }
  done
  shot planned-scroll
  dump || return 1
  has text sub "Planned row" || { log "planned payments not on screen after scrolling"; return 1; }
  return 0
}

# Home top bar -> Search transactions: type the title of the seeded expense (flow_add_expense), its row is listed under the
# search field, Clear search empties the field, back leaves the screen. Seeds its expense through flow_add_expense when needed.
flow_search() {
  ensure_seed || return 1
  tap_node desc exact "Search transactions" || return 1
  wait_for class exact android.widget.EditText || return 1
  adb shell input text smokecoffee
  wait_for desc exact "Clear search" || { log "clear button missing after typing"; return 1; }
  wait_for text exact "smokecoffee" || return 1
  has text exact "smokecoffee" 1 || { log "result row missing: run flow_add_expense first, it seeds the smokecoffee transaction"; return 1; }
  shot search-1-result
  # hide the keyboard, tap the field again: the keyboard must return, the bar stay usable and typing still filter
  adb shell input keyevent KEYCODE_BACK
  sleep 1.5
  dump || return 1
  has class exact android.widget.EditText && has desc exact "Clear search" || { log "back with the keyboard open left the search screen"; return 1; }
  [ -z "$(ime_top)" ] || { log "keyboard still visible after back"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  local ime="" k
  for k in 1 2 3 4 5 6 7 8; do
    sleep 1
    ime=$(ime_top)
    [ "${ime:-0}" -gt 0 ] && break
  done
  [ "${ime:-0}" -gt 0 ] || { log "tapping the search field did not bring the keyboard back"; return 1; }
  shot search-1b-keyboard-back
  adb shell input text x
  wait_gone text exact "smokecoffee" 5 || wait_for text exact "smokecoffeex" 5 || return 1
  has class exact android.widget.EditText && has desc exact "Clear search" || { log "search bar lost after tapping the field"; return 1; }
  has text exact "smokecoffee" 1 && { log "typing after the re-tap did not filter"; return 1; }
  # the keyboard's Search action (bottom right key) hides the keyboard, tapping the field brings it back
  tap_xy $(($(screen_width) * 92 / 100)) $(($(screen_height) * 908 / 1000))
  sleep 2
  [ -z "$(ime_top)" ] || { log "the keyboard's Search action did not hide the keyboard"; return 1; }
  has class exact android.widget.EditText || { log "search bar lost after the Search action"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  for k in 1 2 3 4 5 6 7 8; do
    sleep 1
    ime=$(ime_top)
    [ "${ime:-0}" -gt 0 ] && break
  done
  [ "${ime:-0}" -gt 0 ] || { log "tapping the field after the Search action did not bring the keyboard back"; return 1; }
  tap_node desc exact "Clear search" || return 1
  wait_gone desc exact "Clear search" 5 || { log "Clear search did not empty the field"; return 1; }
  adb shell input text zzzznomatch
  wait_for text sub "no transactions" 5 || wait_for text exact "No transactions" 5 || { log "empty state missing for an unmatched query"; return 1; }
  shot search-2-empty
  local i
  for i in 1 2 3; do
    back
    dump || return 1
    has class exact android.widget.EditText || break
  done
  has class exact android.widget.EditText && { log "back did not leave the search screen"; return 1; }
  crashed && return 1
  return 0
}

# Home More -> Planned payments: the list has a back button and the add button; add one (expense, amount sheet,
# category, recurring rule sheet, title, Set), it is listed under Recurring payments, back returns to Home.
flow_planned_payments() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Planned payments" || return 1
  wait_for desc exact "Add planned payment" || return 1
  has desc exact "Back" || { log "back button missing on the planned payments list"; return 1; }
  shot planned-1-list
  tap_node desc exact "Add planned payment" || return 1
  wait_for text exact "Choose payment type" || return 1
  tap_node text exact "Expense" || return 1
  wait_for text exact "Enter" || return 1
  tap_node text exact "3" || return 1
  tap_node text exact "0" || return 1
  tap_node text exact "Enter" || return 1
  wait_for text exact "Choose category" || return 1
  tap_node text exact "Groceries" || return 1
  wait_for text exact "Plan for" || return 1
  shot planned-2-rule
  tap_node text exact "Set" || return 1
  wait_gone text exact "Plan for" || { log "the recurring rule sheet did not close"; return 1; }
  adb shell input text smokeplanned
  sleep 1
  shot planned-3-edit
  tap_node text exact "Set" || return 1
  wait_for text exact "Recurring payments" || { log "recurring section missing"; return 1; }
  dump || return 1
  has text exact "smokeplanned" || { log "saved planned payment missing from the list"; return 1; }
  has text exact "-30.00 USD" || { log "amount missing in the list"; return 1; }
  shot planned-4-list-item
  # leave nothing behind: a planned payment that is due would fill the top of Home for the flows after this one
  delete_planned_payment smokeplanned || return 1
  back
  wait_for text exact "Quick access" || { log "back from the list should return to the open More sheet"; return 1; }
  crashed && return 1
  return 0
}

# add_planned_payment <title> <digit> [one_time]: from the open planned payments list, adds an expense of <digit>0 (amount
# sheet, Groceries, recurring rule sheet with One time chosen when asked, title, Set) and waits for the list again.
add_planned_payment() {
  tap_node desc exact "Add planned payment" || return 1
  wait_for text exact "Choose payment type" || return 1
  tap_node text exact "Expense" || return 1
  wait_for text exact "Enter" || return 1
  tap_node text exact "$2" || return 1
  tap_node text exact "0" || return 1
  tap_node text exact "Enter" || return 1
  wait_for text exact "Choose category" || return 1
  tap_node text exact "Groceries" || return 1
  wait_for text exact "Plan for" || return 1
  [ "${3:-}" = one_time ] && { tap_node text exact "One time" || return 1; }
  tap_node text exact "Set" || return 1
  wait_gone text exact "Plan for" || return 1
  adb shell input text "$1"
  sleep 1
  tap_node text exact "Set" || return 1
  wait_for text exact "Recurring payments" || { log "recurring section missing after adding $1"; return 1; }
}

# delete_planned_payment <title>: from the open planned payments list, opens the item, Delete, confirms, and waits
# until it is gone from the list
delete_planned_payment() {
  tap_node text exact "$1" || return 1
  wait_for text exact "Edit planned payment" || return 1
  tap_node desc exact "Delete" || return 1
  wait_for text exact "Confirm deletion" || return 1
  tap_node text exact "Delete" || return 1
  wait_gone text exact "$1" 10 || { log "planned payment '$1' still listed after deleting it"; return 1; }
}

# Home Due section: four planned payments are due, so Home shows the Due header and only the first 3 rows with
# "Show all (N)"; Show all lists every row and turns into "Show less", which collapses again. Deletes its planned
# payments again at the end.
flow_due_section() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Planned payments" || return 1
  wait_for desc exact "Add planned payment" || return 1
  add_planned_payment duealpha 1 || return 1
  add_planned_payment duebeta 2 || return 1
  add_planned_payment duegamma 3 || return 1
  add_planned_payment duedelta 4 || return 1
  go_home || return 1
  wait_for text exact "Due" || { log "Due header missing on Home"; return 1; }
  local w h i
  w=$(screen_width); h=$(screen_height)
  for i in 1 2 3; do
    dump
    has text sub "Show all (" && break
    adb shell input swipe $((w / 2)) $((h * 75 / 100)) $((w / 2)) $((h * 45 / 100)) 250 </dev/null
    sleep 1
  done
  has text sub "Show all (" || { log "Show all (N) missing with four due payments"; return 1; }
  shot due-1-collapsed
  [ "$(python3 -I - "$XML" <<'PY'
import sys, xml.etree.ElementTree as ET
print(sum(1 for n in ET.parse(sys.argv[1]).iter('node') if n.get('text') == 'Skip'))
PY
)" -le 3 ] || { log "collapsed Due shows more than 3 rows"; return 1; }
  tap_node text sub "Show all (" || return 1
  wait_for text exact "Show less" || { log "Show less missing after expanding"; return 1; }
  shot due-2-expanded
  tap_node text exact "Show less" || return 1
  wait_for text sub "Show all (" || { log "Show all (N) missing after collapsing"; return 1; }
  # clean up so the flows after this one see the same Home as without it
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Planned payments" || return 1
  wait_for desc exact "Add planned payment" || return 1
  local t
  for t in duealpha duebeta duegamma duedelta; do
    delete_planned_payment $t || return 1
  done
  back
  wait_for text exact "Quick access" || return 1
  back
  wait_gone text exact "Quick access" || return 1
  crashed && return 1
  return 0
}

# tap_pay <title>: taps the Pay (or Get) button of the Due row titled <title>, the nearest button below its title
tap_pay() {
  dump || return 1
  local pos
  pos=$(python3 -I - "$XML" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, title = sys.argv[1:3]
nodes = list(ET.parse(path).iter('node'))
def box(n):
    return tuple(map(int, re.findall(r'\d+', n.get('bounds'))))
titles = [box(n) for n in nodes if n.get('text') == title]
if not titles:
    sys.exit(1)
ty = titles[0][1]
buttons = [box(n) for n in nodes if n.get('text') in ('Pay', 'Get') and box(n)[1] > ty]
if not buttons:
    sys.exit(1)
x1, y1, x2, y2 = min(buttons, key=lambda b: b[1])
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
) || { shot pay-nobutton; log "no Pay button below '$1'"; return 1; }
  tap_xy $pos
}

# pay_row <title>: taps Pay on the row, then fails when the app died or the row still has its Pay button (tap_pay dumps)
pay_row() {
  tap_pay "$1" || return 1
  sleep 2
  crashed && { log "crash after paying '$1'"; return 1; }
  tap_pay "$1" 2>/dev/null && { log "'$1' still has a Pay button after paying it"; return 1; }
  return 0
}

# Pressing Pay on planned payments, one-time and recurring, Overdue and Upcoming, on the account screen (payupone,
# payuprec, payoverone) and on Home (payoverrec).
# Regression for the Home crash 'Key "<id>" was already used' (LazyColumn): the paid transaction was in the history and
# in the Due rows for a frame. Creates four planned payments, removes them and what they paid again at the end.
flow_pay_planned() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Planned payments" || return 1
  wait_for desc exact "Add planned payment" || return 1
  add_planned_payment payoverrec 1 || return 1
  add_planned_payment payupone 2 one_time || return 1
  add_planned_payment payuprec 3 || return 1
  add_planned_payment payoverone 4 one_time || return 1
  adb shell am force-stop $PKG
  db_edit pay_upcoming || return 1
  local rc=0
  pay_planned_body || rc=$?
  adb shell am force-stop $PKG
  db_edit pay_clear || rc=1
  return $rc
}

pay_planned_body() {
  # The account screen lists all four payments (Upcoming, then Overdue). payoverrec is paid last, on Home, where it is the
  # only Due row left and its history row is on screen below it (the duplicate key only throws for composed rows).
  go_home || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  tap_node text exact "Cash" || return 1
  # Overdue is open, Upcoming is collapsed until its header is tapped; paying the Upcoming rows brings Overdue up. Two
  # days back is in the month's period unless it is the 1st or 2nd; then payoverone stays unpaid and is deleted below.
  local overdue_in_period=1
  [ "$(date +%-d)" -le 2 ] && overdue_in_period=0
  if [ $overdue_in_period = 1 ]; then
    wait_for text exact "payoverone" 20 || { log "overdue payment missing on the account screen"; return 1; }
  fi
  wait_for text exact "Upcoming" 20 || return 1
  # the chevron at the end of the header row opens it
  tap_xy $(( $(screen_width) - 100 )) "$(node_cy Upcoming)" || return 1
  wait_for text exact "payupone" || { log "upcoming payment missing after opening Upcoming"; return 1; }
  shot pay-1-account
  pay_row payupone || return 1
  pay_row payuprec || return 1
  [ $overdue_in_period = 1 ] && { pay_row payoverone || return 1; }
  shot pay-2-account-paid
  go_home || return 1
  wait_for text exact "Due" 20 || { log "Due header missing on Home"; return 1; }
  local w h
  w=$(screen_width); h=$(screen_height)
  # scroll the Due row up (a tip card can fill the screen above it) so the history rows below it are on screen
  adb shell input swipe $((w / 2)) $((h * 80 / 100)) $((w / 2)) $((h * 30 / 100)) 250 </dev/null
  sleep 1
  shot pay-3-home
  pay_row payoverrec || return 1
  shot pay-4-home-paid
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Planned payments" || return 1
  wait_for desc exact "Add planned payment" || return 1
  dump || return 1
  has text exact "payoverrec" && has text exact "payuprec" || { log "recurring rules missing from the list"; return 1; }
  has text exact "payupone" && { log "paid one-time rule payupone still listed"; return 1; }
  if [ $overdue_in_period = 1 ]; then
    has text exact "payoverone" && { log "paid one-time rule payoverone still listed"; return 1; }
  else
    delete_planned_payment payoverone || return 1
  fi
  delete_planned_payment payoverrec || return 1
  delete_planned_payment payuprec || return 1
  back
  wait_for text exact "Quick access" || return 1
  crashed && return 1
  return 0
}

# Home More -> Reports: empty state with Set Filter, the filter sheet (Apply stays disabled until the filter is valid),
# pick the Expenses type, the Cash account, the Groceries category and the default period, Apply: the seeded
# smokecoffee expense is listed; back returns to the open More sheet. Seeded on demand by flow_add_expense.
flow_reports() {
  ensure_seed || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Reports" || return 1
  wait_for desc exact "Filter" || return 1
  has desc exact "Back" && has desc exact "Export" || { log "reports top bar buttons missing"; return 1; }
  has text exact "No Filter" && has text exact "Set Filter" || { log "no-filter empty state missing"; return 1; }
  shot reports-1-empty
  tap_node text exact "Set Filter" || return 1
  wait_for text exact "By Type" || { log "filter sheet did not open"; return 1; }
  has text exact "Apply filter" && has text exact "Clear filter" || { log "filter sheet actions missing"; return 1; }
  shot reports-2-sheet
  local n
  n=$(find_node text exact "Expenses" 1 >/dev/null 2>&1 && echo 1 || echo 0)
  tap_node text exact "Expenses" "$n" || return 1
  tap_node text exact "Cash" || return 1
  tap_node text exact "Groceries" || return 1
  tap_node text exact "Select time range" || return 1
  wait_for text exact "Choose month" || { log "period sheet did not open"; return 1; }
  tap_node text exact "Set" || return 1
  wait_gone text exact "Choose month" || return 1
  sleep 1
  shot reports-3-filled
  tap_node text exact "Apply filter" || return 1
  wait_gone text exact "By Type" || { log "Apply did not close the sheet"; return 1; }
  wait_for text exact "smokecoffee" 20 || { log "matching row missing: run flow_add_expense first, it seeds the smokecoffee transaction"; return 1; }
  shot reports-4-result
  back
  wait_for text exact "Quick access" || { log "back from Reports should return to the open More sheet"; return 1; }
  crashed && return 1
  return 0
}

# Home Type filter chips: Expense keeps the seeded smokecoffee expense, Income matches nothing and shows the empty
# message, All brings the row back. Seeds the smokecoffee expense through flow_add_expense when needed.
flow_type_filter() {
  ensure_seed || return 1
  wait_for text exact "smokecoffee" || { log "run flow_add_expense first"; return 1; }
  has text exact "All" || { log "All chip missing"; return 1; }
  # The Balance banner has an "Income" figure label above the chip row (it opens the pie chart): the chip is the second match.
  has text exact "Income" 1 || { log "Income chip missing"; return 1; }
  tap_node text exact "Income" 1 || return 1
  wait_for text exact "No matching transactions for the selected filter." || { log "empty message missing under Income"; return 1; }
  has text exact "smokecoffee" && { log "expense still listed under Income"; return 1; }
  shot type-filter-1-income
  tap_node text exact "Expense" || return 1
  wait_for text exact "smokecoffee" || { log "expense missing under Expense"; return 1; }
  has text exact "No matching transactions for the selected filter." && { log "empty message under Expense"; return 1; }
  shot type-filter-2-expense
  tap_node text exact "All" || return 1
  wait_for text exact "smokecoffee" || { log "row missing under All"; return 1; }
  crashed && return 1
  return 0
}

# The large app bar keeps its collapsed state (and the list its position) across tabs and back navigation, and a
# screen opened again starts expanded. Seeds the smokecoffee row through flow_add_expense when needed; needs the Cash account.
flow_appbar_state() {
  ensure_seed || return 1
  wait_for text exact "smokecoffee" || { log "run flow_add_expense first"; return 1; }
  check_bar expanded "$(home_title)" "More" "Home at start" || return 1
  collapse_bar
  check_bar collapsed "$(home_title)" "More" "Home scrolled" || return 1
  local y_home
  y_home=$(node_cy smokecoffee) || { log "row missing after collapse"; return 1; }
  shot appbar-1-home-collapsed
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  check_bar expanded "Accounts" "Reorder" "Accounts at start" || return 1
  collapse_bar
  check_bar collapsed "Accounts" "Reorder" "Accounts scrolled" || return 1
  shot appbar-2-accounts-collapsed
  tap_tab Home || return 1
  wait_for text exact "smokecoffee" || return 1
  check_bar collapsed "$(home_title)" "More" "Home after tab switch" || return 1
  [ "$(node_cy smokecoffee)" = "$y_home" ] || { log "Home list moved: $(node_cy smokecoffee) vs $y_home"; return 1; }
  shot appbar-3-home-back
  tap_tab Accounts || return 1
  wait_for text exact "Counted in total" || return 1
  check_bar collapsed "Accounts" "Reorder" "Accounts after tab switch" || return 1
  tap_node text exact "Cash" || return 1
  wait_for desc exact "Edit" || return 1
  check_bar expanded "Cash" "Edit" "account at start" || return 1
  collapse_bar
  check_bar collapsed "Cash" "Edit" "account scrolled" || return 1
  shot appbar-4-account-collapsed
  back
  wait_for text exact "Counted in total" || { log "back did not return to Accounts"; return 1; }
  check_bar collapsed "Accounts" "Reorder" "Accounts after back" || return 1
  shot appbar-5-accounts-back
  tap_node text exact "Cash" || return 1
  wait_for desc exact "Edit" || return 1
  check_bar expanded "Cash" "Edit" "account reopened" || return 1
  shot appbar-6-account-reopened
  back
  wait_for text exact "Counted in total" || return 1
  crashed && return 1
  return 0
}

# The Categories tab: the top bar has sort and reorder (no back), the add button is a FAB; the Expense / Income chips
# carry the month total and Income hides the expense-only Groceries row; add a category through the Create category
# sheet, it is listed (under No records this month) and opens its transactions, back returns to the list; Sort by opens
# and closes; the reorder sheet opens and Done closes it; back from the tab returns to Home. Needs the Groceries
# category with an expense this month.
flow_categories() {
  ensure_seed || return 1
  tap_tab Categories || return 1
  wait_for desc exact "Add category" || return 1
  has desc exact "Sort by" && has desc exact "Reorder" || { log "categories top bar buttons missing"; return 1; }
  has desc exact "Back" && { log "Categories is a tab and must not show a back button"; return 1; }
  local w h i
  w=$(screen_width); h=$(screen_height)
  has text sub "Expense " && has text sub "Income " || { log "type chips missing"; return 1; }
  has text exact "Groceries" || { log "seeded category missing from the Expense list"; return 1; }
  shot categories-1-list
  tap_node text sub "Income " || return 1
  sleep 1
  dump || return 1
  has text exact "No records this month" || { log "No records this month section missing under Income"; return 1; }
  [ "$(text_y Groceries)" -gt "$(text_y "No records this month")" ] || { log "Groceries has no income and must sit under No records this month"; return 1; }
  shot categories-1b-income
  tap_node text sub "Expense " || return 1
  sleep 1
  dump || return 1
  has text exact "Groceries" || { log "Groceries missing after switching back to Expense"; return 1; }
  tap_node desc exact "Add category" || return 1
  wait_for text exact "Create category" || { log "category sheet did not open"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  adb shell input text smokecat
  sleep 1
  shot categories-2-sheet
  tap_node text exact "Add" || return 1
  wait_gone text exact "Create category" || { log "Add did not close the category sheet"; return 1; }
  # the new category is last in the list: scroll until it is on screen
  for i in 1 2 3 4 5 6 7 8; do
    sleep 1
    dump || return 1
    has text exact "smokecat" && break
    adb shell input swipe $((w / 2)) $((h * 70 / 100)) $((w / 2)) $((h * 30 / 100)) 300 </dev/null
  done
  has text exact "smokecat" || { log "new category missing from the list"; return 1; }
  shot categories-3-created
  tap_node text exact "smokecat" || return 1
  wait_for desc exact "Edit" || { log "category transactions did not open"; return 1; }
  has text exact "smokecat" || { log "category name missing on its transactions screen"; return 1; }
  back
  wait_for desc exact "Add category" || { log "back from the category should return to the list"; return 1; }
  tap_node desc exact "Sort by" || return 1
  wait_for text exact "Sort by" || { log "sort sheet did not open"; return 1; }
  shot categories-4-sort
  tap_node desc exact "Close" || return 1
  wait_gone text exact "Sort by" || { log "Close did not dismiss the sort sheet"; return 1; }
  tap_node desc exact "Reorder" || return 1
  wait_for text exact "Done" || { log "reorder sheet did not open"; return 1; }
  shot categories-5-reorder
  tap_node text exact "Done" || return 1
  wait_gone text exact "Done" || { log "Done did not close the reorder sheet"; return 1; }
  back
  wait_for desc exact "More" || { log "back from the Categories tab should return to Home"; return 1; }
  crashed && return 1
  return 0
}

# Home More -> Budgets: the top bar has back and reorder, the add button is a FAB; add a budget through the Create
# budget sheet and the amount sheet, it is listed with its progress, tapping it opens the Edit budget sheet, back
# closes the sheet, back again returns to the open More sheet.
flow_budgets() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Budgets" || return 1
  wait_for desc exact "Add budget" || return 1
  has desc exact "Back" && has desc exact "Reorder" && has text exact "Budgets" || { log "budgets top bar missing"; return 1; }
  shot budgets-1-list
  tap_node desc exact "Add budget" || return 1
  wait_for text exact "Create budget" || { log "budget sheet did not open"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  adb shell input text smokebudget
  sleep 1
  adb shell input keyevent KEYCODE_BACK
  sleep 1
  tap_node any sub "Budget amount" || return 1
  wait_for text exact "Enter" || { log "amount sheet did not open"; return 1; }
  tap_node text exact "5" || return 1
  tap_node text exact "0" || return 1
  tap_node text exact "Enter" || return 1
  wait_gone text exact "Enter" 5 || { log "Enter did not close the amount sheet"; return 1; }
  has any sub "50.00" || { log "amount missing from the budget sheet"; return 1; }
  shot budgets-2-sheet
  tap_node text exact "Add" || return 1
  wait_gone text exact "Create budget" || { log "Add did not close the budget sheet"; return 1; }
  wait_for text exact "smokebudget" || { log "new budget missing from the list"; return 1; }
  has any sub "50.00 USD" || { log "amounts missing on the budget card"; return 1; }
  shot budgets-3-created
  tap_node text exact "smokebudget" || return 1
  wait_for text exact "Edit budget" || { log "edit sheet did not open"; return 1; }
  shot budgets-4-edit
  back
  wait_gone text exact "Edit budget" 5 || { log "back did not close the edit sheet"; return 1; }
  has text exact "smokebudget" || { log "budget list missing after the edit sheet closed"; return 1; }
  back
  wait_for text exact "Quick access" || { log "back from Budgets should return to the open More sheet"; return 1; }
  crashed && return 1
  return 0
}

# Home More -> Loans: the top bar has back and reorder, the tabs are Pending and Completed and the add button is a FAB;
# add a loan through the New loan sheet and the amount sheet, it is listed under Pending (not under Completed), tapping
# it opens the details screen (top bar, summary, starting record), Add record adds a 20 record through the sheet and the left amount drops to 30, back returns to the list, back again returns to the open More sheet.
flow_loans() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Loans" || return 1
  wait_for desc exact "Add loan" || return 1
  has desc exact "Back" && has desc exact "Reorder" && has text exact "Loans" && has text exact "Pending" && has text exact "Completed" || { log "loans top bar or tabs missing"; return 1; }
  shot loans-1-list
  tap_node desc exact "Add loan" || return 1
  wait_for text exact "New loan" || { log "loan sheet did not open"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  adb shell input text smokeloan
  sleep 1
  adb shell input keyevent KEYCODE_BACK
  sleep 1
  tap_node any sub "Enter loan amount" || return 1
  wait_for text exact "Enter" || { log "amount sheet did not open"; return 1; }
  tap_node text exact "5" || return 1
  tap_node text exact "0" || return 1
  tap_node text exact "Enter" || return 1
  wait_gone text exact "Enter" 5 || { log "Enter did not close the amount sheet"; return 1; }
  has any sub "50.00" || { log "amount missing from the loan sheet"; return 1; }
  shot loans-2-sheet
  tap_node text exact "Add" || return 1
  wait_gone text exact "New loan" || { log "Add did not close the loan sheet"; return 1; }
  wait_for text exact "smokeloan" || { log "new loan missing from the list"; return 1; }
  has any sub "50.00 USD" || { log "amounts missing on the loan card"; return 1; }
  shot loans-3-created
  tap_node text exact "Completed" || return 1
  wait_gone text exact "smokeloan" 5 || { log "a pending loan is listed under Completed"; return 1; }
  tap_node text exact "Pending" || return 1
  wait_for text exact "smokeloan" || { log "loan missing after switching back to Pending"; return 1; }
  tap_node text exact "smokeloan" || return 1
  wait_gone desc exact "Add loan" 5 || { log "tapping the loan did not leave the list"; return 1; }
  shot loans-4-details
  has text exact "smokeloan" || { log "details screen does not show the loan"; return 1; }
  has desc exact "Back" && has desc exact "Edit" && has desc exact "Delete" && has desc exact "Add record" || { log "loan details top bar or Add record missing"; return 1; }
  has text exact "Left to pay" && has any sub "50.00 USD" && has text exact "Starting record" || { log "loan summary or starting record missing"; return 1; }
  tap_node desc exact "Add record" || return 1
  wait_for text exact "Enter" || { log "record sheet did not open its amount sheet"; return 1; }
  tap_node text exact "2" || return 1
  tap_node text exact "0" || return 1
  tap_node text exact "Enter" || return 1
  wait_gone text exact "Enter" 5 || { log "Enter did not close the record amount sheet"; return 1; }
  tap_node text exact "Add" || return 1
  wait_gone text exact "New record" 5 || { log "Add did not close the record sheet"; return 1; }
  wait_for any sub "30.00 USD" || { log "left amount did not change after the record"; return 1; }
  has any sub "20.00 USD" && has text exact "Decrease" || { log "the new record row is missing"; return 1; }
  shot loans-5-record
  back
  wait_for desc exact "Add loan" || { log "back from the details did not return to the list"; return 1; }
  has text exact "smokeloan" || { log "loan list missing after back"; return 1; }
  back
  wait_for text exact "Quick access" || { log "back from Loans should return to the open More sheet"; return 1; }
  crashed && return 1
  return 0
}

# Home More -> Settings -> Behavior -> Exchange rates: top bar with back, the search field and the Add rate FAB; add a manual SMK rate
# through the Add rate sheet and the amount sheet, it is listed under Manual, searching for another code hides it and the
# empty state shows, clearing the search brings it back, its delete button removes it, back returns to Settings.
flow_exchange_rates() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Settings" || return 1
  wait_for text exact "About & Support" || return 1
  tap_node text exact "Behavior" || return 1
  wait_for text exact "Exchange rates" || { log "behavior page did not open"; return 1; }
  tap_node text exact "Exchange rates" || return 1
  wait_for desc exact "Add rate" || { log "exchange rates screen did not open"; return 1; }
  has desc exact "Back" && has text exact "Exchange rates" && has any sub "Search" || { log "exchange rates top bar or search missing"; return 1; }
  shot rates-1-list
  tap_node desc exact "Add rate" || return 1
  wait_for text exact "Exchange rate" || { log "add rate sheet did not open"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  adb shell input text smk
  sleep 1
  adb shell input keyevent KEYCODE_BACK
  sleep 1
  tap_node text exact "Exchange rate" || return 1
  wait_for text exact "Enter" || { log "amount sheet did not open"; return 1; }
  tap_node text exact "2" || return 1
  tap_node text exact "Enter" || return 1
  wait_gone text exact "Enter" 5 || { log "Enter did not close the amount sheet"; return 1; }
  shot rates-2-sheet
  tap_node text exact "Add" || return 1
  wait_for text exact "Manual" || { log "the Manual section is missing after adding a rate"; return 1; }
  has any sub "SMK" || { log "new rate missing from the list"; return 1; }
  shot rates-3-added
  tap_node class exact android.widget.EditText || return 1
  adb shell input text xyz
  wait_for text exact "No matches" || { log "empty state missing for a search with no match"; return 1; }
  wait_gone any sub "SMK" 5 || { log "rate still listed for another search"; return 1; }
  shot rates-4-no-result
  tap_node desc exact "Clear search" || return 1
  wait_for any sub "SMK" || { log "rate missing after clearing the search"; return 1; }
  wait_stable desc sub "–SMK" || return 1
  tap_node desc sub "–SMK" || return 1
  wait_gone any sub "SMK" 10 || { log "the delete button did not remove the rate"; return 1; }
  shot rates-5-deleted
  back
  dump || return 1
  # the first back only hides the keyboard of the focused search field
  ! has desc exact "Add rate" || back
  wait_gone desc exact "Add rate" 5 || { log "back from Exchange rates should return to Settings"; return 1; }
  wait_for text exact "Exchange rates" || { log "Settings did not return to the Behavior page after back"; return 1; }
  crashed && return 1
  return 0
}

# Home (seeds the smokecoffee expense of flow_add_expense when needed): the balance opens the Balance screen with the back
# button, the period row and the current / planned / after-planned amounts; Next and Previous change the period, back
# returns to Home.
flow_balance() {
  ensure_seed || return 1
  dump || return 1
  has text exact "Cashflow" || has any sub "Cashflow" || { log "home balance missing"; return 1; }
  tap_node text sub " USD" 0 || return 1
  wait_for text exact "Balance" 10 || { log "balance screen did not open: run flow_add_expense first, the balance opens only with transactions"; return 1; }
  has desc exact "Back" && has text exact "Current balance" && has text exact "Planned payments" && has text exact "Balance after planned payments" || { log "balance rows or back button missing"; return 1; }
  shot balance-1
  local before after
  before=$(period_label_anywhere) || { log "period label not found"; return 1; }
  tap_node desc exact "Next" || return 1
  sleep 1
  dump || return 1
  after=$(period_label_anywhere) || { log "period label missing after Next"; return 1; }
  [ "$after" != "$before" ] || { log "Next did not change the period ('$before')"; return 1; }
  shot balance-2-next
  tap_node desc exact "Previous" || return 1
  sleep 1
  dump || return 1
  after=$(period_label_anywhere) || { log "period label missing after Previous"; return 1; }
  [ "$after" = "$before" ] || { log "Previous gave '$after', expected '$before'"; return 1; }
  back
  wait_for desc exact "More" || { log "back from Balance should return to Home"; return 1; }
  crashed && return 1
  return 0
}

# scroll_to <attr> <match> <needle>: swipes the list up until the node is in the dump
scroll_to() {
  local w h i
  w=$(screen_width); h=$(screen_height)
  for i in 1 2 3 4 5 6 7 8; do
    dump || return 1
    has "$@" && return 0
    adb shell input swipe $((w / 2)) $((h * 70 / 100)) $((w / 2)) $((h * 30 / 100)) 300 </dev/null
    sleep 1
  done
  log "never scrolled to: $*"
  return 1
}

# row_checked <text>: "true" or "false", the checked state of the toggleable row that contains the text
row_checked() {
  python3 -I - "$XML" "$1" <<'PY'
import sys, xml.etree.ElementTree as ET
path, needle = sys.argv[1:3]
for n in ET.parse(path).iter('node'):
    if n.get('checkable') != 'true':
        continue
    texts = [m.get('text') for m in n.iter('node')]
    if needle in texts:
        print(n.get('checked'))
        sys.exit(0)
sys.exit(1)
PY
}

# theme_label: the current theme label ("Light mode", "Auto mode"...) in the dump
theme_label() {
  python3 -I - "$XML" <<'PY'
import sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    t = n.get('text') or ''
    if t.endswith(' mode'):
        print(t)
        sys.exit(0)
sys.exit(1)
PY
}

# Home More -> Settings: the category list (large top bar with back, one row per category), Privacy opens with its
# switches, Hide balance flips and Home shows the hidden balance, the theme radio dialog cycles the theme through its
# four values and back, Exchange rates opens from Behavior and returns, the currency dialog opens and closes, Danger zone
# asks before deleting, back from a page returns to the list and back from the list to the More sheet.
flow_settings() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Settings" || return 1
  wait_for text exact "About & Support" || { log "settings did not open"; return 1; }
  has desc exact "Back" && has text exact "Settings" && has text exact "Profile" && has text exact "Import & Export" && has text exact "Danger zone" || { log "settings top bar or categories missing"; return 1; }
  shot settings-1-categories
  tap_node text exact "Privacy" || return 1
  wait_for text exact "Hide balance" || { log "privacy page did not open"; return 1; }
  [ "$(row_checked "Hide balance")" = false ] || { log "Hide balance should start off (seeded state)"; return 1; }
  tap_node text exact "Hide balance" || return 1
  sleep 1
  dump || return 1
  [ "$(row_checked "Hide balance")" = true ] || { log "the Hide balance row did not turn on"; return 1; }
  shot settings-2-switch-on
  back
  wait_for text exact "About & Support" || { log "back from a page should return to the category list"; return 1; }
  back
  wait_for text exact "Quick access" || { log "back from the list should return to the open More sheet"; return 1; }
  tap_node desc exact "Close" || return 1
  wait_for any sub "****" 10 || { log "Home does not show the hidden balance"; return 1; }
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Settings" || return 1
  wait_for text exact "About & Support" || return 1
  tap_node text exact "Privacy" || return 1
  wait_for text exact "Hide balance" || return 1
  [ "$(row_checked "Hide balance")" = true ] || { log "Hide balance did not stay on"; return 1; }
  tap_node text exact "Hide balance" || return 1
  sleep 1
  dump || return 1
  [ "$(row_checked "Hide balance")" = false ] || { log "the Hide balance row did not turn off"; return 1; }
  back
  wait_for text exact "About & Support" || return 1
  tap_node text exact "Appearance" || return 1
  wait_for text exact "Theme" || { log "appearance page did not open"; return 1; }
  local mode first opt
  first=$(theme_label) || { log "theme label not found"; return 1; }
  for opt in "Dark mode" "Light mode" "AMOLED Dark mode" "$first"; do
    tap_node text exact "Theme" || return 1
    wait_for text exact "Cancel" || { log "the theme dialog did not open"; return 1; }
    [ "$opt" != "$first" ] || shot settings-3-theme-dialog
    tap_node text exact "$opt" || return 1
    wait_gone text exact "Cancel" 5 || { log "the theme dialog did not close after choosing $opt"; return 1; }
    sleep 1
    dump || return 1
    mode=$(theme_label) || { log "theme label missing after choosing $opt"; return 1; }
    [ "$mode" = "$opt" ] || { log "chose '$opt' but the theme row shows '$mode'"; return 1; }
  done
  shot settings-3-theme-restored
  back
  wait_for text exact "About & Support" || return 1
  tap_node text exact "Behavior" || return 1
  wait_for text exact "Exchange rates" || { log "behavior page did not open"; return 1; }
  tap_node text exact "Exchange rates" || return 1
  wait_for desc exact "Add rate" || { log "exchange rates did not open from settings"; return 1; }
  back
  wait_gone desc exact "Add rate" 5 || { log "back from Exchange rates should return to Settings"; return 1; }
  # Settings keeps its page while another screen is open: back from Exchange rates shows the Behavior page again
  wait_for text exact "Exchange rates" || { log "Settings did not return to the Behavior page after back"; return 1; }
  back
  wait_for text exact "About & Support" || { log "back from the Behavior page should return to the category list"; return 1; }
  tap_node text exact "Profile" || return 1
  wait_for text exact "Currency" || { log "profile page did not open"; return 1; }
  has text exact "Anonymous" || has text exact "Name" || { log "profile rows missing"; return 1; }
  tap_node text exact "Currency" || return 1
  wait_for text exact "Cancel" || { log "currency dialog did not open"; return 1; }
  has any sub "Search" || { log "currency dialog search field missing"; return 1; }
  shot settings-4-currency
  back
  wait_gone text exact "Cancel" 5 || { log "back did not close the currency dialog"; return 1; }
  has text exact "Currency" || { log "profile page missing after the currency dialog"; return 1; }
  back
  wait_for text exact "About & Support" || return 1
  tap_node text exact "About & Support" || return 1
  wait_for text exact "Attributions" || { log "about page did not open"; return 1; }
  has text exact "Version" && has text exact "GitHub" && has text exact "Terms & Conditions" && has text exact "Privacy Policy" || { log "about rows missing"; return 1; }
  shot settings-5-about
  back
  wait_for text exact "About & Support" || return 1
  tap_node text exact "Danger zone" || return 1
  wait_for text exact "Delete all user data" || { log "danger zone page did not open"; return 1; }
  tap_node text exact "Delete all user data" || return 1
  wait_for text exact "Delete all user data?" || { log "the delete confirmation did not open"; return 1; }
  shot settings-6-delete
  back
  wait_gone text exact "Delete all user data?" 5 || { log "back did not close the delete confirmation"; return 1; }
  has text exact "Delete all user data" || { log "danger zone missing after the confirmation"; return 1; }
  back
  wait_for text exact "About & Support" || return 1
  back
  wait_for text exact "Quick access" || { log "back from Settings should return to the open More sheet"; return 1; }
  crashed && return 1
  return 0
}

# Settings -> Import data: the source list with the manual CSV button, an app source opens its instructions (steps and
# the pinned upload button, nothing is picked), back returns to the list, the manual CSV screen opens with its upload
# button and a disabled continue button, back twice returns to Settings.
flow_import_data() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Settings" || return 1
  wait_for text exact "About & Support" || { log "settings did not open"; return 1; }
  tap_node text exact "Import & Export" || return 1
  wait_for text exact "Import data" || { log "import & export page did not open"; return 1; }
  tap_node text exact "Import data" || return 1
  wait_for text exact "Manual CSV import" || { log "import data did not open"; return 1; }
  has desc exact "Back" && has text exact "Import from" && has text exact "Money Manager" || { log "import sources missing"; return 1; }
  shot import-1-sources
  tap_node text exact "Money Manager" || return 1
  wait_for text exact "How to import" || { log "instructions did not open"; return 1; }
  has text exact "Steps" && has text exact "Export Excel file" || { log "instruction steps missing"; return 1; }
  has_clickable "Upload a CSV file" || { log "pinned upload button missing"; return 1; }
  has text exact "Skip" && { log "instructions: Skip shown when entering from Settings"; return 1; }
  shot import-2-instructions
  tap_node desc exact "Back" || return 1
  wait_for text exact "Import from" || { log "back from the instructions should return to the sources"; return 1; }
  tap_node text exact "Manual CSV import" || return 1
  wait_for text exact "Import CSV file" || { log "manual csv screen did not open"; return 1; }
  has text exact "Continue" || { log "continue button missing"; return 1; }
  shot import-3-manual-csv
  tap_node desc exact "Back" || return 1
  wait_for text exact "Import from" || { log "back from the manual csv screen should return to the sources"; return 1; }
  tap_node desc exact "Back" || return 1
  wait_for any sub "Import & Export" || { log "back from the sources should return to Settings"; return 1; }
  crashed && return 1
  return 0
}

# ---------- runner ----------

run_flow() {
  local name=$1
  DUMP_FAILED=0
  if "$name" && [ "$DUMP_FAILED" = 0 ]; then
    echo "PASS $name"
    RESULTS+=("PASS $name")
  else
    echo "FAIL $name"
    RESULTS+=("FAIL $name")
    shot "FAIL-$name" 2>/dev/null
    FAILED=1
  fi
}

ALL_FLOWS="flow_onboarding flow_fab_menu flow_amount_sheet_close flow_nested_back flow_calculator_reopen flow_keyboard_sheet flow_navbar_clearance flow_cancelled_hide flow_reorder flow_choose_period flow_period_other_year flow_date_picker flow_period_date_picker flow_more_menu flow_account_detail flow_account_scroll flow_planned_payments flow_add_expense flow_type_filter flow_piechart flow_search flow_appbar_state flow_reports flow_categories flow_budgets flow_loans flow_exchange_rates flow_balance flow_settings flow_import_data flow_due_section flow_pay_planned flow_planned_scroll"
# Screens reachable by `shot`: home, accounts and categories are tabs, the others are rows of the More sheet.
ALL_SCREENS="home accounts categories budgets loans planned-payments reports settings"

# ---------- screens ----------

# open_more_item <row text> <text that proves the screen is open>
open_more_item() {
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "$1" || return 1
  wait_for any sub "$2" 15
}

# open_screen <name>: leaves the named screen open, starting from a fresh launch
open_screen() {
  go_home || return 1
  case "$1" in
    home) ;;
    accounts) tap_tab Accounts && wait_for text exact "Counted in total" ;;
    categories) tap_tab Categories && wait_for desc exact "Add category" ;;
    budgets) open_more_item "Budgets" "Add budget" ;;
    loans) open_more_item "Loans" "Add loan" ;;
    planned-payments) open_more_item "Planned payments" "Planned" ;;
    reports) open_more_item "Reports" "Reports" ;;
    settings) open_more_item "Settings" "About & Support" ;;
    *) log "unknown screen '$1', known: $ALL_SCREENS"; return 2 ;;
  esac
  local rc=$?
  sleep 1
  return $rc
}

if [ "$MODE" = list ]; then
  echo "flows:   ${ALL_FLOWS//flow_/}"
  echo "screens: $ALL_SCREENS"
  exit 0
fi

if [ "$MODE" = shot ]; then
  case " $ALL_SCREENS " in *" $SCREEN "*) ;; *) log "usage: scripts/smoke.sh shot <screen>; screens: $ALL_SCREENS"; exit 2 ;; esac
fi
FLOW_LIST=()
if [ ${#FLOW_ARGS[@]} -gt 0 ]; then
  for f in "${FLOW_ARGS[@]}"; do FLOW_LIST+=("flow_${f#flow_}"); done
else
  FLOW_LIST=(${FLOWS:-$ALL_FLOWS})
fi
for flow in "${FLOW_LIST[@]}"; do
  declare -F "$flow" >/dev/null || { log "unknown flow '$flow', see: scripts/smoke.sh list"; exit 2; }
done
# build before booting the emulator: both together can exhaust memory and kill the Gradle daemon
if [ -z "${SKIP_INSTALL:-}" ]; then
  log "building debug build"
  ./gradlew --console=plain --max-workers="${GRADLE_WORKERS:-2}" :app:assembleDebug >"$OUT/install.log" 2>&1 || { log "assembleDebug failed, see $OUT/install.log"; exit 2; }
fi
ensure_device || exit 2
if [ -z "${SKIP_INSTALL:-}" ]; then
  # adb instead of :app:installDebug: a Gradle daemon next to a booting emulator tends to get killed
  APK=$(ls -t app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1)
  log "installing $APK"
  adb install -r -t "$APK" >>"$OUT/install.log" 2>&1 || { log "adb install failed, see $OUT/install.log"; exit 2; }
fi
# a run must not depend on what an earlier one left behind: no accessibility services (TalkBack
# raises a notification-permission dialog over Home after a fresh boot), default font scale
adb shell "settings put secure enabled_accessibility_services ''" >/dev/null 2>&1
adb shell settings put secure accessibility_enabled 0 >/dev/null 2>&1
adb shell settings put system font_scale 1.0 >/dev/null 2>&1
adb shell settings put global animator_duration_scale 1 >/dev/null 2>&1
NIGHT_PREV=$(adb shell cmd uimode night 2>/dev/null | sed 's/.*: *//' | tr -d '\r')
case "$NIGHT_PREV" in yes | no | auto) ;; *) NIGHT_PREV="" ;; esac
adb shell cmd uimode night no >/dev/null 2>&1
adb logcat -c


if [ "$MODE" = shot ]; then
  fresh_start
  for _ in $(seq 1 20); do
    dump
    has text exact "Home" && break
    has text exact "I accept and agree" && { flow_onboarding || { log "onboarding failed"; exit 1; }; break; }
    sleep 1
  done
  mkdir -p "$OUT/screens"
  if ! open_screen "$SCREEN"; then
    OUT="$OUT/screens" shot "FAIL-$SCREEN"
    log "could not open '$SCREEN', see $OUT/screens/FAIL-$SCREEN.png"
    exit 1
  fi
  OUT="$OUT/screens" shot "$SCREEN"
  crashed && { log "FATAL EXCEPTION in logcat"; exit 1; }
  echo "$OUT/screens/$SCREEN.png"
  exit 0
fi

for flow in "${FLOW_LIST[@]}"; do
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
