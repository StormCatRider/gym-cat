#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/check_project.py
Static checks that catch common mistakes WITHOUT compiling (Claude's workspace can't run Gradle):
  1. every R.string / R.array / @string used in code exists in English AND Spanish files
  2. English and Spanish have the same keys and the same %1$s / %1$d placeholders
  3. every Kotlin file has balanced { } ( ) [ ] and starts with a PLAIN-ENGLISH GUIDE box
  4. core/ (formulas + models) has NO Android/Java imports (ready for Kotlin Multiplatform)
  5. every "import com.gymcat.app.X" points to something that really exists
  6. every exercise id written in Kotlin code exists in assets/exercises.json
  7. no copyrighted-looking assets (only our own XML/PNG; no fonts, no downloaded media)
Run: python3 tools/check_project.py
"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main")
JAVA = os.path.join(SRC, "java")
errors = []


def kt_files():
    for d, _, fs in os.walk(JAVA):
        for f in fs:
            if f.endswith(".kt"):
                yield os.path.join(d, f)


def strip_code(t):
    """Removes comments, strings and chars in ONE pass (so "*/*" inside a string isn't a comment)."""
    out, i, n = [], 0, len(t)
    while i < n:
        if t.startswith('//', i):
            j = t.find('\n', i); i = n if j < 0 else j
        elif t.startswith('/*', i):
            j = t.find('*/', i + 2); i = n if j < 0 else j + 2
        elif t.startswith('"""', i):
            j = t.find('"""', i + 3); i = n if j < 0 else j + 3; out.append('""')
        elif t[i] == "'" and re.match(r"'(\\.|[^'\\])'", t[i:i + 4] or ''):
            m = re.match(r"'(\\.|[^'\\])'", t[i:]); i += len(m.group(0)); out.append("''")
        elif t[i] == '"':
            i += 1; depth = 0
            while i < n:
                if t[i] == '\\': i += 2; continue
                if t[i] == '$' and i + 1 < n and t[i + 1] == '{': depth += 1; i += 2; continue
                if t[i] == '}' and depth > 0: depth -= 1; i += 1; continue
                if t[i] == '"' and depth == 0: break
                i += 1
            out.append('""'); i += 1
        else:
            out.append(t[i]); i += 1
    return ''.join(out)


def load_res(lang_dir):
    p = os.path.join(SRC, "res", lang_dir, "strings.xml")
    x = open(p, encoding="utf-8").read()
    strings = dict(re.findall(r'<string name="([a-z0-9_]+)"[^>]*>(.*?)</string>', x, re.S))
    arrays = {k: re.findall(r'<item>(.*?)</item>', v, re.S) for k, v in re.findall(r'<string-array name="([a-z0-9_]+)">(.*?)</string-array>', x, re.S)}
    return strings, arrays


en_s, en_a = load_res("values")
es_s, es_a = load_res("values-es")

# 1 + 2: resources
code = "".join(open(f, encoding="utf-8").read() for f in kt_files())
xml_text = ""
for d, _, fs in os.walk(SRC):
    for f in fs:
        if f.endswith(".xml") and "values" not in d:
            xml_text += open(os.path.join(d, f), encoding="utf-8").read()
used_s = set(re.findall(r'R\.string\.([a-z0-9_]+)', code)) | set(re.findall(r'@string/([a-z0-9_]+)', xml_text))
used_a = set(re.findall(r'R\.array\.([a-z0-9_]+)', code))
for k in sorted(used_s):
    if k not in en_s: errors.append(f"missing English string: {k}")
    if k not in es_s: errors.append(f"missing Spanish string: {k}")
for k in sorted(used_a):
    if k not in en_a: errors.append(f"missing English array: {k}")
    if k not in es_a: errors.append(f"missing Spanish array: {k}")
for k in set(en_s) ^ set(es_s): errors.append(f"key only in one language: {k}")
ph = re.compile(r'%\d\$[sd]')
for k in en_s:
    if k in es_s and sorted(ph.findall(en_s[k])) != sorted(ph.findall(es_s[k])):
        errors.append(f"placeholder mismatch EN/ES: {k}")
for k in en_a:
    if len(en_a[k]) != len(es_a.get(k, [])): errors.append(f"array length mismatch: {k}")
unused = set(en_s) - used_s
print(f"[1-2] strings: {len(en_s)} EN / {len(es_s)} ES, arrays {len(en_a)}; used {len(used_s)}; unused {len(unused)}")

# Enum order vs arrays
models = open(os.path.join(JAVA, "com/gymcat/app/core/model/Models.kt"), encoding="utf-8").read()
def enum_items(name):
    m = re.search(r'enum class ' + name + r' \{([^}]*)\}', models)
    return [x.strip() for x in m.group(1).replace("\n", " ").split(",") if x.strip()]
