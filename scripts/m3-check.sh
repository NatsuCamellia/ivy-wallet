#!/usr/bin/env bash
# Usage: scripts/m3-check.sh <base-ref> [--no-total]
# Scans non-test .kt lines added between <base-ref> and HEAD against the M3 spacing rules
# (docs/superpowers/specs/2026-11-07-m3-expressive-design.md, "無硬編間距規則").
# --no-total skips the "repo-wide .dp count must drop" rule, for checks inside a stage.
set -euo pipefail

base="${1:?usage: scripts/m3-check.sh <base-ref> [--no-total]}"
check_total=1
[ "${2:-}" = "--no-total" ] && check_total=0

cd "$(git rev-parse --show-toplevel)"
git rev-parse --verify --quiet "$base^{commit}" >/dev/null || { echo "unknown ref: $base" >&2; exit 2; }

allowlist=docs/m3-migration/dp-allowlist.tsv
excluded='(/src/(test|androidTest|testFixtures)/|Tests?[.]kt$)'

count_dp() {
  git grep -hoE '[0-9]\.dp\b' "$1" -- '*.kt' ':!*/src/test/*' ':!*/src/androidTest/*' ':!*/src/testFixtures/*' ':!*Test.kt' ':!*Tests.kt' | wc -l
}

added=$(git diff -U0 --no-color --no-ext-diff --diff-filter=AM "$base" HEAD -- '*.kt' |
  awk -v excl="$excluded" '
    /^\+\+\+ b\// { file = substr($0, 7); skip = (file ~ excl); next }
    /^\+\+\+ / { skip = 1; next }
    /^@@ / { match($0, /\+[0-9]+/); line = substr($0, RSTART + 1, RLENGTH - 1) + 0; next }
    /^\+/ { if (!skip) printf "%s\t%d\t%s\n", file, line, substr($0, 2); line++ }
  ')

touched_kt=0
git diff --name-only "$base" HEAD -- '*.kt' | grep -q . && touched_kt=1

status=0
violations=$(mktemp)
trap 'rm -f "$violations"' EXIT

# Allowlist: path<TAB>count<TAB>reason, header line first.
declare -A allow
if [ -f "$allowlist" ]; then
  while IFS=$'\t' read -r path count reason; do
    [ "$path" = "path" ] && continue
    [ -z "$path" ] && continue
    case "$reason" in
      "chart geometry" | "icon size") allow["$path"]=$count ;;
      *) echo "$allowlist: invalid reason '$reason' for $path (chart geometry | icon size)"; status=1 ;;
    esac
  done < "$allowlist"
fi
allow_json=$(for k in "${!allow[@]}"; do printf '%s\t%s\n' "$k" "${allow[$k]}"; done)

