#!/usr/bin/env bash
# Usage: scripts/paparazzi-record.sh [--verify]
# Re-records Paparazzi goldens only for modules with changes and prints the snapshot files that were
# added or changed. A module counts as changed when a file under it differs from BASE (default HEAD, so
# staged, unstaged and untracked work; use BASE=main to include commits on a branch) and it has
# Paparazzi tests (a test extending PaparazziScreenshotTest). With --verify it runs verifyPaparazziDebug
# for those modules instead of recording.
# Env: BASE (git ref to compare with, default HEAD), MODULES (space separated Gradle paths such as
# ":feature:home", overrides detection).
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2
BASE="${BASE:-HEAD}"
TASK=recordPaparazziDebug
[ "${1:-}" = "--verify" ] && TASK=verifyPaparazziDebug

if [ -z "${MODULES:-}" ]; then
  MODULES=""
  changed=$( { git diff --name-only "$BASE" --; git ls-files --others --exclude-standard; } | sort -u)
  dirs=""
  for f in $changed; do
    d=$(dirname "$f")
    while [ "$d" != "." ] && [ ! -f "$d/build.gradle.kts" ]; do d=$(dirname "$d"); done
    [ "$d" != "." ] && dirs="$dirs $d"
  done
  for d in $(printf '%s\n' $dirs | sort -u); do
    case "$d" in .claude/* | ReadYou/* | buildSrc*) continue ;; esac
    [ -d "$d/src/test" ] && grep -rlq PaparazziScreenshotTest "$d/src/test" 2>/dev/null || continue
    MODULES="$MODULES :${d//\//:}"
  done
fi

if [ -z "${MODULES// /}" ]; then
  echo "No changed module has Paparazzi tests, nothing to do."
  exit 0
fi
echo "Modules:$MODULES"

before=$(mktemp)
trap 'rm -f "$before"' EXIT
git status --porcelain --untracked-files=all -- '*/src/test/snapshots/*' | sort >"$before"

tasks=()
for m in $MODULES; do tasks+=("$m:$TASK"); done
./gradlew --console=plain "${tasks[@]}" || { echo "Gradle $TASK failed" >&2; exit 1; }

if [ "$TASK" = verifyPaparazziDebug ]; then
  echo "Verify passed."
  exit 0
fi
echo "--- added/changed snapshots (git status; M = modified, ?? = new, A = staged new)"
git status --porcelain --untracked-files=all -- '*/src/test/snapshots/*' | sort | comm -13 "$before" - | sed 's/^/  /'