for enum, arr in [("Muscle", "muscle_names"), ("Equipment", "equipment_names"), ("ExerciseType", "exercise_types"),
                  ("LogMode", "log_modes"), ("Level", "levels"), ("Goal", "goals"), ("EquipmentAccess", "equipment_access")]:
    if len(enum_items(enum)) != len(en_a[arr]): errors.append(f"enum {enum} has {len(enum_items(enum))} items but array {arr} has {len(en_a[arr])}")

# 3: brackets + guide box
for f in kt_files():
    t = open(f, encoding="utf-8").read()
    rel = os.path.relpath(f, ROOT)
    if "PLAIN-ENGLISH GUIDE" not in t[:400]: errors.append(f"no guide box: {rel}")
    s = strip_code(t)
    for a, b in ["{}", "()", "[]"]:
        if s.count(a) != s.count(b): errors.append(f"unbalanced {a}{b} in {rel}: {s.count(a)} vs {s.count(b)}")
print(f"[3] kotlin files: {len(list(kt_files()))}")

# 4: core purity
for f in kt_files():
    if "/core/" in f:
        for imp in re.findall(r'^import\s+([\w.]+)', open(f, encoding="utf-8").read(), re.M):
            if imp.startswith(("android", "java.", "javax.")):
                errors.append(f"core file uses platform import {imp}: {os.path.relpath(f, ROOT)}")
        if re.search(r'\bMath\.|java\.', strip_code(open(f, encoding="utf-8").read())):
            errors.append(f"core file uses JVM-only API: {os.path.relpath(f, ROOT)}")

# 5: internal imports resolve
decls = {}
for f in kt_files():
    t = open(f, encoding="utf-8").read()
    pkg = re.search(r'^package\s+([\w.]+)', t, re.M).group(1)
    for name in re.findall(r'^(?:@\w+(?:\([^)]*\))?\s*)*(?:private\s+|internal\s+)?(?:data\s+|abstract\s+|enum\s+)?(?:class|object|interface|fun|val|const val)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)', t, re.M):
        decls.setdefault(pkg + "." + name, f)
for f in kt_files():
    t = open(f, encoding="utf-8").read()
    for imp in re.findall(r'^import\s+(com\.gymcat\.app\.[\w.]+)', t, re.M):
        if imp.endswith(".*") or imp in decls or imp in ("com.gymcat.app.R", "com.gymcat.app.BuildConfig"): continue
        if imp.rsplit(".", 1)[0] in decls: continue           # nested (Muscle.CHEST style)
        errors.append(f"unresolved import {imp} in {os.path.relpath(f, ROOT)}")
    dup = [i for i in re.findall(r'^import\s+([\w.]+)', t, re.M)]
    if len(dup) != len(set(dup)): errors.append(f"duplicate import in {os.path.relpath(f, ROOT)}")

# 6: exercise ids in code exist
ex = json.load(open(os.path.join(SRC, "assets", "exercises.json"), encoding="utf-8"))
ids = {e["id"] for e in ex}
id_like = re.compile(r'"((?:bb|db|pu|pl|sq|st|kb|cable|machine|smith)_[a-z0-9_]+|plank|lsit|tuck_lsit|hollow_hold|dead_bug|inverted_row|glute_bridge_bw|pike_pushup|pike_pushup_elevated|wall_handstand_hold|hspu_wall|dip_bench|dip_parallel|dip_weighted|muscle_up|muscle_up_jumping|lat_pulldown_wide|leg_curl_lying|hanging_knee_raise|treadmill_run|outdoor_run|stationary_bike|rowing_machine|jump_rope|trap_bar_deadlift|sumo_deadlift|sprints)"')
for f in kt_files():
    for m in id_like.findall(open(f, encoding="utf-8").read()):
        if m not in ids and not m.startswith(("bw_", "st_")) or (m.startswith("st_") and m not in ids and "streak" not in m):
            if m not in ids: errors.append(f"unknown exercise id \"{m}\" in {os.path.relpath(f, ROOT)}")
print(f"[6] exercises in library: {len(ex)}")

# 7: assets — only expected kinds
for d, _, fs in os.walk(os.path.join(SRC, "res")):
    for f in fs:
        if f.endswith((".ttf", ".otf", ".mp3", ".wav", ".mp4")):
            errors.append(f"media/font file found (check license): {f}")

if errors:
    print("\nPROBLEMS:"); [print(" -", e) for e in errors]; sys.exit(1)
print("\nALL STATIC CHECKS PASSED")
