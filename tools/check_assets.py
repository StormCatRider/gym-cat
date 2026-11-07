#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/check_assets.py
Lists which cat images and meow sounds you've added and which are still missing.
Run:  python3 tools/check_assets.py
"""
import os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
guide = open(os.path.join(ROOT, "docs", "ASSETS_GUIDE.md"), encoding="utf-8").read()
wanted = re.findall(r"`(cat_\d\d_[a-z_]+)\.png`", guide)

found = {}
for d, _, fs in os.walk(RES):
    for f in fs:
        found[os.path.splitext(f)[0]] = os.path.join(d, f)

bad = [f for f in found if not re.fullmatch(r"[a-z0-9_]+", f)]
print(f"Cat images: {sum(1 for w in wanted if w in found)} of {len(wanted)} added")
for w in wanted:
    print(("  ✓ " if w in found else "  · missing ") + w)
meows = sorted(k for k in found if re.fullmatch(r"meow_[1-9]", k))
print(f"Meows: {len(meows)} found {meows}")
if bad:
    print("Names Android will reject (use only a-z, 0-9, _):", bad)
