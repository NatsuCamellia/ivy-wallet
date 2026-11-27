#!/usr/bin/env bash
# Usage: scripts/check-hilt-keys.sh [variant]   (default: release, the variant the demo build uses)
# Fails when a module of an @HiltViewModel has no generated Hilt classes for it in its build directory. An APK
# built from such a tree starts, but the screen of that ViewModel crashes with "Cannot create an instance of
# class ..." because Hilt cannot find the ViewModel. Two KSP tasks that ran in parallel did that (see
# KspSerialLock); a task that ended that way is up to date afterwards, so it stays broken across `./gradlew clean`.
# Run this after assembling and before installing a build. If it fails, run
# `./gradlew :<module>:kspReleaseKotlin --rerun` for the module it names and assemble again.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
variant="${1:-release}"

missing=0
checked=0
while IFS= read -r file; do
  pkg=$(sed -n 's/^package \(.*\)$/\1/p' "$file" | head -1)
  # the class is the first one after @HiltViewModel (other annotations may sit in between)
  cls=$(awk '/^@HiltViewModel/ { f = 1 } f && /^(open |abstract |internal )*class / { sub(/^(open |abstract |internal )*class /, ""); sub(/[^A-Za-z0-9_].*/, ""); print; exit }' "$file")
  [ -n "$cls" ] || continue
  module="${file%%/src/*}"
  # :app builds the variant "demo" or "release" itself, its libraries the variant "release"
  if [ "$module" = app ]; then
    generated="$module/build/generated/ksp/*/java/${pkg//.//}/${cls}_HiltModules.java"
  else
    generated="$module/build/generated/ksp/$variant/java/${pkg//.//}/${cls}_HiltModules.java"
  fi
  checked=$((checked + 1))
  # shellcheck disable=SC2086
  if ! ls $generated >/dev/null 2>&1; then
    echo "MISSING $generated (the $variant build of $module has no Hilt classes for $pkg.$cls)" >&2
    missing=1
  fi
done < <(git ls-files '*.kt' ':!*/src/test/*' ':!*/src/androidTest/*' | xargs grep -l '^@HiltViewModel')

[ "$missing" = 0 ] && echo "OK: $checked @HiltViewModel classes have their generated Hilt classes in the $variant build"
exit "$missing"
