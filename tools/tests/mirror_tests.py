#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/tests/mirror_tests.py
"Mirror" tests: each Kotlin formula in core/calc is copied here in Python, line for line,
and checked against the worked examples written in the Kotlin comments. If a formula in
Kotlin changes, change it here too and re-run:   python3 tools/tests/mirror_tests.py
Also checks the exercise library data (assets/exercises.json) and the programs/ladders ids.
"""
import json, math, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
passed = 0


def check(name, got, want, tol=0.05):
    global passed
    ok = (abs(got - want) <= tol) if isinstance(want, (int, float)) and not isinstance(want, bool) else got == want
    if not ok:
        raise AssertionError(f"{name}: got {got!r}, want {want!r}")
    passed += 1


# ── StrengthMath.kt
def epley(w, r): return 0.0 if r <= 0 or w <= 0 else (w if r == 1 else w * (1 + r / 30))
def brzycki(w, r): return 0.0 if r <= 0 or w <= 0 or r >= 37 else (w if r == 1 else w * 36 / (37 - r))
def round_to(x, inc): return math.floor(x / inc + 0.5) * inc
def e1rm_rpe(w, r, rpe): return w * (1 + (r + (10 - rpe)) / 30)
def suggest(lw, lr, lrpe, tr, trpe, inc):
    if lrpe is None or trpe is None: return None
    raw = e1rm_rpe(lw, lr, lrpe) / (1 + (tr + (10 - trpe)) / 30)
    return round_to(min(max(raw, lw * 0.9), lw * 1.1), inc)
def warmups(work, bar, inc):
    out = []
    for p, r in [(0.5, 8), (0.7, 5), (0.85, 3)]:
        w = max(bar, round_to(work * p, inc))
        if w < work and all(o[0] != w for o in out): out.append((w, r))
    return out
def effective(mode, lb, body, pct):
    return lb if mode == "WEIGHT_REPS" else max(0.0, body * pct + lb)
def is_stalled(s): return len(s) >= 4 and max(s[-3:]) <= max(s[:-3])

check("epley 185x5", epley(185, 5), 215.83)
check("brzycki 185x5", brzycki(185, 5), 208.13)
check("epley 1 rep", epley(225, 1), 225)
check("pushup load", effective("BODYWEIGHT_REPS", 0, 180, 0.64), 115.2)
check("pullup +25", effective("BODYWEIGHT_REPS", 25, 180, 1.0), 205)
check("assisted pullup -40", effective("BODYWEIGHT_REPS", -40, 180, 1.0), 140)
check("rpe suggestion 185x5@7 -> 5@8", suggest(185, 5, 7, 5, 8, 5), 190)
check("rpe same effort keeps weight", suggest(185, 5, 8, 5, 8, 5), 185)
check("rpe cap +10%", suggest(100, 5, 5, 5, 9, 5), 110)
check("round half up", round_to(112.5, 5), 115)
check("round down", round_to(191.7, 5), 190)
check("warmups 225", warmups(225, 45, 5), [(115, 8), (160, 5), (190, 3)])
check("warmups light (empty bar, then 50)", warmups(60, 45, 5), [(45, 8), (50, 3)])
check("stalled", is_stalled([200, 210, 215, 214, 215, 213]), True)
check("not stalled", is_stalled([200, 210, 215, 214, 216, 213]), False)
check("deload weight", round_to(200 * 0.9, 5), 180)

# ── WorkoutScreen.kt stepWeight: ± buttons snap to 5 lb, or to 2.5 kg in metric
def step_weight(lb, d, metric, neg=False):
    if metric:
        kg = lb * 0.45359237
        s = (math.floor(kg / 2.5 + 1e-6) * 2.5 + 2.5) if d > 0 else (math.ceil(kg / 2.5 - 1e-6) * 2.5 - 2.5)
        nxt = s / 0.45359237
    else:
        nxt = (math.floor(lb / 5 + 1e-6) * 5 + 5) if d > 0 else (math.ceil(lb / 5 - 1e-6) * 5 - 5)
    return nxt if neg else max(0.0, nxt)
check("lb +", step_weight(135, 1, False), 140)
check("lb off-grid +", step_weight(137.8, 1, False), 140)
check("lb -", step_weight(135, -1, False), 130)
check("kg + snaps 61.2->62.5", step_weight(135, 1, True) * 0.45359237, 62.5, 1e-6)
check("kg - snaps 61.2->60", step_weight(135, -1, True) * 0.45359237, 60.0, 1e-6)
check("kg on grid +", step_weight(100 / 0.45359237, 1, True) * 0.45359237, 102.5, 1e-6)
check("no negative plates", step_weight(0, -1, False), 0)
check("assisted can go negative", step_weight(0, -1, False, True), -5)

# ── Format.kt cleanDecimal (Spanish decimal comma)
def clean_decimal(raw, neg=False, n=8):
    out = ""
    for i, ch in enumerate(raw.replace(",", ".")):
        if ch.isdigit(): out += ch
        elif ch == "." and "." not in out: out += ch
        elif ch == "-" and neg and i == 0: out += ch
    return out[:n]
check("comma -> dot", clean_decimal("62,5"), "62.5")
check("one dot only", clean_decimal("1.2.3"), "1.23")
check("minus only when allowed", clean_decimal("-20"), "20")
check("assist minus", clean_decimal("-20", True), "-20")
check("letters removed", clean_decimal("3.1 mi"), "3.1")

# ── PlateMath.kt
def per_side(target, bar, plates):
    if target <= bar: return []
    rem = (target - bar) / 2; used = []
    for p in sorted(plates, reverse=True):
        while rem + 1e-9 >= p: used.append(p); rem -= p
    return used
LB = [45, 35, 25, 10, 5, 2.5]; KG = [25, 20, 15, 10, 5, 2.5, 1.25]
check("plates 225", per_side(225, 45, LB), [45, 45])
check("plates 190", per_side(190, 45, LB), [45, 25, 2.5])
check("plates 135", per_side(135, 45, LB), [45])
check("plates 100kg", per_side(100, 20, KG), [25, 15])
check("plates 62.5kg", per_side(62.5, 20, KG), [20, 1.25])

# ── CalorieMath.kt
def kcal_met(met, lb, minutes): return met * (lb / 2.2046) * (minutes / 60)
def kcal_strength(lb, total_min, active_s):
    kg = lb / 2.2046; tot = max(total_min * 60, active_s); rest = tot - active_s
    return (5.0 * active_s / 3600 + 1.5 * rest / 3600) * kg
check("run 30 min 180 lb", kcal_met(9.8, 180, 30), 400.08, 0.1)
check("strength 60 min, 20x10", kcal_strength(180, 60, 20 * 10 * 3), 170.1, 0.2)
check("gym&nam weights 60 min", kcal_met(5.0, 180, 60), 408.2, 0.1)

# ── StreakMath.kt (Monday weeks, distinct days, earned freezes max 2)
def dow(d): return (d + 3) % 7
def week_start(d): return d - dow(d)
def streak(days, goal, today, maxf=2):
    cur = week_start(today)
    if not days: return (0, 0)
    per = {}
    for d in set(days): per[week_start(d)] = per.get(week_start(d), 0) + 1
    s = f = 0; w = week_start(min(days))
    while w < cur:
        c = per.get(w, 0)
        if c >= goal: s += 1; f = min(f + 1, maxf)
        elif s > 0 and f > 0: f -= 1
        else: s = 0
        w += 7
    if per.get(cur, 0) >= goal: s += 1
    return (s, f)
check("epochDay 0 is Thursday", dow(0), 3)
mon = week_start(20000)
three = lambda w: [w, w + 2, w + 4]
check("4 good weeks", streak(three(mon) + three(mon + 7) + three(mon + 14) + three(mon + 21), 3, mon + 28), (4, 2))
check("miss with freeze", streak(three(mon) + three(mon + 7) + [mon + 14] + three(mon + 21), 3, mon + 28), (3, 2))
check("freeze saves 1 miss", streak(three(mon) + [mon + 7] + three(mon + 14), 3, mon + 21), (2, 1))
check("2nd miss breaks streak", streak(three(mon) + [mon + 7] + [mon + 14] + three(mon + 21), 3, mon + 28), (1, 1))
check("two days same date count once", streak([mon, mon, mon], 2, mon + 7), (0, 0))

# ── BodyMath.kt (Navy, inches)
def navy(sex, h, neck, waist, hip=None):
    if sex == "M": return 86.010 * math.log10(waist - neck) - 70.041 * math.log10(h) + 36.76
    return 163.205 * math.log10(waist + hip - neck) - 97.684 * math.log10(h) - 78.387
check("navy man 70/15/34", navy("M", 70, 15, 34), 17.5, 0.1)
check("navy woman 65/13/30/38", navy("F", 65, 13, 30, 38), 28.56, 0.05)
check("kg exact", 100 * 0.45359237, 45.359237, 1e-9)

# ── GeoMath.kt
R = 3958.8
def hav(a, b):
    p1, p2 = math.radians(a[0]), math.radians(b[0]); dp = p2 - p1; dl = math.radians(b[1] - a[1])
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h))
check("0.01 deg lat", hav((47.6, -122.3), (47.61, -122.3)), 0.691, 0.002)
# 2 miles straight north at 10:00 /mi -> splits [600, 600]
step = 0.01 / 0.691                        # degrees per 0.01 mi... build points every 0.05 mi
pts = []; t = 0
for i in range(41):
    lat = 47.6 + i * (0.05 / 69.09); pts.append((lat, -122.3, t)); t += 30_000      # 0.05 mi each 30 s
def splits(points):
    out = []; dist = 0; nxt = 1.0; mark = points[0][2]
    for a, b in zip(points, points[1:]):
        seg = hav(a, b)
        while dist + seg >= nxt:
            frac = (nxt - dist) / seg; tt = a[2] + (b[2] - a[2]) * frac
            out.append((tt - mark) / 1000); mark = tt; nxt += 1
        dist += seg
    return out
sp = splits(pts)
check("two splits", len(sp), 2)
check("split ~600 s", sp[0], 600, 3)

# ── Csv.kt dates (Hinnant days_from_civil)
def date_of(z):
    z += 719468; era = (z if z >= 0 else z - 146096) // 146097; doe = z - era * 146097
    yoe = (doe - doe // 1460 + doe // 36524 - doe // 146096) // 365; y = yoe + era * 400
    doy = doe - (365 * yoe + yoe // 4 - yoe // 100); mp = (5 * doy + 2) // 153
    d = doy - (153 * mp + 2) // 5 + 1; m = mp + 3 if mp < 10 else mp - 9
    return f"{y + (1 if m <= 2 else 0)}-{m:02d}-{d:02d}"
import datetime
for z in [0, 20397, -1, 11016, 20763]:
    check(f"date {z}", date_of(z), (datetime.date(1970, 1, 1) + datetime.timedelta(days=z)).isoformat())

# ── Exercise library data
ex = json.load(open(os.path.join(ROOT, "app/src/main/assets/exercises.json"), encoding="utf-8"))
models = open(os.path.join(ROOT, "app/src/main/java/com/gymcat/app/core/model/Models.kt"), encoding="utf-8").read()
enum = lambda n: set(x.strip() for x in re.search(r"enum class " + n + r" \{([^}]*)\}", models).group(1).replace("\n", " ").split(",") if x.strip())
MU, EQ, TY, MO = enum("Muscle"), enum("Equipment"), enum("ExerciseType"), enum("LogMode")
check("300+ exercises", len(ex) >= 300, True)
check("unique ids", len({e["id"] for e in ex}), len(ex))
for e in ex:
    assert e["type"] in TY and e["equipment"] in EQ and e["mode"] in MO, e["id"]
    assert e["primary"] and all(m in MU for m in e["primary"] + e["secondary"]), e["id"]
    assert all(e[k].strip() for k in ["nameEn", "nameEs", "howToEn", "howToEs", "mistakesEn", "mistakesEs"]), e["id"]
    assert 0 <= e["bwPct"] <= 1.0, e["id"]
    assert 2.0 <= e["met"] <= 12.0, e["id"]
    assert (e["mode"] == "CARDIO") == (e["type"] == "CARDIO"), e["id"]
passed += 1
check("26 cat drawings", sum(1 for e in ex if e["catArt"]), 26)
check("calisthenics count", sum(1 for e in ex if e["type"] == "CALISTHENICS") >= 60, True)

# ── Program / ladder ids exist
ids = {e["id"] for e in ex}
prog = open(os.path.join(ROOT, "app/src/main/java/com/gymcat/app/core/programs/Programs.kt"), encoding="utf-8").read()
refs = set(re.findall(r'(?:lift|hold|cardio|stretch|LadderStep|TemplateItem)\("([a-z0-9_]+)"', prog))
missing = [r for r in refs if r not in ids]
check("program/ladder ids exist", missing, [])

print(f"ALL {passed} MIRROR CHECKS PASSED")
