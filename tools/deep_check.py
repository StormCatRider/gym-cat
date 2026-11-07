#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/deep_check.py
Deeper static checks (still no compiling):
  A. Calls to the app's own functions use parameter names that really exist
  B. Room SQL: every column/table named in @Query exists in the entity classes
  C. Crash patterns: maxOf/minOf/first()/last()/!! on things that can be empty or null
  D. String placeholders: getString/stringResource(R.string.x, args) passes as many args as %n$ in the text
  E. Every R.drawable.x exists; every class in AndroidManifest.xml exists
Run: python3 tools/deep_check.py   (prints WARN lines to review; FAIL lines must be fixed)
"""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main")
JAVA = os.path.join(SRC, "java")
files = [os.path.join(d, f) for d, _, fs in os.walk(JAVA) for f in fs if f.endswith(".kt")]
text = {f: open(f, encoding="utf-8").read() for f in files}
fails, warns = [], []
rel = lambda f: os.path.relpath(f, ROOT)

# ── A. named parameters of our own functions
sig = {}
for f, t in text.items():
    for m in re.finditer(r'\bfun\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)\s*\(', t):
        i = m.end(); depth = 1; j = i
        while j < len(t) and depth:
            depth += {'(': 1, ')': -1}.get(t[j], 0); j += 1
        params = t[i:j - 1]
        names = set(re.findall(r'(?:^|,)\s*(?:@\w+\s+)?(?:vararg\s+|noinline\s+|crossinline\s+)?(\w+)\s*:', params, re.S))
        sig.setdefault(m.group(1), []).append(names)
for cls in re.finditer(r'(?:data\s+)?class\s+(\w+)\s*(?:<[^>]+>)?\s*\(', "\n".join(text.values())):
    pass
# data classes / classes constructor params
allt = "\n".join(text.values())
for m in re.finditer(r'class\s+(\w+)\s*(?:<[^>]+>)?\s*(?:@\w+\s*)?(?:constructor\s*)?\(', allt):
    i = m.end(); depth = 1; j = i
    while j < len(allt) and depth:
        depth += {'(': 1, ')': -1}.get(allt[j], 0); j += 1
    names = set(re.findall(r'(?:val|var)\s+(\w+)\s*:', allt[i:j - 1])) | set(re.findall(r'(?:^|,)\s*(\w+)\s*:', allt[i:j - 1]))
    sig.setdefault(m.group(1), []).append(names)

for f, t in text.items():
    for m in re.finditer(r'(?<![\w.])(\w+)\s*\(', t):
        name = m.group(1)
        if name not in sig: continue
        i = m.end(); depth = 1; j = i
        while j < len(t) and depth:
            depth += {'(': 1, ')': -1, '{': 0}.get(t[j], 0); j += 1
        args = t[i:j - 1]
        # only top-level "name =" (skip nested parens/lambdas)
        top, d = [], 0
        for k, ch in enumerate(args):
            if ch in '([{': d += 1
            elif ch in ')]}': d -= 1
            elif d == 0: top.append(k)
        flat = ''.join(args[k] if k in set(top) else ' ' for k in range(len(args)))
        used = set(re.findall(r'(?:^|,)\s*(\w+)\s*=(?!=)', flat))
        if not used: continue
        if not any(used <= s for s in sig[name]):
            missing = used - set().union(*sig[name])
            if missing: fails.append(f"A {rel(f)}: {name}(…) uses unknown parameter(s) {sorted(missing)}")

# ── B. Room SQL vs entities
ent_src = text[os.path.join(JAVA, "com/gymcat/app/data/db/Entities.kt")]
tables = {}
for m in re.finditer(r'@Entity\(tableName = "(\w+)"[\s\S]*?\ndata class \w+\(([\s\S]*?)\n\)', ent_src):
    tables[m.group(1)] = set(re.findall(r'val\s+(\w+)\s*:', m.group(2)))
for m in re.finditer(r'@Entity\(tableName = "(\w+)"\)\s*\ndata class \w+\(([^\n]*)\)\n', ent_src):
    tables.setdefault(m.group(1), set(re.findall(r'val\s+(\w+)\s*:', m.group(2))))
dao = text[os.path.join(JAVA, "com/gymcat/app/data/db/GymDao.kt")]
cols_all = set().union(*tables.values())
for q in re.findall(r'@Query\(\s*"""([\s\S]*?)"""\s*\)|@Query\("([^"]*)"\)', dao):
    sql = q[0] or q[1]
    for tname in re.findall(r'(?:FROM|JOIN|UPDATE|INTO)\s+(\w+)', sql, re.I):
        if tname not in tables: fails.append(f"B unknown table {tname} in: {sql[:60]}")
    for col in re.findall(r'\b(?:\w+\.)?(\w+)\s*(?:=|>|<|IN\b|DESC|ASC|,)', sql):
        if col.lower() in {"jo", "select", "from", "where", "and", "or", "order", "by", "limit", "join", "on", "set", "desc", "asc", "s", "w", "s2", "w2", "id"} or col.isdigit(): continue
        if col not in cols_all and not col.startswith(":"): warns.append(f"B check column '{col}' in: {sql.strip()[:70]}")

# ── C. crash patterns
for f, t in text.items():
    for i, line in enumerate(t.split("\n"), 1):
        s = line.strip()
        if s.startswith("//") or s.startswith("*"): continue
        if re.search(r'\.(maxOf|minOf|max|min|first|last|reduce)\s*[({]', s) and "OrNull" not in s:
            warns.append(f"C {rel(f)}:{i}: {s[:110]}")
        if "!!" in s:
            warns.append(f"C!! {rel(f)}:{i}: {s[:110]}")

# ── D. placeholders vs args
strings = dict(re.findall(r'<string name="(\w+)"[^>]*>(.*?)</string>', open(os.path.join(SRC, "res/values/strings.xml"), encoding="utf-8").read(), re.S))
for f, t in text.items():
    for m in re.finditer(r'(?:stringResource|getString)\(\s*R\.string\.(\w+)\s*([,)])', t):
        key = m.group(1); n_needed = len(set(re.findall(r'%(\d)\$', strings.get(key, ""))))
        i = m.end(); depth = 1; j = i
        if m.group(2) == ')': n_given = 0
        else:
            while j < len(t) and depth:
                depth += {'(': 1, ')': -1}.get(t[j], 0); j += 1
            args = t[i:j - 1]; d = 0; n_given = 1
            for ch in args:
                if ch in '([{': d += 1
                elif ch in ')]}': d -= 1
                elif ch == ',' and d == 0: n_given += 1
        # "val x = stringResource(R.string.k)" then x.format(...) later is fine: check the variable is formatted
        var = re.search(r'val\s+(\w+)\s*=\s*$', t[max(0, m.start() - 60):m.start()])
        if n_given == 0 and n_needed > 0 and var and re.search(re.escape(var.group(1)) + r'\.format\(', t): continue
        if n_needed != n_given:
            fails.append(f"D {rel(f)}: R.string.{key} needs {n_needed} arg(s), given {n_given}")
# strings used via .format() after stringResource
for f, t in text.items():
    for key in re.findall(r'stringResource\(R\.string\.(\w+)\)\s*\n?.*?\.format', t):
        pass

# ── E. drawables + manifest classes
res_names = {os.path.splitext(fn)[0] for d, _, fs in os.walk(os.path.join(SRC, "res")) if "drawable" in d or "mipmap" in d for fn in fs}
for f, t in text.items():
    for d in re.findall(r'R\.drawable\.(\w+)', t):
        if d not in res_names: fails.append(f"E missing drawable {d} in {rel(f)}")
man = open(os.path.join(SRC, "AndroidManifest.xml"), encoding="utf-8").read()
declared = {re.search(r'^package\s+([\w.]+)', t, re.M).group(1) + "." + c for t in text.values() for c in re.findall(r'^class\s+(\w+)', t, re.M)}
for c in re.findall(r'android:name="(\.[\w.]+)"', man):
    full = "com.gymcat.app" + c
    if full not in declared: fails.append(f"E manifest class not found: {full}")

for w in warns: print("WARN", w)
for x in fails: print("FAIL", x)
print(f"\n{len(fails)} FAIL, {len(warns)} WARN to review")
sys.exit(1 if fails else 0)
