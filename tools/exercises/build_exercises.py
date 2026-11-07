#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/exercises/build_exercises.py
Builds app/src/main/assets/exercises.json (the built-in exercise library).

Why a script? Many exercises are "variants" of one movement (barbell / dumbbell /
Smith bench press). Variants share the same how-to and mistakes text, so it is
written once here and copied to each variant. All text is ORIGINAL (written for
Gym Cat, not copied from any book, site or app).

Run:  python3 tools/exercises/build_exercises.py
Then: python3 tools/tests/mirror_tests.py   (checks the file)
"""
import json, os

# Muscle short names → app names (core/model/Models.kt Muscle enum)
M = dict(CH="CHEST", LA="LATS", UB="UPPER_BACK", TR="TRAPS", FD="FRONT_DELTS", SD="SIDE_DELTS",
         RD="REAR_DELTS", BI="BICEPS", TRI="TRICEPS", FA="FOREARMS", AB="ABS", OB="OBLIQUES",
         LB="LOWER_BACK", GL="GLUTES", QU="QUADS", HA="HAMSTRINGS", CA="CALVES", AD="ADDUCTORS")

OUT = []

# Cat drawings you will make (cat_##_name.png). Only these exercises get one.
CAT = {
    "bb_back_squat": "cat_17_squat", "bb_deadlift": "cat_18_deadlift", "bb_bench_press": "cat_19_bench",
    "bb_ohp": "cat_20_ohp", "bb_row": "cat_21_row", "bb_rdl": "cat_22_rdl", "bb_hip_thrust": "cat_23_hip_thrust",
    "leg_press": "cat_24_leg_press", "lat_pulldown_wide": "cat_25_lat_pulldown", "db_curl": "cat_26_curl",
    "pushdown_bar": "cat_27_pushdown", "db_lateral_raise": "cat_28_lateral_raise",
    "db_lunge_walking": "cat_29_lunge", "pu_standard": "cat_30_pushup", "pl_pullup": "cat_31_pullup",
    "dip_parallel": "cat_32_dip", "plank": "cat_33_plank", "crunch": "cat_34_crunch",
    "sq_pistol": "cat_35_pistol", "wall_handstand_hold": "cat_36_handstand",
    "treadmill_run": "cat_37_treadmill", "stationary_bike": "cat_38_bike", "rowing_machine": "cat_39_rower",
    "jump_rope": "cat_40_jump_rope", "st_hamstring_fold": "cat_41_hamstring", "st_down_dog": "cat_42_down_dog",
}

EMOJI = {"STRENGTH": "🏋️", "CALISTHENICS": "🤸", "CARDIO": "🏃", "STRETCH": "🧘"}


def group(text, variants, type_="STRENGTH", pri="", sec="", mode="WEIGHT_REPS",
          compound=False, bw=0.0, met=None, emoji=None):
    """text = (howto_en, howto_es, mistakes_en, mistakes_es)
    variants = list of (id, name_en, name_es, equipment[, overrides dict])"""
    for v in variants:
        vid, en, es, eq = v[:4]
        o = v[4] if len(v) > 4 else {}
        t = o.get("type", type_)
        OUT.append(dict(
            id=vid, nameEn=en, nameEs=es, type=t, equipment=eq,
            primary=[M[x] for x in o.get("pri", pri).split()],
            secondary=[M[x] for x in o.get("sec", sec).split()],
            mode=o.get("mode", mode),
            howToEn=o.get("howEn", text[0]), howToEs=o.get("howEs", text[1]),
            mistakesEn=o.get("misEn", text[2]), mistakesEs=o.get("misEs", text[3]),
            bwPct=o.get("bw", bw), compound=o.get("compound", compound),
            emoji=o.get("emoji", emoji or EMOJI[t]), catArt=CAT.get(vid),
            met=o.get("met", met if met is not None else {"STRENGTH": 5.0, "CALISTHENICS": 3.8,
                                                           "CARDIO": 7.0, "STRETCH": 2.3}[t]),
        ))


def single(vid, en, es, eq, howEn, howEs, misEn, misEs, **kw):
    group((howEn, howEs, misEn, misEs), [(vid, en, es, eq)], **kw)


# ═════════════════════════════ CHEST ═════════════════════════════
group(("Lie on the bench, eyes under the bar, feet flat. Lower to mid-chest with elbows about 45° from your body, then press up.",
       "Acuéstate en la banca con los ojos bajo la barra y los pies firmes. Baja al centro del pecho con los codos a unos 45° y empuja hacia arriba.",
       "Bouncing off the chest, flaring elbows to 90°, lifting the hips off the bench.",
       "Rebotar en el pecho, abrir los codos a 90°, despegar la cadera de la banca."),
      [("bb_bench_press", "Bench press (barbell)", "Press de banca (barra)", "BARBELL"),
       ("db_bench_press", "Bench press (dumbbell)", "Press de banca (mancuernas)", "DUMBBELL"),
       ("smith_bench_press", "Bench press (Smith)", "Press de banca (Smith)", "SMITH"),
       ("db_bench_neutral", "Neutral-grip dumbbell press", "Press con mancuernas agarre neutro", "DUMBBELL"),
       ("bb_floor_press", "Floor press (barbell)", "Press en el piso (barra)", "BARBELL"),
       ("db_floor_press", "Floor press (dumbbell)", "Press en el piso (mancuernas)", "DUMBBELL")],
      pri="CH", sec="FD TRI", compound=True)
group(("Set the bench to 30–45°. Lower to the upper chest, then press up and slightly back.",
       "Pon la banca a 30–45°. Baja a la parte alta del pecho y empuja hacia arriba y un poco atrás.",
       "Bench too steep (turns into a shoulder press), bar drifting toward the belly.",
       "Banca muy inclinada (se vuelve press de hombro), la barra se va hacia el abdomen."),
      [("bb_incline_press", "Incline press (barbell)", "Press inclinado (barra)", "BARBELL"),
       ("db_incline_press", "Incline press (dumbbell)", "Press inclinado (mancuernas)", "DUMBBELL"),
       ("smith_incline_press", "Incline press (Smith)", "Press inclinado (Smith)", "SMITH"),
       ("machine_incline_press", "Incline chest press (machine)", "Press inclinado (máquina)", "MACHINE")],
      pri="CH FD", sec="TRI", compound=True)
group(("Lie on a decline bench with legs secured. Lower to the lower chest and press up.",
       "Acuéstate en banca declinada con las piernas sujetas. Baja a la parte baja del pecho y empuja.",
       "Dropping the weight fast, losing the leg hold.",
       "Bajar el peso muy rápido, soltar el apoyo de las piernas."),
      [("bb_decline_press", "Decline press (barbell)", "Press declinado (barra)", "BARBELL"),
       ("db_decline_press", "Decline press (dumbbell)", "Press declinado (mancuernas)", "DUMBBELL")],
      pri="CH", sec="TRI FD", compound=True)
group(("Sit tall with the back on the pad, handles at chest height. Press forward until arms are nearly straight, then return slowly.",
       "Siéntate derecho con la espalda en el respaldo y las manijas a la altura del pecho. Empuja al frente casi hasta estirar y regresa lento.",
       "Seat too low or high, shoulders rolling forward at the end.",
       "Asiento muy bajo o muy alto, hombros que se van hacia adelante al final."),
      [("machine_chest_press", "Chest press (machine)", "Press de pecho (máquina)", "MACHINE"),
       ("cable_chest_press", "Chest press (cable)", "Press de pecho (polea)", "CABLE"),
       ("band_chest_press", "Chest press (band)", "Press de pecho (liga)", "BAND")],
      pri="CH", sec="FD TRI", compound=True)
group(("With a slight bend in the elbows, open the arms wide in an arc until you feel a chest stretch, then hug back together.",
       "Con los codos un poco doblados, abre los brazos en arco hasta sentir el pecho estirado y vuelve como si abrazaras.",
       "Bending the elbows too much (turns into a press), going too deep for the shoulders.",
       "Doblar mucho los codos (se vuelve press), bajar demasiado para los hombros."),
      [("db_fly", "Chest fly (dumbbell)", "Aperturas (mancuernas)", "DUMBBELL"),
       ("cable_fly", "Chest fly (cable)", "Aperturas (polea)", "CABLE"),
       ("pec_deck", "Pec deck (machine)", "Pec deck (máquina)", "MACHINE"),
       ("db_incline_fly", "Incline fly (dumbbell)", "Aperturas inclinadas (mancuernas)", "DUMBBELL"),
       ("cable_incline_fly", "Incline fly (cable)", "Aperturas inclinadas (polea)", "CABLE"),
       ("cable_crossover_low", "Low-to-high cable fly", "Aperturas en polea de abajo hacia arriba", "CABLE")],
      pri="CH", sec="FD")
single("plate_press", "Plate squeeze press", "Press apretando disco", "PLATE",
       "Squeeze a plate between your palms at chest height and press it straight out, keep squeezing.",
       "Aprieta un disco entre las palmas a la altura del pecho y empújalo al frente sin dejar de apretar.",
       "Letting the squeeze relax during the press.", "Dejar de apretar mientras empujas.",
       pri="CH", sec="FD TRI")
single("landmine_press", "Landmine press", "Press landmine", "BARBELL",
       "Hold the end of a barbell anchored in a corner at shoulder height. Press up and forward, one arm or both.",
       "Sostén la punta de una barra anclada en una esquina a la altura del hombro. Empuja arriba y al frente, con uno o dos brazos.",
       "Arching the low back to push the bar.", "Arquear la espalda baja para empujar.",
       pri="FD CH", sec="TRI", compound=True)

# ═════════════════════════════ BACK ═════════════════════════════
group(("Bar over mid-foot, hinge and grip just outside the legs. Brace, flat back, push the floor away and stand tall. Lower the same way.",
       "Barra sobre el medio del pie, inclínate y agarra por fuera de las piernas. Aprieta el abdomen, espalda recta, empuja el piso y párate derecho. Baja igual.",
       "Rounding the lower back, bar drifting away from the legs, jerking it off the floor.",
       "Redondear la espalda baja, alejar la barra de las piernas, jalar de golpe desde el piso."),
      [("bb_deadlift", "Deadlift (barbell)", "Peso muerto (barra)", "BARBELL"),
       ("trap_bar_deadlift", "Deadlift (trap bar)", "Peso muerto (barra hexagonal)", "TRAP_BAR"),
       ("sumo_deadlift", "Sumo deadlift", "Peso muerto sumo", "BARBELL", {"sec": "AD LB QU"}),
       ("deficit_deadlift", "Deficit deadlift", "Peso muerto con déficit", "BARBELL"),
       ("rack_pull", "Rack pull", "Jalón desde rack", "BARBELL", {"pri": "LB TR GL"}),
       ("kb_deadlift", "Deadlift (kettlebell)", "Peso muerto (pesa rusa)", "KETTLEBELL")],
      pri="GL HA LB", sec="TR QU FA", compound=True)
group(("Hinge forward with a flat back, about 45° or lower. Pull the weight to the lower ribs, squeeze the shoulder blades, lower slowly.",
       "Inclínate con la espalda recta, a unos 45° o más. Jala el peso hacia las costillas bajas, junta los omóplatos y baja lento.",
       "Standing up more each rep, using the hips to swing the weight.",
       "Enderezarte cada repetición, usar la cadera para balancear el peso."),
      [("bb_row", "Bent-over row (barbell)", "Remo inclinado (barra)", "BARBELL"),
       ("pendlay_row", "Pendlay row", "Remo Pendlay", "BARBELL"),
       ("smith_row", "Bent-over row (Smith)", "Remo inclinado (Smith)", "SMITH"),
       ("tbar_row", "T-bar row", "Remo en T", "BARBELL"),
       ("meadows_row", "Meadows row", "Remo Meadows", "BARBELL"),
       ("band_row", "Row (band)", "Remo (liga)", "BAND"),
       ("kb_row", "Row (kettlebell)", "Remo (pesa rusa)", "KETTLEBELL")],
      pri="UB LA", sec="RD BI LB", compound=True)
group(("One hand and knee on a bench, back flat. Pull the dumbbell toward the hip, elbow close, then lower with control.",
       "Una mano y una rodilla en la banca, espalda recta. Jala la mancuerna hacia la cadera con el codo pegado y baja con control.",
       "Twisting the torso to lift more, shrugging the shoulder up.",
       "Girar el torso para subir más, encoger el hombro."),
      [("db_row_one_arm", "One-arm row (dumbbell)", "Remo a una mano (mancuerna)", "DUMBBELL"),
       ("landmine_row", "Landmine row", "Remo landmine", "BARBELL"),
       ("kroc_row", "Heavy one-arm row (high reps)", "Remo pesado a una mano (muchas reps)", "DUMBBELL")],
      pri="LA UB", sec="RD BI", compound=True)
group(("Chest on the pad so the back can't cheat. Pull the handles back, squeeze the shoulder blades, return slowly.",
       "Pecho apoyado en el respaldo para no hacer trampa. Jala las manijas hacia atrás, junta los omóplatos y regresa lento.",
       "Lifting the chest off the pad, cutting the range short.",
       "Despegar el pecho del respaldo, hacer el movimiento a medias."),
      [("db_chest_supported_row", "Chest-supported row (dumbbell)", "Remo con pecho apoyado (mancuernas)", "DUMBBELL"),
       ("machine_row", "Seated row (machine)", "Remo sentado (máquina)", "MACHINE"),
       ("seal_row", "Seal row", "Remo foca", "BARBELL")],
      pri="UB LA", sec="RD BI", compound=True)
single("cable_row_seated", "Seated cable row", "Remo sentado en polea", "CABLE",
       "Sit tall, knees soft. Pull the handle to the belly, chest up, then let the arms reach forward without rounding.",
       "Siéntate derecho con las rodillas un poco dobladas. Jala la manija al abdomen con el pecho arriba y deja que los brazos regresen sin encorvarte.",
       "Rocking the torso back and forth to move the weight.", "Mecer el torso para mover el peso.",
       pri="UB LA", sec="RD BI", compound=True)
group(("Sit with thighs under the pad. Pull the bar to the upper chest, elbows down toward the ribs, then let it rise slowly.",
       "Siéntate con los muslos bajo el soporte. Jala la barra a la parte alta del pecho llevando los codos hacia las costillas y súbela lento.",
       "Leaning far back, pulling behind the neck, letting the weight yank the arms up.",
       "Echarse muy atrás, jalar por detrás de la nuca, dejar que el peso jale los brazos de golpe."),
      [("lat_pulldown_wide", "Lat pulldown (wide grip)", "Jalón al pecho (agarre abierto)", "CABLE"),
       ("lat_pulldown_close", "Lat pulldown (close V-grip)", "Jalón al pecho (agarre cerrado en V)", "CABLE"),
       ("lat_pulldown_underhand", "Lat pulldown (underhand)", "Jalón al pecho (agarre supino)", "CABLE"),
       ("lat_pulldown_single", "Single-arm cable pulldown", "Jalón a una mano en polea", "CABLE"),
       ("assisted_pullup_machine", "Assisted pull-up (machine)", "Dominada asistida (máquina)", "MACHINE")],
      pri="LA", sec="BI UB RD", compound=True)
group(("Arms nearly straight, hinge slightly. Sweep the bar or rope down to the thighs using the lats, then return slowly.",
       "Brazos casi rectos, inclínate un poco. Lleva la barra o cuerda hasta los muslos usando la espalda y regresa lento.",
       "Bending the elbows so it becomes a triceps pushdown.",
       "Doblar los codos y convertirlo en extensión de tríceps."),
      [("straight_arm_pulldown", "Straight-arm pulldown", "Jalón con brazos rectos", "CABLE"),
       ("machine_pullover", "Pullover (machine)", "Pullover (máquina)", "MACHINE"),
       ("db_pullover", "Pullover (dumbbell)", "Pullover (mancuerna)", "DUMBBELL", {"sec": "CH TRI"})],
      pri="LA", sec="TRI")
group(("Hold the weight at your sides and lift the shoulders straight up toward the ears, pause, lower slowly.",
       "Sostén el peso a los lados y sube los hombros hacia las orejas, pausa y baja lento.",
       "Rolling the shoulders in circles, using a bounce.", "Girar los hombros en círculos, rebotar."),
      [("bb_shrug", "Shrug (barbell)", "Encogimientos (barra)", "BARBELL"),
       ("db_shrug", "Shrug (dumbbell)", "Encogimientos (mancuernas)", "DUMBBELL"),
       ("trap_bar_shrug", "Shrug (trap bar)", "Encogimientos (barra hexagonal)", "TRAP_BAR"),
       ("cable_shrug", "Shrug (cable)", "Encogimientos (polea)", "CABLE")],
      pri="TR", sec="FA")
group(("Hips on the pad, body straight. Lower the chest by bending at the hips, then raise until the body is in a line. Don't overarch.",
       "Cadera en el soporte, cuerpo recto. Baja el pecho doblando la cadera y sube hasta quedar en línea, sin arquear de más.",
       "Swinging up fast and hyperextending the low back.", "Subir de golpe y arquear de más la espalda baja."),
      [("back_extension", "Back extension", "Extensión de espalda", "MACHINE"),
       ("reverse_hyper", "Reverse hyperextension", "Hiperextensión inversa", "MACHINE", {"pri": "GL LB"}),
       ("glute_ham_raise", "Glute-ham raise", "Elevación glúteo-femoral", "MACHINE", {"pri": "HA GL"})],
      pri="LB GL", sec="HA")
single("good_morning", "Good morning", "Buenos días (good morning)", "BARBELL",
       "Bar on the upper back, knees soft. Push the hips back with a flat back until the torso is near parallel, then stand up.",
       "Barra en la parte alta de la espalda, rodillas un poco dobladas. Lleva la cadera atrás con la espalda recta hasta casi paralelo y sube.",
       "Rounding the back, going heavier than you can control.", "Redondear la espalda, usar más peso del que controlas.",
       pri="HA LB", sec="GL", compound=True)
group(("Hike the bell back between the legs, then snap the hips forward so it floats to chest height. Arms just guide it.",
       "Lleva la pesa hacia atrás entre las piernas y empuja la cadera al frente para que suba a la altura del pecho. Los brazos solo guían.",
       "Squatting instead of hinging, lifting with the arms.", "Hacer sentadilla en vez de bisagra, subirla con los brazos."),
      [("kb_swing", "Kettlebell swing", "Swing con pesa rusa", "KETTLEBELL", {"met": 8.0}),
       ("cable_pull_through", "Cable pull-through", "Pull-through en polea", "CABLE"),
       ("band_good_morning", "Good morning (band)", "Buenos días (liga)", "BAND")],
      pri="GL HA", sec="LB", compound=True)
group(("Pull toward your face with elbows high, hands finishing beside the ears. Squeeze the back of the shoulders.",
       "Jala hacia tu cara con los codos altos, terminando con las manos junto a las orejas. Aprieta la parte de atrás de los hombros.",
       "Pulling to the chest with elbows low, leaning back.", "Jalar al pecho con los codos abajo, echarte atrás."),
      [("face_pull", "Face pull (cable)", "Face pull (polea)", "CABLE"),
       ("band_face_pull", "Face pull (band)", "Face pull (liga)", "BAND"),
       ("band_pull_apart", "Band pull-apart", "Separación con liga", "BAND")],
      pri="RD", sec="UB TR")

# ═════════════════════════════ SHOULDERS ═════════════════════════════
group(("Brace the core and squeeze the glutes. Press from the shoulders to overhead, moving the head back then through as the weight passes.",
       "Aprieta abdomen y glúteos. Empuja desde los hombros hasta arriba, mueve la cabeza hacia atrás y luego adelante cuando pase el peso.",
       "Leaning back into a standing bench press, pressing in front of the face.",
       "Echarse hacia atrás como press de banca de pie, empujar por delante de la cara."),
      [("bb_ohp", "Overhead press (barbell)", "Press militar (barra)", "BARBELL"),
       ("db_shoulder_press_seated", "Shoulder press (seated dumbbell)", "Press de hombro sentado (mancuernas)", "DUMBBELL"),
       ("db_shoulder_press_standing", "Shoulder press (standing dumbbell)", "Press de hombro de pie (mancuernas)", "DUMBBELL"),
       ("machine_shoulder_press", "Shoulder press (machine)", "Press de hombro (máquina)", "MACHINE"),
       ("smith_shoulder_press", "Shoulder press (Smith)", "Press de hombro (Smith)", "SMITH"),
       ("band_overhead_press", "Overhead press (band)", "Press arriba (liga)", "BAND"),
       ("kb_press", "Overhead press (kettlebell)", "Press arriba (pesa rusa)", "KETTLEBELL"),
       ("z_press", "Z press (seated on floor)", "Press Z (sentado en el piso)", "BARBELL", {"sec": "TRI AB"}),
       ("push_press", "Push press", "Push press", "BARBELL", {"sec": "TRI QU"}),
       ("arnold_press", "Arnold press", "Press Arnold", "DUMBBELL")],
      pri="FD SD", sec="TRI", compound=True)
group(("Slight bend in the elbows, raise the weights out to the sides up to shoulder height, lead with the elbows, lower slowly.",
       "Con los codos un poco doblados, sube los pesos a los lados hasta la altura del hombro, guiando con los codos, y baja lento.",
       "Swinging the body, shrugging the traps, lifting above shoulder height with a heavy weight.",
       "Balancear el cuerpo, encoger el trapecio, subir por arriba del hombro con mucho peso."),
      [("db_lateral_raise", "Lateral raise (dumbbell)", "Elevaciones laterales (mancuernas)", "DUMBBELL"),
       ("cable_lateral_raise", "Lateral raise (cable)", "Elevaciones laterales (polea)", "CABLE"),
       ("machine_lateral_raise", "Lateral raise (machine)", "Elevaciones laterales (máquina)", "MACHINE"),
       ("band_lateral_raise", "Lateral raise (band)", "Elevaciones laterales (liga)", "BAND")],
      pri="SD", sec="TR")
group(("Raise the weight straight in front to shoulder height with soft elbows, then lower slowly.",
       "Sube el peso al frente hasta la altura del hombro con los codos suaves y baja lento.",
       "Using momentum, arching the back.", "Usar impulso, arquear la espalda."),
      [("db_front_raise", "Front raise (dumbbell)", "Elevaciones frontales (mancuernas)", "DUMBBELL"),
       ("plate_front_raise", "Front raise (plate)", "Elevaciones frontales (disco)", "PLATE"),
       ("cable_front_raise", "Front raise (cable)", "Elevaciones frontales (polea)", "CABLE")],
      pri="FD", sec="SD")
group(("Hinge forward (or face the machine). Open the arms out wide with soft elbows, squeezing the back of the shoulders.",
       "Inclínate al frente (o de frente a la máquina). Abre los brazos con los codos suaves apretando la parte trasera del hombro.",
       "Squeezing the shoulder blades hard so the upper back does the work.",
       "Juntar fuerte los omóplatos y que la espalda haga el trabajo."),
      [("db_rear_delt_fly", "Rear delt fly (dumbbell)", "Aperturas posteriores (mancuernas)", "DUMBBELL"),
       ("cable_rear_delt_fly", "Rear delt fly (cable)", "Aperturas posteriores (polea)", "CABLE"),
       ("reverse_pec_deck", "Reverse pec deck", "Pec deck inverso", "MACHINE")],
      pri="RD", sec="UB")
group(("Pull the weight up along the body to lower-chest height, elbows leading and slightly higher than the hands.",
       "Sube el peso pegado al cuerpo hasta la parte baja del pecho, con los codos guiando y un poco más altos que las manos.",
       "Pulling up to the chin with a narrow grip, which can pinch the shoulders.",
       "Subir hasta la barbilla con agarre cerrado, lo que puede lastimar los hombros."),
      [("bb_upright_row", "Upright row (barbell)", "Remo al mentón (barra)", "BARBELL"),
       ("cable_upright_row", "Upright row (cable)", "Remo al mentón (polea)", "CABLE"),
       ("db_upright_row", "Upright row (dumbbell)", "Remo al mentón (mancuernas)", "DUMBBELL")],
      pri="SD TR", sec="FD")
group(("Lie face-down on an incline bench, thumbs up. Raise the arms in a Y shape to ear height with light weights.",
       "Boca abajo en banca inclinada, pulgares arriba. Sube los brazos en forma de Y hasta las orejas con poco peso.",
       "Going too heavy and shrugging.", "Usar mucho peso y encoger los hombros."),
      [("incline_y_raise", "Incline Y-raise", "Elevación en Y inclinado", "DUMBBELL"),
       ("cuban_press", "Cuban press", "Press cubano", "DUMBBELL", {"pri": "RD SD"})],
      pri="TR RD", sec="SD")

# ═════════════════════════════ ARMS ═════════════════════════════
group(("Elbows pinned at your sides. Curl the weight up by bending only the elbows, squeeze, then lower all the way.",
       "Codos pegados a los costados. Sube el peso doblando solo los codos, aprieta y baja completo.",
       "Swinging the body, elbows drifting forward, half reps.",
       "Balancear el cuerpo, adelantar los codos, repeticiones a medias."),
      [("bb_curl", "Biceps curl (barbell)", "Curl de bíceps (barra)", "BARBELL"),
       ("db_curl", "Biceps curl (dumbbell)", "Curl de bíceps (mancuernas)", "DUMBBELL"),
       ("ez_curl", "Biceps curl (EZ bar)", "Curl de bíceps (barra Z)", "EZ_BAR"),
       ("cable_curl", "Biceps curl (cable)", "Curl de bíceps (polea)", "CABLE"),
       ("band_curl", "Biceps curl (band)", "Curl de bíceps (liga)", "BAND"),
       ("db_incline_curl", "Incline curl (dumbbell)", "Curl inclinado (mancuernas)", "DUMBBELL"),
       ("concentration_curl", "Concentration curl", "Curl concentrado", "DUMBBELL"),
       ("spider_curl", "Spider curl", "Curl araña", "DUMBBELL"),
       ("drag_curl", "Drag curl", "Curl arrastrado", "BARBELL"),
       ("bayesian_curl", "Behind-body cable curl", "Curl en polea por detrás del cuerpo", "CABLE"),
       ("zottman_curl", "Zottman curl", "Curl Zottman", "DUMBBELL", {"sec": "FA"})],
      pri="BI", sec="FA")
group(("Arm resting on the pad. Curl up, then lower slowly until the elbow is nearly straight.",
       "Brazo apoyado en el atril. Sube y baja lento hasta casi estirar el codo.",
       "Dropping fast at the bottom (hard on the elbow).", "Bajar rápido al final (forza el codo)."),
      [("ez_preacher_curl", "Preacher curl (EZ bar)", "Curl predicador (barra Z)", "EZ_BAR"),
       ("db_preacher_curl", "Preacher curl (dumbbell)", "Curl predicador (mancuerna)", "DUMBBELL"),
       ("machine_preacher_curl", "Preacher curl (machine)", "Curl predicador (máquina)", "MACHINE")],
      pri="BI")
group(("Palms facing each other (thumbs up). Curl without rotating the wrist.",
       "Palmas mirándose (pulgares arriba). Sube sin girar la muñeca.",
       "Swinging the weights up.", "Subir los pesos balanceándote."),
      [("hammer_curl", "Hammer curl (dumbbell)", "Curl martillo (mancuernas)", "DUMBBELL"),
       ("cable_hammer_curl", "Hammer curl (cable rope)", "Curl martillo (cuerda en polea)", "CABLE"),
       ("ez_reverse_curl", "Reverse curl (EZ bar)", "Curl inverso (barra Z)", "EZ_BAR", {"pri": "FA BI"})],
      pri="BI FA")
group(("Elbows tucked at your sides. Push the handle down until the arms are straight, then let it rise to about chest height.",
       "Codos pegados a los costados. Empuja la manija hacia abajo hasta estirar los brazos y deja que suba a la altura del pecho.",
       "Elbows moving forward and back, leaning over the weight.",
       "Mover los codos adelante y atrás, inclinarse sobre el peso."),
      [("pushdown_bar", "Triceps pushdown (bar)", "Extensión de tríceps en polea (barra)", "CABLE"),
       ("pushdown_rope", "Triceps pushdown (rope)", "Extensión de tríceps en polea (cuerda)", "CABLE"),
       ("pushdown_single", "Single-arm triceps pushdown", "Extensión de tríceps a una mano", "CABLE"),
       ("band_pushdown", "Triceps pushdown (band)", "Extensión de tríceps (liga)", "BAND")],
      pri="TRI")
group(("Hold the weight overhead with elbows pointing up. Lower behind the head by bending only the elbows, then extend.",
       "Sostén el peso arriba con los codos apuntando al techo. Bájalo detrás de la cabeza doblando solo los codos y estira.",
       "Elbows flaring wide, arching the low back.", "Abrir mucho los codos, arquear la espalda baja."),
      [("db_overhead_extension", "Overhead triceps extension (dumbbell)", "Extensión de tríceps sobre la cabeza (mancuerna)", "DUMBBELL"),
       ("cable_overhead_extension", "Overhead triceps extension (cable)", "Extensión de tríceps sobre la cabeza (polea)", "CABLE"),
       ("ez_overhead_extension", "Overhead triceps extension (EZ bar)", "Extensión de tríceps sobre la cabeza (barra Z)", "EZ_BAR")],
      pri="TRI")
group(("Lying on a bench, lower the weight toward the forehead (or just behind it) by bending the elbows, then extend.",
       "Acostado en la banca, baja el peso hacia la frente (o un poco atrás) doblando los codos y estira.",
       "Letting the elbows flare, moving the upper arms.", "Abrir los codos, mover la parte alta del brazo."),
      [("ez_skull_crusher", "Skull crusher (EZ bar)", "Rompecráneos (barra Z)", "EZ_BAR"),
       ("db_skull_crusher", "Skull crusher (dumbbell)", "Rompecráneos (mancuernas)", "DUMBBELL"),
       ("db_tate_press", "Tate press", "Press Tate", "DUMBBELL"),
       ("jm_press", "JM press", "Press JM", "BARBELL")],
      pri="TRI")
single("close_grip_bench", "Close-grip bench press", "Press de banca agarre cerrado", "BARBELL",
       "Hands about shoulder width. Lower to the lower chest with elbows close to the body, then press.",
       "Manos al ancho de hombros. Baja a la parte baja del pecho con los codos pegados y empuja.",
       "Grip so narrow the wrists bend.", "Agarre tan cerrado que se doblan las muñecas.",
       pri="TRI CH", sec="FD", compound=True)
group(("Hinge forward with the upper arm parallel to the floor. Straighten the elbow back, pause, return.",
       "Inclínate con la parte alta del brazo paralela al piso. Estira el codo hacia atrás, pausa y regresa.",
       "Swinging the weight, dropping the elbow.", "Balancear el peso, dejar caer el codo."),
      [("db_kickback", "Triceps kickback (dumbbell)", "Patada de tríceps (mancuerna)", "DUMBBELL"),
       ("cable_kickback_triceps", "Triceps kickback (cable)", "Patada de tríceps (polea)", "CABLE")],
      pri="TRI")
group(("Forearms on your thighs or a bench. Move only the wrists through the full range.",
       "Antebrazos sobre los muslos o una banca. Mueve solo las muñecas en todo el rango.",
       "Using too much weight and bouncing.", "Usar mucho peso y rebotar."),
      [("wrist_curl", "Wrist curl", "Curl de muñeca", "DUMBBELL"),
       ("reverse_wrist_curl", "Reverse wrist curl", "Curl de muñeca inverso", "DUMBBELL")],
      pri="FA")
single("farmers_carry", "Farmer's carry", "Caminata del granjero", "DUMBBELL",
       "Hold heavy weights at your sides, stand tall and walk with short steady steps. Log the seconds.",
       "Sostén pesos pesados a los lados, párate derecho y camina con pasos cortos y firmes. Registra los segundos.",
       "Leaning to one side, shrugging.", "Inclinarte hacia un lado, encoger los hombros.",
       pri="FA TR", sec="AB GL", mode="HOLD", compound=True, met=6.0)

# ═════════════════════════════ LEGS ═════════════════════════════
group(("Feet about shoulder width, brace. Sit down and back between the hips until thighs are at least parallel, knees following the toes. Drive up.",
       "Pies al ancho de hombros, aprieta el abdomen. Baja entre la cadera hasta que los muslos queden al menos paralelos, rodillas en dirección de los pies. Sube con fuerza.",
       "Knees caving in, heels lifting, rounding the back at the bottom.",
       "Rodillas hacia adentro, talones despegados, redondear la espalda abajo."),
      [("bb_back_squat", "Back squat (barbell)", "Sentadilla trasera (barra)", "BARBELL"),
       ("bb_front_squat", "Front squat (barbell)", "Sentadilla frontal (barra)", "BARBELL", {"sec": "GL AB"}),
       ("smith_squat", "Squat (Smith)", "Sentadilla (Smith)", "SMITH"),
       ("pause_squat", "Pause squat", "Sentadilla con pausa", "BARBELL"),
       ("box_squat", "Box squat", "Sentadilla a cajón", "BARBELL"),
       ("zercher_squat", "Zercher squat", "Sentadilla Zercher", "BARBELL", {"sec": "GL AB UB"}),
       ("db_goblet_squat", "Goblet squat (dumbbell)", "Sentadilla goblet (mancuerna)", "DUMBBELL"),
       ("kb_goblet_squat", "Goblet squat (kettlebell)", "Sentadilla goblet (pesa rusa)", "KETTLEBELL"),
       ("kb_front_squat", "Front squat (kettlebell)", "Sentadilla frontal (pesa rusa)", "KETTLEBELL"),
       ("band_squat", "Squat (band)", "Sentadilla (liga)", "BAND"),
       ("belt_squat", "Belt squat (machine)", "Sentadilla con cinturón (máquina)", "MACHINE"),
       ("hack_squat", "Hack squat (machine)", "Sentadilla hack (máquina)", "MACHINE"),
       ("pendulum_squat", "Pendulum squat (machine)", "Sentadilla péndulo (máquina)", "MACHINE"),
       ("v_squat", "V-squat (machine)", "Sentadilla en V (máquina)", "MACHINE")],
      pri="QU GL", sec="AD LB", compound=True)
group(("Back flat on the pad, feet mid-platform. Lower until knees are near 90° without the low back lifting, then press.",
       "Espalda pegada al respaldo, pies a la mitad de la plataforma. Baja hasta que las rodillas estén cerca de 90° sin despegar la espalda baja y empuja.",
       "Locking the knees hard at the top, going so deep the hips roll up.",
       "Trabar las rodillas arriba, bajar tanto que la cadera se despega."),
      [("leg_press", "Leg press", "Prensa de piernas", "MACHINE"),
       ("leg_press_single", "Single-leg leg press", "Prensa a una pierna", "MACHINE")],
      pri="QU GL", sec="AD", compound=True)
group(("Step out long enough that both knees reach about 90°. Front heel down, torso tall, push back up.",
       "Da un paso largo para que ambas rodillas lleguen a unos 90°. Talón delantero apoyado, torso derecho, empuja para subir.",
       "Front knee caving in, short steps that lift the heel.",
       "Rodilla delantera hacia adentro, pasos cortos que levantan el talón."),
      [("db_lunge_walking", "Walking lunge (dumbbell)", "Zancadas caminando (mancuernas)", "DUMBBELL"),
       ("bb_lunge", "Lunge (barbell)", "Zancada (barra)", "BARBELL"),
       ("db_reverse_lunge", "Reverse lunge (dumbbell)", "Zancada hacia atrás (mancuernas)", "DUMBBELL"),
       ("lateral_lunge", "Lateral lunge", "Zancada lateral", "DUMBBELL", {"sec": "AD HA"}),
       ("db_step_up", "Step-up (dumbbell)", "Subida a cajón (mancuernas)", "DUMBBELL")],
      pri="QU GL", sec="HA AD", compound=True)
group(("Back foot on a bench behind you. Lower straight down until the front thigh is near parallel, then drive up through the front foot.",
       "Pie trasero sobre una banca. Baja recto hasta que el muslo delantero quede casi paralelo y sube empujando con el pie delantero.",
       "Front foot too close (heel lifts), leaning on the back leg.",
       "Pie delantero muy cerca (se levanta el talón), cargar en la pierna de atrás."),
      [("db_bulgarian", "Bulgarian split squat (dumbbell)", "Sentadilla búlgara (mancuernas)", "DUMBBELL"),
       ("bb_bulgarian", "Bulgarian split squat (barbell)", "Sentadilla búlgara (barra)", "BARBELL"),
       ("smith_bulgarian", "Bulgarian split squat (Smith)", "Sentadilla búlgara (Smith)", "SMITH")],
      pri="QU GL", sec="AD HA", compound=True)
single("leg_extension", "Leg extension", "Extensión de piernas", "MACHINE",
       "Knee lined up with the machine's pivot. Straighten the legs, squeeze the quads, lower slowly.",
       "Rodilla alineada con el eje de la máquina. Estira las piernas, aprieta los cuádriceps y baja lento.",
       "Kicking the weight up, hips lifting off the seat.", "Patear el peso, despegar la cadera del asiento.",
       pri="QU")
group(("Knee lined up with the pivot. Curl the heels toward the glutes, pause, lower slowly.",
       "Rodilla alineada con el eje. Lleva los talones hacia los glúteos, pausa y baja lento.",
       "Hips lifting, swinging the weight.", "Levantar la cadera, balancear el peso."),
      [("leg_curl_lying", "Leg curl (lying)", "Curl femoral acostado", "MACHINE"),
       ("leg_curl_seated", "Leg curl (seated)", "Curl femoral sentado", "MACHINE"),
       ("leg_curl_standing", "Leg curl (standing)", "Curl femoral de pie", "MACHINE")],
      pri="HA", sec="CA")
group(("Soft knees, push the hips back with a flat back, weight sliding down the legs to mid-shin, then squeeze the glutes to stand.",
       "Rodillas suaves, lleva la cadera atrás con la espalda recta y deja que el peso baje por las piernas hasta media espinilla; aprieta glúteos para subir.",
       "Rounding the back to reach lower, turning it into a squat.",
       "Redondear la espalda para bajar más, convertirlo en sentadilla."),
      [("bb_rdl", "Romanian deadlift (barbell)", "Peso muerto rumano (barra)", "BARBELL"),
       ("db_rdl", "Romanian deadlift (dumbbell)", "Peso muerto rumano (mancuernas)", "DUMBBELL"),
       ("db_rdl_single", "Single-leg Romanian deadlift", "Peso muerto rumano a una pierna", "DUMBBELL"),
       ("stiff_leg_deadlift", "Stiff-leg deadlift", "Peso muerto piernas rígidas", "BARBELL")],
      pri="HA GL", sec="LB", compound=True)
group(("Upper back on a bench, bar over the hips (use a pad). Drive the hips up until the body is flat from knees to shoulders, chin tucked.",
       "Parte alta de la espalda en la banca, barra sobre la cadera (usa protector). Sube la cadera hasta quedar recto de rodillas a hombros, barbilla abajo.",
       "Overarching the low back at the top instead of squeezing the glutes.",
       "Arquear la espalda baja arriba en lugar de apretar glúteos."),
      [("bb_hip_thrust", "Hip thrust (barbell)", "Hip thrust (barra)", "BARBELL"),
       ("machine_hip_thrust", "Hip thrust (machine)", "Hip thrust (máquina)", "MACHINE"),
       ("bb_glute_bridge", "Glute bridge (barbell)", "Puente de glúteo (barra)", "BARBELL"),
       ("db_hip_thrust", "Hip thrust (dumbbell)", "Hip thrust (mancuerna)", "DUMBBELL"),
       ("smith_hip_thrust", "Hip thrust (Smith)", "Hip thrust (Smith)", "SMITH")],
      pri="GL", sec="HA", compound=True)
group(("Lean slightly forward holding the machine. Kick the leg back by squeezing the glute, without arching the back.",
       "Inclínate un poco sosteniendo la máquina. Lleva la pierna atrás apretando el glúteo, sin arquear la espalda.",
       "Swinging the leg using the low back.", "Balancear la pierna usando la espalda baja."),
      [("cable_glute_kickback", "Glute kickback (cable)", "Patada de glúteo (polea)", "CABLE")],
      pri="GL")
group(("Sit in the machine, knees against the pads, and press out (abduction) or in (adduction) slowly.",
       "Siéntate en la máquina con las rodillas en los soportes y empuja hacia afuera (abducción) o hacia adentro (aducción) lento.",
       "Letting the weight snap back.", "Dejar que el peso regrese de golpe."),
      [("hip_abduction", "Hip abduction (machine)", "Abducción de cadera (máquina)", "MACHINE", {"pri": "GL"}),
       ("hip_adduction", "Hip adduction (machine)", "Aducción de cadera (máquina)", "MACHINE", {"pri": "AD"})],
      pri="GL")
group(("Balls of the feet on the edge, rise as high as possible, pause, then lower until the heels drop below the step.",
       "Puntas de los pies en la orilla, sube lo más alto posible, pausa y baja hasta que los talones queden abajo del escalón.",
       "Bouncing fast with a tiny range.", "Rebotar rápido en un rango muy corto."),
      [("calf_raise_standing", "Calf raise (standing machine)", "Elevación de talones de pie (máquina)", "MACHINE"),
       ("calf_raise_seated", "Calf raise (seated)", "Elevación de talones sentado", "MACHINE"),
       ("calf_raise_leg_press", "Calf raise (leg press)", "Elevación de talones en prensa", "MACHINE"),
       ("calf_raise_single_db", "Single-leg calf raise (dumbbell)", "Elevación de talón a una pierna (mancuerna)", "DUMBBELL"),
       ("calf_raise_smith", "Calf raise (Smith)", "Elevación de talones (Smith)", "SMITH")],
      pri="CA")
single("sissy_squat", "Sissy squat", "Sentadilla sissy", "BODYWEIGHT",
       "Hold something for balance, rise on the toes and lean back as the knees travel forward. Keep hips straight.",
       "Sostente de algo, sube en puntas y échate atrás mientras las rodillas van al frente. Cadera recta.",
       "Bending at the hips, going too deep too soon.", "Doblar la cadera, bajar demasiado muy pronto.",
       type_="CALISTHENICS", pri="QU", mode="BODYWEIGHT_REPS", bw=0.7)
single("nordic_curl", "Nordic curl", "Curl nórdico", "BODYWEIGHT",
       "Kneel with ankles held. Lower the body forward as slowly as possible with straight hips, catch with the hands, push back up.",
       "De rodillas con los tobillos sujetos. Baja el cuerpo al frente lo más lento posible con la cadera recta, recíbete con las manos y empuja para subir.",
       "Bending at the hips to make it easier.", "Doblar la cadera para hacerlo más fácil.",
       type_="CALISTHENICS", pri="HA", sec="GL CA", mode="BODYWEIGHT_REPS", bw=0.0)

# ═════════════════════════════ KETTLEBELL / OLYMPIC ═════════════════════════════
group(("Start from the hang or floor, extend hips and knees explosively, pull yourself under and catch in a front rack.",
       "Desde colgado o desde el piso, extiende cadera y rodillas con explosión, métete bajo el peso y recíbelo en posición de rack frontal.",
       "Pulling early with the arms, catching with a rounded back.",
       "Jalar con los brazos antes de tiempo, recibir con la espalda redondeada."),
      [("power_clean", "Power clean", "Cargada de potencia", "BARBELL"),
       ("hang_clean", "Hang clean", "Cargada colgante", "BARBELL"),
       ("kb_clean", "Clean (kettlebell)", "Cargada (pesa rusa)", "KETTLEBELL")],
      pri="GL HA TR", sec="QU UB", compound=True, met=6.0)
single("kb_snatch", "Snatch (kettlebell)", "Arranque (pesa rusa)", "KETTLEBELL",
       "Swing the bell back, drive the hips and let it travel up close to the body, punch through to lock out overhead.",
       "Lleva la pesa atrás, empuja la cadera y deja que suba pegada al cuerpo; mete la mano y bloquea arriba.",
       "Letting the bell flop onto the wrist.", "Dejar que la pesa golpee la muñeca.",
       pri="GL SD", sec="HA TR", compound=True, met=8.0)
single("turkish_getup", "Turkish get-up", "Levantamiento turco", "KETTLEBELL",
       "From lying, keep the bell locked overhead and stand up in steps (elbow, hand, bridge, knee, stand), then reverse.",
       "Desde acostado, mantén la pesa arriba con el brazo recto y levántate por pasos (codo, mano, puente, rodilla, de pie); luego regresa.",
       "Rushing, losing eyes on the bell.", "Hacerlo con prisa, dejar de mirar la pesa.",
       pri="SD AB", sec="GL OB", compound=True)
single("kb_windmill", "Windmill (kettlebell)", "Molino (pesa rusa)", "KETTLEBELL",
       "Bell locked overhead, feet angled. Push the hip out and lower the free hand toward the floor, eyes on the bell.",
       "Pesa arriba con brazo recto, pies girados. Saca la cadera y baja la mano libre al piso mirando la pesa.",
       "Bending the top arm, twisting the lower back.", "Doblar el brazo de arriba, torcer la espalda baja.",
       pri="OB", sec="SD HA")

# ═════════════════════════════ CORE ═════════════════════════════
group(("Lift the shoulders by curling the ribs toward the hips, exhale at the top, lower slowly. Don't pull the neck.",
       "Sube los hombros acercando las costillas a la cadera, exhala arriba y baja lento. No jales el cuello.",
       "Yanking the head with the hands, sitting all the way up with the hip flexors.",
       "Jalar la cabeza con las manos, subir completo usando la cadera."),
      [("crunch", "Crunch", "Abdominal crunch", "BODYWEIGHT", {"type": "CALISTHENICS", "mode": "BODYWEIGHT_REPS"}),
       ("cable_crunch", "Cable crunch", "Crunch en polea", "CABLE"),
       ("machine_crunch", "Ab crunch (machine)", "Crunch (máquina)", "MACHINE"),
       ("decline_situp", "Decline sit-up", "Abdominal declinado", "BODYWEIGHT", {"type": "CALISTHENICS", "mode": "BODYWEIGHT_REPS"}),
       ("bicycle_crunch", "Bicycle crunch", "Crunch bicicleta", "BODYWEIGHT", {"type": "CALISTHENICS", "mode": "BODYWEIGHT_REPS", "pri": "AB OB"}),
       ("reverse_crunch", "Reverse crunch", "Crunch inverso", "BODYWEIGHT", {"type": "CALISTHENICS", "mode": "BODYWEIGHT_REPS"}),
       ("v_up", "V-up", "V-up", "BODYWEIGHT", {"type": "CALISTHENICS", "mode": "BODYWEIGHT_REPS"})],
      pri="AB")
group(("Hang from a bar, shoulders active. Raise the knees or legs by curling the pelvis up, lower without swinging.",
       "Cuelga de la barra con los hombros activos. Sube rodillas o piernas enrollando la pelvis y baja sin balancearte.",
       "Swinging, only lifting the legs without curling the pelvis.",
       "Balancearte, subir las piernas sin enrollar la pelvis."),
      [("hanging_knee_raise", "Hanging knee raise", "Elevación de rodillas colgado", "BODYWEIGHT"),
       ("hanging_leg_raise", "Hanging leg raise", "Elevación de piernas colgado", "BODYWEIGHT"),
       ("toes_to_bar", "Toes to bar", "Pies a la barra", "BODYWEIGHT", {"sec": "LA FA"}),
       ("lying_leg_raise", "Lying leg raise", "Elevación de piernas acostado", "BODYWEIGHT")],
      type_="CALISTHENICS", pri="AB", sec="FA", mode="BODYWEIGHT_REPS")
group(("Forearms under shoulders, body straight from head to heels. Squeeze glutes and abs and breathe. Log the seconds.",
       "Antebrazos bajo los hombros, cuerpo recto de cabeza a talones. Aprieta glúteos y abdomen y respira. Registra los segundos.",
       "Hips sagging or sticking up, holding the breath.", "Cadera caída o muy arriba, aguantar la respiración."),
      [("plank", "Plank", "Plancha", "BODYWEIGHT"),
       ("side_plank", "Side plank", "Plancha lateral", "BODYWEIGHT", {"pri": "OB"}),
       ("copenhagen_plank", "Copenhagen plank", "Plancha Copenhague", "BODYWEIGHT", {"pri": "AD OB"}),
       ("hollow_hold", "Hollow hold", "Hollow (posición de banana)", "BODYWEIGHT",
        {"howEn": "Lie on your back, press the low back into the floor, lift arms and legs a few inches. Hold.",
         "howEs": "Boca arriba, pega la espalda baja al piso y sube brazos y piernas unos centímetros. Mantén."}),
       ("dead_bug", "Dead bug", "Dead bug (bicho muerto)", "BODYWEIGHT",
        {"howEn": "On your back, arms up, knees at 90°. Slowly extend opposite arm and leg while the low back stays down. Log seconds.",
         "howEs": "Boca arriba, brazos arriba, rodillas a 90°. Estira lento brazo y pierna contrarios sin despegar la espalda baja. Registra segundos."}),
       ("bird_dog", "Bird dog", "Bird dog (perro de caza)", "BODYWEIGHT",
        {"howEn": "On hands and knees, reach one arm and the opposite leg long without rotating the hips. Alternate. Log seconds.",
         "howEs": "En cuatro puntos, estira un brazo y la pierna contraria sin girar la cadera. Alterna. Registra segundos.",
         "pri": "LB GL"})],
      type_="CALISTHENICS", pri="AB", sec="OB", mode="HOLD")
group(("Twist or resist twisting with control, moving from the trunk while the hips stay steady.",
       "Gira o resiste el giro con control, moviendo el tronco mientras la cadera se queda firme.",
       "Moving only the arms, rushing.", "Mover solo los brazos, hacerlo rápido."),
      [("russian_twist", "Russian twist", "Giro ruso", "PLATE", {
         "howEn": "Sit leaning back, feet down or up, holding a plate. Rotate the chest to touch the plate beside each hip.",
         "howEs": "Sentado inclinado atrás, pies abajo o arriba, con un disco. Gira el pecho para tocar con el disco junto a cada cadera."}),
       ("pallof_press", "Pallof press", "Press Pallof", "CABLE", {
         "howEn": "Stand sideways to the cable, handle at the chest. Press straight out and hold 2 s without letting the cable turn you.",
         "howEs": "De lado a la polea, manija en el pecho. Empuja al frente y sostén 2 s sin dejar que la polea te gire."}),
       ("band_pallof", "Pallof press (band)", "Press Pallof (liga)", "BAND", {
         "howEn": "Stand sideways to the band anchor, hands at the chest. Press straight out and hold 2 s without rotating.",
         "howEs": "De lado al anclaje de la liga, manos en el pecho. Empuja al frente y sostén 2 s sin girar."}),
       ("cable_woodchop", "Cable woodchop", "Leñador en polea", "CABLE", {
         "howEn": "Cable high on one side. Pull diagonally down across the body to the opposite hip, turning from the trunk.",
         "howEs": "Polea alta a un lado. Jala en diagonal hacia abajo cruzando el cuerpo hasta la cadera contraria, girando desde el tronco."}),
       ("torso_rotation_machine", "Torso rotation (machine)", "Rotación de torso (máquina)", "MACHINE")],
      pri="OB", sec="AB")
single("ab_wheel", "Ab wheel rollout", "Rueda abdominal", "OTHER",
       "From the knees, roll forward with a braced, slightly rounded trunk as far as you can control, then pull back.",
       "Desde las rodillas, rueda al frente con el abdomen firme y la espalda un poco redonda hasta donde controles; regresa.",
       "Low back sagging into an arch.", "Que la espalda baja se hunda y se arquee.",
       type_="CALISTHENICS", pri="AB", sec="LA", mode="BODYWEIGHT_REPS")
group(("Keep a strong plank while moving the legs or body as described, steady breathing.",
       "Mantén una plancha firme mientras mueves piernas o cuerpo, respirando constante.",
       "Hips bouncing up and down.", "Cadera rebotando arriba y abajo."),
      [("mountain_climber", "Mountain climber", "Escaladores", "BODYWEIGHT", {"met": 8.0,
         "howEn": "From a high plank, drive one knee toward the chest, then switch legs quickly, hips level. Count each leg as a rep.",
         "howEs": "Desde plancha alta, lleva una rodilla al pecho y cambia de pierna rápido, con la cadera nivelada. Cada pierna cuenta como una rep."}),
       ("flutter_kicks", "Flutter kicks", "Patadas de tijera", "BODYWEIGHT", {
         "howEn": "Lie on your back, low back pressed down, legs straight a few inches up. Kick up and down in small fast moves.",
         "howEs": "Boca arriba, espalda baja pegada al piso, piernas rectas unos centímetros arriba. Patea arriba y abajo con movimientos cortos y rápidos.",
         "misEn": "Low back lifting off the floor.", "misEs": "Despegar la espalda baja del piso."}),
       ("heel_taps", "Heel taps", "Toques de talón", "BODYWEIGHT", {"pri": "OB",
         "howEn": "On your back, knees bent, shoulders slightly lifted. Reach side to side to touch each heel.",
         "howEs": "Boca arriba, rodillas dobladas, hombros un poco arriba. Estírate de lado a lado para tocar cada talón.",
         "misEn": "Pulling on the neck, letting the shoulders drop between reps.", "misEs": "Jalar el cuello, bajar los hombros entre repeticiones."}),
       ("side_plank_dip", "Side plank hip dip", "Plancha lateral con bajada de cadera", "BODYWEIGHT", {"pri": "OB",
         "howEn": "In a side plank on the forearm, lower the hip toward the floor, then lift it above straight. Do both sides.",
         "howEs": "En plancha lateral sobre el antebrazo, baja la cadera hacia el piso y súbela un poco más arriba de recto. Haz ambos lados."}),
       ("stir_the_pot", "Stir the pot (ball)", "Mover la olla (pelota)", "OTHER", {
         "howEn": "Plank with forearms on an exercise ball. Draw small circles with the forearms while the body stays still.",
         "howEs": "Plancha con los antebrazos sobre una pelota de ejercicio. Haz círculos pequeños con los antebrazos sin mover el cuerpo."})],
      type_="CALISTHENICS", pri="AB", sec="OB", mode="BODYWEIGHT_REPS")
single("dragon_flag", "Dragon flag", "Bandera del dragón", "BODYWEIGHT",
       "Lying on a bench, hold behind your head and lower a straight body from the shoulders as slowly as you can.",
       "Acostado en la banca, sujétate detrás de la cabeza y baja el cuerpo recto desde los hombros lo más lento posible.",
       "Bending at the hips.", "Doblar la cadera.",
       type_="CALISTHENICS", pri="AB", sec="LA", mode="BODYWEIGHT_REPS")

# ═════════════════════════════ CALISTHENICS LADDERS ═════════════════════════════
# bwPct for push-ups — Ebben et al. 2011 (ISBS proceedings, "Kinetic analysis of several
# variations of push-ups"), peak force ÷ body mass: regular 0.64, bent knee 0.49,
# feet elevated 12 in 0.70 (24 in 0.74), hands elevated 12 in 0.55 (24 in 0.41).
# Checked twice against the paper's Table 1 (Nov 2026). Wall, archer, pseudo-planche
# and one-arm values are ESTIMATES (not in the study).
group(("Hands a bit wider than shoulders, body straight. Lower until the chest is about a fist from the surface, then push away.",
       "Manos un poco más abiertas que los hombros, cuerpo recto. Baja hasta que el pecho quede a un puño de la superficie y empuja.",
       "Hips sagging, flaring the elbows straight out, half reps.",
       "Cadera caída, codos totalmente abiertos, repeticiones a medias."),
      [("pu_wall", "Wall push-up", "Lagartija en la pared", "BODYWEIGHT", {"bw": 0.30}),
       ("pu_incline", "Incline push-up", "Lagartija inclinada", "BODYWEIGHT", {"bw": 0.55}),
       ("pu_knee", "Knee push-up", "Lagartija con rodillas", "BODYWEIGHT", {"bw": 0.49}),
       ("pu_standard", "Push-up", "Lagartija", "BODYWEIGHT", {"bw": 0.64}),
       ("pu_wide", "Wide push-up", "Lagartija abierta", "BODYWEIGHT", {"bw": 0.64}),
       ("pu_diamond", "Diamond push-up", "Lagartija diamante", "BODYWEIGHT", {"bw": 0.64, "pri": "TRI CH"}),
       ("pu_decline", "Decline push-up", "Lagartija declinada", "BODYWEIGHT", {"bw": 0.70, "pri": "CH FD"}),
       ("pu_archer", "Archer push-up", "Lagartija arquero", "BODYWEIGHT", {"bw": 0.64}),
       ("pu_pseudo_planche", "Pseudo-planche push-up", "Lagartija pseudo-plancha", "BODYWEIGHT", {"bw": 0.75, "pri": "FD CH"}),
       ("pu_one_arm", "One-arm push-up", "Lagartija a un brazo", "BODYWEIGHT", {"bw": 0.64}),
       ("burpee", "Burpee", "Burpee", "BODYWEIGHT", {"bw": 0.0, "met": 8.0, "pri": "QU CH",
         "howEn": "Squat, kick the feet back to a plank, do a push-up, jump the feet in and jump up.",
         "howEs": "Agáchate, lleva los pies atrás a plancha, haz una lagartija, regresa los pies y salta."})],
      type_="CALISTHENICS", pri="CH", sec="TRI FD AB", mode="BODYWEIGHT_REPS", compound=True)
group(("Hang with hands just wider than shoulders. Pull the chest toward the bar by driving elbows down, lower to full hang with control.",
       "Cuelga con las manos un poco más abiertas que los hombros. Lleva el pecho a la barra bajando los codos y baja hasta colgar con control.",
       "Kipping/swinging, half reps that stop short of a full hang.",
       "Balancearse, repeticiones a medias sin colgar completo."),
      [("pl_dead_hang", "Dead hang", "Colgarse (dead hang)", "BODYWEIGHT", {"mode": "HOLD", "bw": 0.0,
         "howEn": "Hang from the bar with straight arms, shoulders active, breathe. Log the seconds.",
         "howEs": "Cuelga de la barra con brazos rectos y hombros activos, respira. Registra los segundos.", "pri": "FA LA"}),
       ("pl_scap_pull", "Scapular pull-up", "Dominada escapular", "BODYWEIGHT", {"bw": 0.3,
         "howEn": "From a dead hang, pull the shoulder blades down without bending the elbows, then relax.",
         "howEs": "Colgado, baja los omóplatos sin doblar los codos y relaja."}),
       ("pl_negative", "Negative pull-up", "Dominada negativa", "BODYWEIGHT", {"bw": 1.0,
         "howEn": "Jump or step to the top, then lower as slowly as you can (3–5 s) to a full hang.",
         "howEs": "Salta o súbete a la posición de arriba y baja lo más lento posible (3–5 s) hasta colgar."}),
       ("pl_band", "Band-assisted pull-up", "Dominada asistida con liga", "BAND", {"bw": 1.0}),
       ("pl_pullup", "Pull-up", "Dominada", "BODYWEIGHT", {"bw": 1.0}),
       ("pl_chinup", "Chin-up", "Dominada supina", "BODYWEIGHT", {"bw": 1.0, "pri": "LA BI"}),
       ("pl_weighted", "Weighted pull-up", "Dominada con peso", "OTHER", {"bw": 1.0}),
       ("pl_lsit_pullup", "L-sit pull-up", "Dominada en L", "BODYWEIGHT", {"bw": 1.0, "sec": "AB BI"}),
       ("pl_archer", "Archer pull-up", "Dominada arquero", "BODYWEIGHT", {"bw": 1.0}),
       ("pl_one_arm_negative", "One-arm negative pull-up", "Dominada negativa a un brazo", "BODYWEIGHT", {"bw": 1.0}),
       ("pl_high_pullup", "High pull-up (chest to bar)", "Dominada alta (pecho a la barra)", "BODYWEIGHT", {"bw": 1.0}),
       ("muscle_up_jumping", "Jumping muscle-up", "Muscle-up con salto", "BODYWEIGHT", {"bw": 1.0, "sec": "TRI CH"}),
       ("muscle_up", "Muscle-up", "Muscle-up", "BODYWEIGHT", {"bw": 1.0, "sec": "TRI CH",
         "howEn": "Explosive pull to the lower chest, lean over the bar, then press to straight arms on top.",
         "howEs": "Jalón explosivo hasta el pecho bajo, pasa el cuerpo sobre la barra y empuja hasta estirar los brazos."}),
       ("inverted_row", "Inverted row", "Remo invertido", "BODYWEIGHT", {"bw": 0.6, "pri": "UB LA",
         "howEn": "Under a low bar, body straight, heels down. Pull the chest to the bar and lower slowly.",
         "howEs": "Debajo de una barra baja, cuerpo recto, talones apoyados. Lleva el pecho a la barra y baja lento."})],
      type_="CALISTHENICS", pri="LA", sec="BI UB FA", mode="BODYWEIGHT_REPS", compound=True)
group(("Feet about shoulder width. Sit down between the hips, chest up, knees following the toes, then stand up.",
       "Pies al ancho de hombros. Baja entre la cadera con el pecho arriba y las rodillas en dirección de los pies; sube.",
       "Knees caving in, heels lifting.", "Rodillas hacia adentro, talones levantados."),
      [("sq_assisted", "Assisted squat (hold support)", "Sentadilla asistida (con apoyo)", "BODYWEIGHT", {"bw": 0.5}),
       ("sq_bodyweight", "Bodyweight squat", "Sentadilla libre", "BODYWEIGHT", {"bw": 0.85}),
       ("jump_squat", "Jump squat", "Sentadilla con salto", "BODYWEIGHT", {"bw": 0.85, "met": 8.0}),
       ("sumo_squat_hold", "Wall sit", "Sentadilla en pared", "BODYWEIGHT", {"mode": "HOLD", "bw": 0.0,
         "howEn": "Back against a wall, slide down until thighs are parallel and hold. Log seconds.",
         "howEs": "Espalda en la pared, baja hasta que los muslos queden paralelos y mantén. Registra segundos."}),
       ("sq_bulgarian_bw", "Bulgarian split squat (bodyweight)", "Sentadilla búlgara (peso corporal)", "BODYWEIGHT", {"bw": 0.85}),
       ("sq_shrimp", "Shrimp squat", "Sentadilla camarón", "BODYWEIGHT", {"bw": 0.85,
         "howEn": "On one leg, hold the other foot behind you and lower until the back knee touches the floor.",
         "howEs": "En una pierna, sujeta el otro pie atrás y baja hasta tocar el piso con la rodilla trasera."}),
       ("sq_pistol", "Pistol squat", "Sentadilla pistol", "BODYWEIGHT", {"bw": 0.85,
         "howEn": "On one leg, the other straight in front, sit all the way down and stand up without touching.",
         "howEs": "En una pierna, la otra recta al frente, baja completo y sube sin apoyar."}),
       ("glute_bridge_bw", "Glute bridge (bodyweight)", "Puente de glúteo (peso corporal)", "BODYWEIGHT", {"bw": 0.5, "pri": "GL HA",
         "howEn": "On your back, knees bent, push through the heels to lift the hips until knees-hips-shoulders line up.",
         "howEs": "Boca arriba con rodillas dobladas, empuja con los talones y sube la cadera hasta alinear rodillas, cadera y hombros."}),
       ("single_leg_hip_thrust", "Single-leg hip thrust", "Hip thrust a una pierna", "BODYWEIGHT", {"bw": 0.6, "pri": "GL"})],
      type_="CALISTHENICS", pri="QU GL", sec="HA AD", mode="BODYWEIGHT_REPS", compound=True)
group(("Sit with legs straight, hands beside the hips (floor or parallettes). Push down to lift the hips and legs and hold. Log seconds.",
       "Sentado con piernas rectas y manos junto a la cadera (piso o paralelas). Empuja para levantar cadera y piernas y mantén. Registra segundos.",
       "Bending the arms, shrugging.", "Doblar los brazos, encoger los hombros."),
      [("tuck_lsit", "Tuck L-sit", "L-sit con rodillas recogidas", "BODYWEIGHT"),
       ("lsit", "L-sit", "L-sit", "BODYWEIGHT"),
       ("tuck_front_lever", "Tuck front lever", "Front lever recogido", "BODYWEIGHT", {"pri": "LA AB"}),
       ("tuck_planche_lean", "Planche lean", "Inclinación de plancha", "BODYWEIGHT", {"pri": "FD CH"})],
      type_="CALISTHENICS", pri="AB", sec="TRI FD", mode="HOLD")
group(("Hips high in an upside-down V. Bend the elbows to bring the head toward the floor in front of the hands, then push up.",
       "Cadera alta en forma de V invertida. Dobla los codos llevando la cabeza al piso frente a las manos y empuja.",
       "Elbows flaring, head going straight down between the hands.",
       "Codos abiertos, la cabeza baja justo entre las manos."),
      [("pike_pushup", "Pike push-up", "Lagartija en pica", "BODYWEIGHT", {"bw": 0.6}),
       ("pike_pushup_elevated", "Elevated pike push-up", "Lagartija en pica elevada", "BODYWEIGHT", {"bw": 0.75}),
       ("wall_handstand_hold", "Wall handstand hold", "Parada de manos en la pared", "BODYWEIGHT", {"mode": "HOLD", "bw": 0.0,
         "howEn": "Kick up or walk up the wall, arms locked, body straight, ribs in. Log seconds.",
         "howEs": "Sube a la pared con impulso o caminando, brazos bloqueados, cuerpo recto, costillas adentro. Registra segundos."}),
       ("hspu_wall", "Wall handstand push-up", "Lagartija en parada de manos (pared)", "BODYWEIGHT", {"bw": 0.9})],
      type_="CALISTHENICS", pri="FD SD", sec="TRI", mode="BODYWEIGHT_REPS", compound=True)
group(("Support yourself with straight arms. Lower until the shoulders are about elbow height, chest slightly forward, then press up.",
       "Apóyate con brazos rectos. Baja hasta que los hombros queden a la altura de los codos, pecho un poco al frente, y empuja.",
       "Going too deep with the shoulders rolling forward, shrugging.",
       "Bajar demasiado con los hombros hacia adelante, encogerse."),
      [("dip_bench", "Bench dip", "Fondos en banca", "BODYWEIGHT", {"bw": 0.5}),
       ("dip_parallel", "Parallel bar dip", "Fondos en paralelas", "BODYWEIGHT", {"bw": 1.0}),
       ("dip_weighted", "Weighted dip", "Fondos con peso", "OTHER", {"bw": 1.0}),
       ("dip_ring", "Ring dip", "Fondos en anillas", "OTHER", {"bw": 1.0}),
       ("dip_straight_bar", "Straight bar dip", "Fondos en barra recta", "BODYWEIGHT", {"bw": 1.0}),
       ("assisted_dip_machine", "Assisted dip (machine)", "Fondos asistidos (máquina)", "MACHINE", {"bw": 1.0})],
      type_="CALISTHENICS", pri="TRI CH", sec="FD", mode="BODYWEIGHT_REPS", compound=True)

# ═════════════════════════════ CARDIO ═════════════════════════════
# MET values: 2011 Compendium of Physical Activities (Ainsworth et al.) — same edition as
# Gym & Ñam. Checked twice against the 2011 PDF (Nov 2026): run 6 mph 9.8, walk 3 mph 3.5,
# uphill walk 5.3, hike 6.0, stationary bike 7.0, bicycling 7.5, indoor cycling class 8.5 (Compendium code 02019), rowing
# 100 W 7.0, elliptical 5.0, stair-treadmill 9.0, rope jumping moderate 11.8, circuit 8.0.
# Swimming 8.0 = Gym & Ñam's value. ESTIMATES (no exact Compendium entry): air bike 8.0,
# battle ropes 8.0, sled push 8.0, sprints 9.8 (uses running 6 mph as a floor).
CARDIO_TEXT = ("Log time and distance. Keep a pace you can sustain; an easy pace is one where you can still talk.",
               "Registra tiempo y distancia. Mantén un ritmo sostenible; uno suave es en el que todavía puedes platicar.",
               "Starting too fast, skipping a warm-up.", "Empezar demasiado rápido, no calentar.")
group(CARDIO_TEXT, [
    ("treadmill_run", "Treadmill run", "Correr en caminadora", "MACHINE", {"met": 9.8}),
    ("outdoor_run", "Outdoor run", "Correr al aire libre", "NONE", {"met": 9.8}),
    ("walk", "Walk", "Caminar", "NONE", {"met": 3.5}),
    ("incline_walk", "Incline walk", "Caminata inclinada", "MACHINE", {"met": 5.3}),
    ("hike", "Hike", "Senderismo", "NONE", {"met": 6.0}),
    ("stationary_bike", "Stationary bike", "Bicicleta fija", "MACHINE", {"met": 7.0}),
    ("outdoor_ride", "Outdoor ride", "Bicicleta al aire libre", "OTHER", {"met": 7.5}),
    ("indoor_cycling_bike", "Indoor cycling bike (class pace)", "Bici de ciclismo indoor (ritmo de clase)", "MACHINE", {"met": 8.5}),
    ("rowing_machine", "Rowing machine", "Remo (máquina)", "MACHINE", {"met": 7.0, "pri": "UB QU", "sec": "LA HA BI"}),
    ("elliptical", "Elliptical", "Elíptica", "MACHINE", {"met": 5.0}),
    ("stair_climber", "Stair climber", "Escaladora", "MACHINE", {"met": 9.0, "pri": "QU GL"}),
    ("jump_rope", "Jump rope", "Saltar la cuerda", "OTHER", {"met": 11.8, "pri": "CA", "sec": "QU"}),
    ("swimming", "Swimming", "Natación", "NONE", {"met": 8.0, "pri": "LA", "sec": "SD TRI"}),
    ("hiit", "HIIT", "HIIT", "NONE", {"met": 8.0}),
    ("air_bike", "Air bike", "Bici de aire", "MACHINE", {"met": 8.0}),
    ("battle_ropes", "Battle ropes", "Cuerdas de batalla", "OTHER", {"met": 8.0, "pri": "SD FD", "sec": "AB"}),
    ("sled_push", "Sled push", "Empujar trineo", "OTHER", {"met": 8.0, "pri": "QU GL"}),
    ("sprints", "Sprints", "Sprints", "NONE", {"met": 9.8, "pri": "QU HA GL"}),
], type_="CARDIO", pri="QU HA", sec="CA GL", mode="CARDIO")

# ═════════════════════════════ STRETCHES ═════════════════════════════
def stretch(vid, en, es, pri, howEn, howEs):
    single(vid, en, es, "NONE", howEn, howEs,
           "Bouncing or pushing into pain. A stretch should feel like gentle tension.",
           "Rebotar o forzar hasta el dolor. Un estiramiento debe sentirse como tensión suave.",
           type_="STRETCH", pri=pri, mode="STRETCH")

stretch("st_hamstring_fold", "Standing forward fold", "Flexión de pie hacia adelante", "HA",
        "Feet hip width, soft knees. Hinge at the hips and let the chest hang toward the legs.",
        "Pies al ancho de cadera, rodillas suaves. Dóblate desde la cadera y deja colgar el pecho hacia las piernas.")
stretch("st_hamstring_seated", "Seated hamstring stretch", "Estiramiento de isquios sentado", "HA",
        "Sit with one leg straight. Reach toward the foot with a long back.",
        "Sentado con una pierna recta. Estírate hacia el pie con la espalda larga.")
stretch("st_hamstring_strap", "Lying hamstring stretch (strap)", "Isquios acostado (con correa)", "HA",
        "On your back, loop a strap or towel around one foot and raise the straight leg.",
        "Boca arriba, pasa una correa o toalla por un pie y sube la pierna recta.")
stretch("st_quad_standing", "Standing quad stretch", "Cuádriceps de pie", "QU",
        "Hold one ankle behind you, knees together, squeeze the glute.",
        "Sujeta un tobillo atrás, rodillas juntas, aprieta el glúteo.")
stretch("st_couch", "Couch stretch", "Estiramiento de sofá", "QU",
        "Back knee on the floor near a wall, shin up the wall, front foot forward. Tall chest.",
        "Rodilla trasera en el piso cerca de la pared, espinilla en la pared, pie delantero al frente. Pecho arriba.")
stretch("st_hip_flexor", "Kneeling hip flexor stretch", "Flexor de cadera de rodillas", "QU",
        "Half-kneel, tuck the pelvis and shift forward until the front of the back hip stretches.",
        "Con una rodilla en el piso, mete la pelvis y avanza hasta sentir el frente de la cadera trasera.")
stretch("st_pigeon", "Pigeon pose", "Postura de paloma", "GL",
        "Front shin across the body, back leg long. Sink the hips and fold forward if comfortable.",
        "Espinilla delantera cruzada, pierna trasera larga. Baja la cadera e inclínate al frente si es cómodo.")
stretch("st_figure4", "Figure-4 glute stretch", "Glúteo en figura 4", "GL",
        "On your back, ankle over the opposite knee, pull the bottom thigh toward you.",
        "Boca arriba, tobillo sobre la rodilla contraria, jala el muslo de abajo hacia ti.")
stretch("st_butterfly", "Butterfly stretch", "Mariposa", "AD",
        "Sit with soles together, knees out, gently lean forward.",
        "Sentado con las plantas juntas y rodillas afuera, inclínate suave al frente.")
stretch("st_frog", "Frog stretch", "Rana", "AD",
        "On hands and knees, slide the knees wide with feet in line, ease the hips back.",
        "En cuatro puntos, abre las rodillas con los pies alineados y lleva la cadera atrás.")
stretch("st_adductor_rock", "Adductor rock-back", "Balanceo de aductores", "AD",
        "Kneel with one leg out to the side, rock the hips back and forth slowly.",
        "De rodillas con una pierna estirada al lado, mueve la cadera atrás y adelante lento.")
stretch("st_9090", "90/90 hip stretch", "Cadera 90/90", "GL",
        "Sit with both knees at 90°, one in front, one to the side. Lean over the front shin.",
        "Sentado con ambas rodillas a 90°, una al frente y otra al lado. Inclínate sobre la espinilla delantera.")
stretch("st_calf_wall", "Wall calf stretch", "Pantorrilla en la pared", "CA",
        "Hands on a wall, one leg back straight with the heel down. Lean in.",
        "Manos en la pared, una pierna atrás recta con el talón abajo. Inclínate.")
stretch("st_ankle_knee_wall", "Knee-to-wall ankle mobility", "Movilidad de tobillo rodilla a pared", "CA",
        "Foot a few inches from the wall, push the knee toward the wall without lifting the heel.",
        "Pie a unos centímetros de la pared, lleva la rodilla a la pared sin levantar el talón.")
stretch("st_chest_doorway", "Doorway chest stretch", "Pecho en el marco de la puerta", "CH",
        "Forearm on the door frame at shoulder height, step through until the chest stretches.",
        "Antebrazo en el marco a la altura del hombro, da un paso al frente hasta sentir el pecho.")
stretch("st_cross_body_shoulder", "Cross-body shoulder stretch", "Hombro cruzado", "RD",
        "Pull one straight arm across the chest with the other hand.",
        "Jala un brazo recto cruzando el pecho con la otra mano.")
stretch("st_sleeper", "Sleeper stretch", "Estiramiento del durmiente", "RD",
        "Lie on your side, arm at 90°, gently push the forearm toward the floor.",
        "Acostado de lado con el brazo a 90°, empuja suave el antebrazo hacia el piso.")
stretch("st_triceps_overhead", "Overhead triceps stretch", "Tríceps sobre la cabeza", "TRI",
        "Reach one hand down your back, gently push the elbow with the other hand.",
        "Lleva una mano por la espalda y empuja suave el codo con la otra mano.")
stretch("st_lat_side_bend", "Standing side bend", "Inclinación lateral de pie", "LA",
        "Reach both arms overhead and lean to one side, then the other.",
        "Sube ambos brazos y inclínate hacia un lado y luego al otro.")
stretch("st_lat_doorway", "Lat stretch (hold a post)", "Dorsal sujetando un poste", "LA",
        "Hold a post at waist height, sit the hips back and away to stretch along the side.",
        "Sujeta un poste a la altura de la cintura y lleva la cadera atrás y hacia afuera para estirar el costado.")
stretch("st_child_pose", "Child's pose", "Postura del niño", "LB",
        "Kneel, sit back on the heels and walk the hands forward, forehead down.",
        "De rodillas, siéntate en los talones y estira las manos al frente con la frente abajo.")
stretch("st_cat_cow", "Cat-cow", "Gato-vaca", "LB",
        "On hands and knees, slowly round then arch the spine with the breath.",
        "En cuatro puntos, redondea y luego arquea la espalda lento con la respiración.")
stretch("st_cobra", "Cobra", "Cobra", "AB",
        "Lying face down, press the chest up with the hips on the floor.",
        "Boca abajo, sube el pecho empujando con las manos, cadera en el piso.")
stretch("st_down_dog", "Downward dog", "Perro boca abajo", "HA",
        "Hands and feet on the floor, hips high in a V. Push the floor away, heels toward the floor.",
        "Manos y pies en el piso, cadera alta en V. Empuja el piso y lleva los talones hacia abajo.")
stretch("st_thoracic_open_book", "Open book (upper back)", "Libro abierto (espalda alta)", "UB",
        "Lie on your side, knees bent, open the top arm across to the other side, following it with your eyes.",
        "De lado con rodillas dobladas, abre el brazo de arriba hacia el otro lado siguiéndolo con la mirada.")
stretch("st_spinal_twist", "Lying spinal twist", "Torsión acostado", "OB",
        "On your back, drop both knees to one side, arms out.",
        "Boca arriba, deja caer ambas rodillas a un lado con los brazos abiertos.")
stretch("st_neck_side", "Neck side stretch", "Cuello lateral", "TR",
        "Tilt the ear toward the shoulder, the other shoulder relaxed down.",
        "Inclina la oreja al hombro con el otro hombro relajado hacia abajo.")
stretch("st_wrist_flexor", "Wrist flexor stretch", "Flexores de muñeca", "FA",
        "Arm straight, palm up, gently pull the fingers back.",
        "Brazo recto, palma arriba, jala suave los dedos hacia atrás.")
stretch("st_worlds_greatest", "World's greatest stretch", "El mejor estiramiento del mundo", "QU",
        "From a lunge, place the hand inside the front foot, then rotate the other arm up to the ceiling.",
        "Desde zancada, pon la mano por dentro del pie delantero y gira el otro brazo hacia el techo.")
stretch("st_squat_hold", "Deep squat hold", "Sentadilla profunda sostenida", "AD",
        "Sit into a deep squat, heels down, elbows gently pushing the knees out.",
        "Baja a una sentadilla profunda con talones abajo y empuja suave las rodillas con los codos.")


stretch("st_glute_seated", "Seated glute stretch", "Glúteo sentado", "GL",
        "Sit tall, cross one ankle over the other knee and lean forward with a long back.",
        "Siéntate derecho, cruza un tobillo sobre la otra rodilla e inclínate al frente con la espalda larga.")
stretch("st_biceps_wall", "Wall biceps stretch", "Bíceps en la pared", "BI",
        "Palm on a wall behind you at shoulder height, arm straight, slowly turn the body away.",
        "Palma en una pared detrás de ti a la altura del hombro, brazo recto, gira el cuerpo lento hacia afuera.")


def main():
    ids = [e["id"] for e in OUT]
    dup = {i for i in ids if ids.count(i) > 1}
    assert not dup, f"duplicate ids: {dup}"
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    out = os.path.join(root, "app", "src", "main", "assets", "exercises.json")
    with open(out, "w", encoding="utf-8") as f:
        json.dump(OUT, f, ensure_ascii=False, indent=0)
    print(f"wrote {len(OUT)} exercises -> {out}")


if __name__ == "__main__":
    main()