summary=$(printf '%s\n' "$added" | awk -F'\t' -v viol="$violations" -v allowtxt="$allow_json" '
  BEGIN {
    n = split(allowtxt, rows, "\n")
    for (i = 1; i <= n; i++) { split(rows[i], kv, "\t"); if (kv[1] != "") allowed[kv[1]] = kv[2] + 0 }
    grid["4"]; grid["8"]; grid["12"]; grid["16"]; grid["24"]
  }
  function fail(msg) { printf "%s:%d: %s\n", $1, $2, msg >> viol }
  function module(p,   a) { split(p, a, "/"); return (a[2] == "" || a[1] ~ /\.kt$/) ? a[1] : a[1] "/" a[2] }
  function numeric_dps(s,   t, c) { t = s; c = 0; while (match(t, /[0-9]\)?\.dp([^A-Za-z0-9_]|$)/)) { c++; t = substr(t, RSTART + RLENGTH) } return c }
  function off_grid(s,   t, v) {
    t = s
    while (match(t, /[0-9]+(\.[0-9]+)?\)?\.dp([^A-Za-z0-9_]|$)/)) {
      v = substr(t, RSTART, RLENGTH); sub(/\)?\.dp.*/, "", v)
      if (!(v in grid)) return 1
      t = substr(t, RSTART + RLENGTH)
    }
    return 0
  }
  NF >= 3 {
    m = module($1); lines[m]++
    text = $3
    for (i = 4; i <= NF; i++) text = text "\t" $i
    if (text ~ /^[ \t]*(\/\/|\*|\/\*)/) next
    sub(/[ \t]\/\/.*$/, "", text)
    dps = numeric_dps(text)
    if (dps > 0) dplines[m]++
    hard = 0
    if (match(text, /\.offset\(([^()]|\([^()]*\))*\)/) && numeric_dps(substr(text, RSTART, RLENGTH)) > 0) { fail("numeric .dp inside .offset("); hard = 1 }
    if (text ~ /[0-9]\.sp([^A-Za-z0-9_]|$)/) { fail(".sp literal"); hard = 1 }
    if (text ~ /fontSize[ \t]*=/) { fail("fontSize ="); hard = 1 }
    if (match(text, /RoundedCornerShape\(([^()]|\([^()]*\))*\)/) && numeric_dps(substr(text, RSTART, RLENGTH)) > 0) { fail("RoundedCornerShape with numeric .dp"); hard = 1 }
    if (hard) next
    sizing = (text ~ /\.(size|height|width)\(.*(\.dp|\.toDp\(\))/)
    spacer = (text ~ /Spacer\(/)
    spacing = (text ~ /(padding|Padding|PaddingValues|spacedBy)\(/ || spacer)
    if ((sizing && !spacer) || (dps > 0 && (!spacing || off_grid(text)))) {
      if (!sizing && spacing) why = "off-grid spacing (use 4, 8, 12, 16, 24) or allowlist"
      else why = "numeric .dp / size needs an entry in docs/m3-migration/dp-allowlist.tsv"
      pending[$1] = pending[$1] "\n" $2 "\t" why
      pendcount[$1]++
    }
  }
  END {
    for (f in pendcount) {
      k = split(substr(pending[f], 2), rows, "\n")
      for (i = 1; i <= k; i++) {
        if (i <= allowed[f] + 0) continue
        split(rows[i], kv, "\t"); printf "%s:%d: %s\n", f, kv[1], kv[2] >> viol
      }
    }
    for (m in lines) {
      d = dplines[m] + 0
      over = (d * 100 > 3 * lines[m]) ? " OVER BUDGET" : ""
      printf "BUDGET\t%s\t%d\t%d\t%.1f\t%s\n", m, lines[m], d, d * 100 / lines[m], over
    }
  }
' | sort)

echo "m3-check: base=$base head=$(git rev-parse --short HEAD)"
echo "module                          added  dp-lines  per-100"
over_budget=0
while IFS=$'\t' read -r tag m lines dps per over; do
  [ "$tag" = "BUDGET" ] || continue
  printf '%-30s %6s %9s %8s%s\n' "$m" "$lines" "$dps" "$per" "$over"
  if [ -n "$over" ]; then over_budget=1; echo "$m: more than 3 numeric .dp lines per 100 added lines" >> "$violations"; fi
done <<< "$summary"
[ -z "$summary" ] && echo "(no added non-test .kt lines)"

base_dp=$(count_dp "$base")
head_dp=$(count_dp HEAD)
echo "repo-wide numeric .dp (non-test): base=$base_dp head=$head_dp"
if [ "$check_total" = 1 ] && [ "$touched_kt" = 1 ] && [ "$head_dp" -ge "$base_dp" ]; then
  echo "repo-wide .dp count did not drop ($base_dp -> $head_dp)" >> "$violations"
fi

if [ -s "$violations" ]; then
  echo "VIOLATIONS:"
  sort -u "$violations"
  status=1
fi
[ "$status" = 0 ] && echo "m3-check: OK"
exit "$status"
