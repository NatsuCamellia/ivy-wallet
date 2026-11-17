#!/usr/bin/env python3
"""Pair Paparazzi goldens between a base ref and HEAD and render before/after/diff sheets.

usage: m3-report.py --base m3-baseline --out <dir> [--width 270] [--quality 72]

Pairing key: module + file name with spaces replaced by '_' (Paparazzi 2 renames them).
Each test gets one JPEG sheet: a row per theme (Light, Dark), columns before | after | diff.
report.json lists every test with its status and changed-pixel ratio per theme (in sheet row order),
plus the sheet geometry so a page can crop one theme row.
"""
import argparse
import io
import json
import re
import subprocess
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

SNAPSHOT = re.compile(r"^(?P<module>.+)/src/test/snapshots/images/(?P<file>[^/]+\.png)$")
THEMES = ("Light", "Dark")
GAP = 6
THRESHOLD = 16


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True).stdout


def goldens(ref):
    """{(module, normalised file): path} for a ref, or for the working tree when ref is None."""
    out = git("ls-tree", "-r", "--name-only", ref) if ref else git("ls-files")
    found = {}
    for path in out.decode().splitlines():
        match = SNAPSHOT.match(path)
        if match:
            found[(match["module"], match["file"].replace(" ", "_"))] = path
    return found


def load(ref, path):
    data = git("show", f"{ref}:{path}") if ref else Path(path).read_bytes()
    return Image.open(io.BytesIO(data)).convert("RGB")


def split(file):
    """'pkg_Class_test[Dark,Account].png' -> ('pkg_Class_test[Account]', 'Dark')."""
    stem = file[: -len(".png")]
    match = re.match(r"^(.*)\[(.*)\]$", stem)
    if not match:
        return stem, ""
    params = match[2].split(",")
    theme = next((p for p in params if p in THEMES), "")
    rest = [p for p in params if p != theme]
    return match[1] + (f"[{','.join(rest)}]" if rest else ""), theme


def diff_panel(before, after):
    if before.size != after.size:
        before = before.resize(after.size)
    delta = ImageChops.difference(before, after).convert("L").point(lambda v: 255 if v > THRESHOLD else 0)
    ratio = sum(1 for v in delta.tobytes() if v) / (delta.width * delta.height)
    base = Image.blend(after.convert("L").convert("RGB"), Image.new("RGB", after.size, "white"), 0.6)
    base.paste(Image.new("RGB", after.size, (214, 40, 40)), mask=delta)
    return base, ratio


def placeholder(size, text):
    panel = Image.new("RGB", size, (236, 236, 236))
    ImageDraw.Draw(panel).text((12, 12), text, fill=(110, 110, 110))
    return panel


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--width", type=int, default=270)
    parser.add_argument("--quality", type=int, default=72)
    args = parser.parse_args()

    out = Path(args.out)
    (out / "img").mkdir(parents=True, exist_ok=True)
    before, after = goldens(args.base), goldens(None)

    groups = {}
    for module, file in sorted(set(before) | set(after)):
        test, theme = split(file)
        groups.setdefault((module, test), {})[theme] = (before.get((module, file)), after.get((module, file)))

    entries = []
    for index, ((module, test), themes) in enumerate(sorted(groups.items())):
        rows, info = [], {}
        for theme in sorted(themes, key=lambda t: THEMES.index(t) if t in THEMES else -1):
            old_path, new_path = themes[theme]
            old = load(args.base, old_path) if old_path else None
            new = load(None, new_path) if new_path else None
            size = (new or old).size
            if old and new:
                heat, ratio = diff_panel(old, new)
                status = "changed" if ratio > 0 else "same"
            else:
                heat, ratio = placeholder(size, "no pair"), None
                status = "new" if new else "removed"
            rows.append([old or placeholder(size, "not in base"), new or placeholder(size, "removed"), heat])
            info[theme or "default"] = {"status": status, "ratio": ratio}

        entry = {"module": module, "test": test, "themes": info, "image": None, "sheet": None}
        if any(v["status"] != "same" for v in info.values()):
            height = round(rows[0][0].height * args.width / rows[0][0].width)
            sheet = Image.new("RGB", (3 * args.width + 2 * GAP, len(rows) * height + (len(rows) - 1) * GAP), "white")
            for r, row in enumerate(rows):
                for c, panel in enumerate(row):
                    sheet.paste(panel.resize((args.width, height), Image.LANCZOS), (c * (args.width + GAP), r * (height + GAP)))
            name = f"img/{index:03d}.jpg"
            sheet.save(out / name, "JPEG", quality=args.quality, optimize=True)
            entry["image"] = name
            entry["sheet"] = {"width": sheet.width, "row": height, "gap": GAP}
        entries.append(entry)

    (out / "report.json").write_text(json.dumps(entries, indent=1))
    total = sum(f.stat().st_size for f in (out / "img").iterdir())
    statuses = [v["status"] for e in entries for v in e["themes"].values()]
    print(f"{len(entries)} tests, {len(statuses)} goldens: " + ", ".join(f"{s} {statuses.count(s)}" for s in sorted(set(statuses))))
    print(f"{len(list((out / 'img').iterdir()))} sheets, {total / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
