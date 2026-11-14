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
NIGHT_PREV=""
RESULTS=()
FAILED=0

cd "$(git rev-parse --show-toplevel)" || exit 2
mkdir -p "$OUT"

log() { echo "[m3-smoke] $*" >&2; }

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

DUMP_FAILED=0

# Fresh UI tree or failure: never leaves a stale tree behind; a failure also fails the running flow.
dump() {
  local attempt
  rm -f "$XML"
  for attempt in 1 2 3; do
    adb shell rm -f /sdcard/m3-ui.xml >/dev/null 2>&1
    if adb shell uiautomator dump /sdcard/m3-ui.xml >/dev/null 2>&1 \
      && adb pull /sdcard/m3-ui.xml "$XML" >/dev/null 2>&1 && [ -s "$XML" ]; then
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
  tap_tab Accounts || return 1
  wait_for text exact "Total Balance" || return 1
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
flow_cancelled_hide() {
  open_account_edit_sheet || return 1
  adb shell settings put global animator_duration_scale "${ANIM_SCALE:-20}"
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
  wait_for text exact "Total Balance" || return 1
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

# Home FAB: tap opens the menu, back collapses it (still on Home), reopen, Add expense opens the edit screen.
flow_fab_menu() {
  go_home || return 1
  has desc exact "Add" || { log "FAB missing on Home"; return 1; }
  shot fab-1-collapsed
  open_fab_menu || return 1
  has text exact "Add income" && has text exact "Transfer" && has text exact "Add planned payment" || { log "menu items missing"; return 1; }
  has desc exact "Close" || { log "FAB should read Close while expanded"; return 1; }
  shot fab-2-expanded
  back
  wait_gone text exact "Add expense" || { log "back did not collapse the menu"; return 1; }
  has text exact "Home" && has desc exact "Add" || { log "back left the main screen"; return 1; }
  shot fab-3-collapsed-by-back
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
  wait_for text exact "Total Balance" || return 1
  has desc exact "Add" && { log "Home FAB shown on Accounts"; return 1; }
  back
  dump || return 1
  has text exact "Total Balance" && { log "Back was swallowed on Accounts (stale menu state)"; return 1; }
  has desc exact "Add" && ! has text exact "Add expense" || { log "Home should show the collapsed menu"; return 1; }
  shot fab-5-home-after-accounts
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

# Accounts -> Cash -> the Expenses card opens the pie chart: title, back, add, period buttons and the seeded Groceries
# row; Previous shows the empty state and Next brings the row back; the row opens the category transactions; back twice
# returns to the account. Needs flow_add_expense to have run (it seeds the Groceries expense).
flow_piechart() {
  go_home || return 1
  tap_tab Accounts || return 1
  wait_for text exact "Total Balance" || return 1
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
  has desc exact "More" || { log "more button missing on Home"; return 1; }
  shot more-1-home
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  has text exact "Search transactions..." && has text exact "Settings" && has text exact "Loans" || { log "menu items missing"; return 1; }
  shot more-2-sheet
  tap_node text exact "Settings" || return 1
  wait_for text exact "Set currency" || return 1
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
  wait_for text exact "Total Balance" || return 1
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
  wait_for text exact "Total Balance" || { log "back did not return to Accounts"; return 1; }
  crashed && return 1
  return 0
}

# Home More -> Search transactions: type the title of the seeded expense (flow_add_expense), its row is listed under the
# search field, Clear search empties the field, back leaves the screen. Needs flow_add_expense to have run.
flow_search() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Search transactions..." || return 1
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
  back
  wait_for text exact "Quick access" || { log "back from the list should return to the open More sheet"; return 1; }
  crashed && return 1
  return 0
}

# Home More -> Reports: empty state with Set Filter, the filter sheet (Apply stays disabled until the filter is valid),
# pick the Expenses type, the Cash account, the Groceries category and the default period, Apply: the seeded
# smokecoffee expense is listed; back returns to the open More sheet. Seeded by flow_add_expense.
flow_reports() {
  go_home || return 1
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

# Home More -> Categories: the top bar has back, sort and reorder, the add button is a FAB; add a category through the
# Create category sheet, it is listed and opens its transactions, back returns to the list; Sort by opens and closes;
# the reorder sheet opens and Done closes it; back returns to the open More sheet. Needs the Groceries category.
flow_categories() {
  go_home || return 1
  tap_node desc exact "More" || return 1
  wait_for text exact "Quick access" || return 1
  tap_node text exact "Categories" || return 1
  wait_for desc exact "Add category" || return 1
  has desc exact "Back" && has desc exact "Sort by" && has desc exact "Reorder" || { log "categories top bar buttons missing"; return 1; }
  has text exact "Groceries" || { log "seeded category missing"; return 1; }
  shot categories-1-list
  tap_node desc exact "Add category" || return 1
  wait_for text exact "Create category" || { log "category sheet did not open"; return 1; }
  tap_node class exact android.widget.EditText || return 1
  adb shell input text smokecat
  sleep 1
  shot categories-2-sheet
  tap_node text exact "Add" || return 1
  wait_gone text exact "Create category" || { log "Add did not close the category sheet"; return 1; }
  # the new category is last in the list: scroll until it is on screen
  local w h i
  w=$(screen_width); h=$(screen_height)
  for i in 1 2 3 4 5 6; do
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
  wait_for text exact "Quick access" || { log "back from Categories should return to the open More sheet"; return 1; }
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

ensure_device || exit 2
if [ -z "${SKIP_INSTALL:-}" ]; then
  log "installing debug build"
  ./gradlew --console=plain --max-workers=3 :app:installDebug >"$OUT/install.log" 2>&1 || { log "installDebug failed, see $OUT/install.log"; exit 2; }
fi
adb shell settings put global animator_duration_scale 1 >/dev/null 2>&1
NIGHT_PREV=$(adb shell cmd uimode night 2>/dev/null | sed 's/.*: *//' | tr -d '\r')
case "$NIGHT_PREV" in yes | no | auto) ;; *) NIGHT_PREV="" ;; esac
adb shell cmd uimode night no >/dev/null 2>&1
adb logcat -c

for flow in ${FLOWS:-flow_onboarding flow_fab_menu flow_amount_sheet_close flow_nested_back flow_calculator_reopen flow_keyboard_sheet flow_navbar_clearance flow_cancelled_hide flow_reorder flow_choose_period flow_date_picker flow_period_date_picker flow_more_menu flow_account_detail flow_planned_payments flow_add_expense flow_piechart flow_search flow_reports flow_categories flow_budgets}; do
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
