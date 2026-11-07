#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/strings/build_strings.py
Writes BOTH language files from one table so English and Spanish never drift apart:
  app/src/main/res/values/strings.xml      (English, default)
  app/src/main/res/values-es/strings.xml   (Spanish — used when the phone is in Spanish)
To change a text: edit it here, run  python3 tools/strings/build_strings.py
%1$s = text placeholder, %1$d = number placeholder (filled in by the app).
"""
import os
from xml.sax.saxutils import escape

S = {
 "app_name": ("Gym Cat", "Gym Cat"),
 "tab_home": ("Home", "Inicio"), "tab_routines": ("Routines", "Rutinas"),
 "tab_progress": ("Progress", "Progreso"), "tab_body": ("Body", "Cuerpo"),
 "home_subtitle": ("Let's move today", "A moverse hoy"),
 "settings": ("Settings", "Ajustes"), "back": ("Back", "Atrás"), "cancel": ("Cancel", "Cancelar"),
 "save": ("Save", "Guardar"), "ok": ("OK", "OK"), "done": ("Done", "Listo"), "delete": ("Delete", "Borrar"),
 "edit": ("Edit", "Editar"), "add": ("Add", "Agregar"), "next": ("Next", "Siguiente"), "start": ("Start", "Empezar"),
 "stop": ("Stop", "Detener"), "pause": ("Pause", "Pausa"), "skip": ("Skip", "Saltar"), "change": ("Change", "Cambiar"),
 "clear": ("Clear", "Limpiar"), "all": ("All", "Todos"), "none": ("None", "Ninguno"), "allow": ("Allow", "Permitir"),
 "share": ("Share", "Compartir"), "search": ("Search", "Buscar"), "name": ("Name", "Nombre"), "emoji": ("Emoji", "Emoji"),
 "type": ("Type", "Tipo"), "time": ("Time", "Hora"), "total": ("Total", "Total"), "latest": ("Latest", "Último"),
 # Home
 "resume_workout": ("Resume workout", "Continuar entrenamiento"),
 "missed_day_title": ("You missed a planned day", "Te saltaste un día del plan"),
 "missed_day_body": ("Do that workout now, or skip it and move to the next day?", "¿Haces ese entrenamiento ahora o lo saltas y pasas al siguiente día?"),
 "do_it_now": ("Do it now", "Hacerlo ahora"), "skip_it": ("Skip it", "Saltarlo"),
 "deload_title": ("Time for a lighter week?", "¿Toca una semana ligera?"),
 "deload_body": ("Progress has stalled or it's been 6+ weeks. A deload uses 90% of the weight and half the sets.",
                 "El progreso se estancó o ya pasaron 6+ semanas. Una descarga usa 90% del peso y la mitad de las series."),
 "not_now": ("Not now", "Ahora no"),
 "start_deload": ("Start a deload workout", "Empezar entrenamiento de descarga"),
 "next_workout": ("Next workout", "Próximo entrenamiento"),
 "no_plan_yet": ("No plan yet. Start a free workout or pick a program.", "Aún no tienes plan. Empieza uno libre o elige un programa."),
 "free_workout": ("Free workout", "Entrenamiento libre"), "start_empty": ("Empty workout", "Entrenamiento vacío"),
 "pick_program": ("Pick a program", "Elegir programa"), "this_week": ("This week", "Esta semana"),
 "days_of_goal": ("%1$d of %2$d days", "%1$d de %2$d días"),
 "streak_weeks": ("%1$d-week streak", "Racha de %1$d semanas"), "freezes_n": ("%1$d freezes", "%1$d congeladores"),
 "best_streak": ("best %1$d", "mejor %1$d"),
 "latest_win": ("Latest win", "Último logro"),
 "latest_win_empty": ("Your first badge is one workout away.", "Tu primera medalla está a un entrenamiento."),
 "start_workout": ("Start workout", "Empezar entrenamiento"), "start_program_day": ("Next program day", "Siguiente día del programa"),
 "cardio_gps": ("Outdoor run (GPS)", "Correr al aire libre (GPS)"), "cardio_timer": ("Cardio timer", "Cronómetro de cardio"),
 "cardio_intervals": ("Interval timer", "Temporizador de intervalos"), "cardio_manual": ("Log cardio by hand", "Registrar cardio a mano"),
 "guided_stretch": ("Guided stretch", "Estiramiento guiado"), "log_class": ("Class or sport", "Clase o deporte"),
 "minutes": ("Minutes", "Minutos"),
 # Workout
 "no_active_workout": ("No workout in progress.", "No hay entrenamiento en curso."),
 "discard": ("Discard", "Descartar"), "finish": ("Finish", "Terminar"), "rest": ("Rest", "Descanso"),
 "add_exercise": ("Add exercise", "Agregar ejercicio"), "add_set": ("Add set", "Agregar serie"),
 "discard_title": ("Discard this workout?", "¿Descartar este entrenamiento?"),
 "nothing_logged": ("No sets were ticked, so nothing was saved.", "No marcaste ninguna serie, así que no se guardó nada."),
 "discard_body": ("Nothing from it will be saved.", "No se guardará nada."),
 "superset": ("Superset", "Superserie"), "how_to": ("How to", "Cómo se hace"),
 "swap_exercise": ("Swap exercise", "Cambiar ejercicio"), "skip_exercise": ("Skip exercise", "Saltar ejercicio"),
 "move_up": ("Move up", "Subir"), "move_down": ("Move down", "Bajar"),
 "superset_with_next": ("Superset with next", "Superserie con el siguiente"),
 "plate_calculator": ("Plate calculator", "Calculadora de discos"),
 "last_time": ("Last time: %1$s", "La vez pasada: %1$s"),
 "suggested_weight": ("Try %1$s (tap to use)", "Prueba %1$s (toca para usar)"),
 "set_warmup": ("Warm-up", "Calentamiento"), "set_working": ("Working", "Efectiva"),
 "set_drop": ("Drop set", "Descendente"), "set_failure": ("To failure", "Al fallo"),
 "per_dumbbell": ("per DB", "por manc."), "reps": ("Reps", "Reps"), "seconds": ("Seconds", "Segundos"),
 "e1rm_both": ("Est. 1RM — Epley %1$s · Brzycki %2$s", "1RM estimado — Epley %1$s · Brzycki %2$s"),
 "note": ("Note", "Nota"), "notes": ("Notes", "Notas"), "add_note": ("Add note", "Agregar nota"),
 "target": ("Target", "Objetivo"), "bar_is": ("Bar: %1$s", "Barra: %1$s"), "per_side": ("Per side", "Por lado"),
 "plates_leftover": ("%1$s can't be made with these plates", "No se pueden formar %1$s con estos discos"),
 "plates_total": ("Total on the bar: %1$s", "Total en la barra: %1$s"),
 "finish_title": ("Finish workout", "Terminar entrenamiento"),
 "how_did_it_feel": ("How did it feel?", "¿Cómo se sintió?"),
 "save_changes_routine": ("Save changes to the routine", "Guardar cambios en la rutina"),
 "save_as_routine": ("Save as a new routine", "Guardar como rutina nueva"),
 # Finish
 "workout_done": ("Workout done!", "¡Entrenamiento listo!"), "duration": ("Duration", "Duración"),
 "volume": ("Volume", "Volumen"), "sets": ("Sets", "Series"), "calories": ("Calories", "Calorías"),
 "protein_today": ("Protein so far today: %1$d g", "Proteína hoy hasta ahora: %1$d g"),
 "new_records": ("New records", "Nuevos récords"), "new_badges": ("New badges", "Nuevas medallas"),
 "level_up_title": ("Level up %1$s?", "¿Subir de nivel en %1$s?"),
 "level_up_body": ("You hit the target. Next step: %1$s", "Llegaste a la meta. Siguiente nivel: %1$s"),
 "level_up_yes": ("Level up", "Subir de nivel"), "not_yet": ("Not yet", "Todavía no"),
 "pr_weight": ("%1$s: heaviest %2$s", "%1$s: más pesado %2$s"),
 "pr_e1rm": ("%1$s: best est. 1RM %2$s", "%1$s: mejor 1RM estimado %2$s"),
 "pr_reps": ("%1$s: %2$d reps", "%1$s: %2$d reps"), "pr_hold": ("%1$s: %2$s hold", "%1$s: %2$s sostenido"),
 "pr_distance": ("%1$s: longest %2$s", "%1$s: más larga %2$s"), "pr_pace": ("%1$s: fastest %2$s", "%1$s: más rápido %2$s"),
 # Routines
 "programs": ("Programs", "Programas"), "active": ("Active", "Activo"),
 "days_per_week_n": ("%1$d days a week", "%1$d días por semana"),
 "start_next_day": ("Start next day", "Empezar siguiente día"), "stop_program": ("Stop program", "Dejar programa"),
 "follow_program": ("Follow this program", "Seguir este programa"), "my_routines": ("My routines", "Mis rutinas"),
 "new_routine": ("New", "Nueva"), "import_file": ("Import file", "Importar archivo"), "scan_qr": ("Scan QR", "Escanear QR"),
 "no_routines": ("No routines yet. Make one or import a friend's.", "Aún no hay rutinas. Crea una o importa la de un amigo."),
 "share_file": ("Share as file", "Compartir como archivo"), "show_qr": ("Show QR", "Mostrar QR"),
 "delete_routine": ("Delete this routine?", "¿Borrar esta rutina?"),
 "imported_routine": ("Imported \"%1$s\"", "Se importó \"%1$s\""),
 "import_failed": ("That isn't a Gym Cat routine.", "Eso no es una rutina de Gym Cat."),
 "routine_name": ("Routine name", "Nombre de la rutina"), "reps_min": ("Reps from", "Reps desde"),
 "reps_max": ("Reps to", "Reps hasta"), "target_rpe": ("Target RPE (how hard, 1–10)", "RPE objetivo (qué tan duro, 1–10)"),
 "auto_warmups": ("Automatic warm-up sets", "Series de calentamiento automáticas"), "default_short": ("Default", "Normal"),
 "qr_help": ("Ask your friend to open Gym Cat → Routines → Scan QR.", "Pide a tu amigo abrir Gym Cat → Rutinas → Escanear QR."),
 "qr_too_big": ("This routine is too long for a QR. Share it as a file.", "Esta rutina es muy larga para un QR. Compártela como archivo."),
 "camera_needed": ("Camera permission is needed to scan.", "Se necesita permiso de cámara para escanear."),
 # Library
 "library": ("Exercise library", "Biblioteca de ejercicios"), "choose_exercise": ("Choose exercise", "Elige ejercicio"),
 "my_exercise": ("Mine", "Mío"), "muscle": ("Muscle", "Músculo"), "equipment": ("Equipment", "Equipo"),
 "n_exercises": ("%1$d exercises", "%1$d ejercicios"), "favorite": ("Favorite", "Favorito"),
 "muscles_worked": ("Muscles worked", "Músculos trabajados"), "primary": ("Main", "Principal"),
 "secondary": ("Helpers", "Secundarios"), "mistakes": ("Common mistakes", "Errores comunes"),
 "your_records": ("Your records", "Tus récords"), "heaviest": ("Heaviest", "Más pesado"),
 "chart_e1rm_legend": ("Blue: Epley 1RM · Navy: Brzycki 1RM · Yellow: top weight",
                       "Azul: 1RM Epley · Marino: 1RM Brzycki · Amarillo: peso máximo"),
 "most_reps": ("Most reps", "Más repeticiones"), "longest_hold": ("Longest hold", "Mayor tiempo"),
 "longest": ("Longest", "Más larga"), "your_rest": ("Your rest time", "Tu descanso"),
 "copy_and_edit": ("Copy & edit", "Copiar y editar"), "how_logged": ("How it's logged", "Cómo se registra"),
 "compound_lift": ("Compound lift (longer rest)", "Ejercicio compuesto (más descanso)"),
 # Cardio / stretch
 "distance": ("Distance", "Distancia"), "pace": ("Pace", "Ritmo"), "work": ("Work", "Trabajo"),
 "rounds": ("Rounds", "Rondas"), "round_of": ("%1$d of %2$d", "%1$d de %2$d"),
 "intervals_done": ("Intervals done. Nice!", "Intervalos terminados. ¡Bien!"),
 "gps_permission_title": ("Location for your run", "Ubicación para tu carrera"),
 "gps_permission_body": ("Gym Cat uses the GPS only while a run is recording, to measure distance. No internet, no map, nothing leaves your phone.",
                         "Gym Cat usa el GPS solo mientras grabas una carrera, para medir la distancia. Sin internet, sin mapa, nada sale de tu teléfono."),
 "splits": ("Mile splits", "Tiempo por milla"), "start_run": ("Start run", "Empezar carrera"),
 "next_up": ("Next:", "Siguiente:"), "stretch_done": ("All stretched out!", "¡Bien estirado!"),
 "add_stretch": ("Add stretch", "Agregar estiramiento"),
 # Progress
 "history": ("History", "Historial"),
 "no_workouts_title": ("No workouts yet", "Aún no hay entrenamientos"),
 "no_workouts_body": ("Tap the big + to start your first one.", "Toca el + grande para empezar el primero."),
 "calendar": ("Calendar", "Calendario"), "workouts_per_week": ("Workouts per week", "Entrenamientos por semana"),
 "per_exercise": ("Per exercise", "Por ejercicio"),
 "sets_per_muscle_week": ("Sets per muscle this week", "Series por músculo esta semana"),
 "nothing_this_week": ("Nothing yet this week.", "Nada todavía esta semana."),
 "weekly_volume": ("Weekly volume (lb × reps)", "Volumen semanal (lb × reps)"),
 "body_weight": ("Body weight", "Peso corporal"),
 "body_weight_hc_off": ("Turn on Health Connect in Settings to see the weight you log in Gym & Ñam.",
                        "Activa Health Connect en Ajustes para ver el peso que registras en Gym & Ñam."),
 "body_weight_none": ("No weight entries yet.", "Aún no hay registros de peso."),
 "records": ("Records", "Récords"), "badges": ("Badges", "Medallas"), "skill_ladders": ("Skill ladders", "Escaleras de habilidad"),
 "ladder_current": ("Now: %1$s", "Ahora: %1$s"),
 "ladder_target_reps": ("Goal to move up: %1$d × %2$d reps", "Meta para subir: %1$d × %2$d reps"),
 "ladder_target_hold": ("Goal to move up: %1$d × %2$d s", "Meta para subir: %1$d × %2$d s"),
 "step_down": ("Easier", "Más fácil"), "step_up": ("Harder", "Más difícil"),
 "history_hint": ("Tap a workout to see every set. Each exercise's full history is in the library.",
                  "Toca un entrenamiento para ver cada serie. El historial completo de cada ejercicio está en la biblioteca."),
 "delete_workout": ("Delete this workout?", "¿Borrar este entrenamiento?"),
 "delete_workout_body": ("Its calories are also removed from Health Connect.", "Sus calorías también se quitan de Health Connect."),
 # Body
 "muscle_map": ("Muscle map", "Mapa muscular"),
 "muscle_map_hint": ("Darker = more sets this week (10+ = full color).", "Más oscuro = más series esta semana (10+ = color completo)."),
 "front": ("Front", "Frente"), "back_view": ("Back", "Espalda"), "measurements": ("Measurements", "Medidas"),
 "no_measurements": ("No measurements yet.", "Aún no hay medidas."),
 "m_neck": ("Neck", "Cuello"), "m_chest": ("Chest", "Pecho"), "m_arm": ("Arm", "Brazo"),
 "m_waist": ("Waist", "Cintura"), "m_hip": ("Hips", "Cadera"), "m_thigh": ("Thigh", "Muslo"),
 "body_fat_navy": ("Body fat (Navy method): %1$s%%", "Grasa corporal (método Navy): %1$s%%"),
 "body_fat_note": ("Estimate, about ± 3–4 points.", "Estimación, aprox. ± 3–4 puntos."),
 "body_fat_needs": ("Body fat needs neck + waist (+ hips for women) and your height in Settings.",
                    "La grasa corporal necesita cuello + cintura (+ cadera en mujeres) y tu estatura en Ajustes."),
 "progress_photos": ("Progress photos", "Fotos de progreso"),
 "photos_private": ("Saved only inside Gym Cat, never in your gallery.", "Se guardan solo dentro de Gym Cat, nunca en tu galería."),
 "camera": ("Camera", "Cámara"), "gallery": ("Gallery", "Galería"), "compare": ("Compare", "Comparar"),
 "tap_two_to_compare": ("Tap two photos to compare them.", "Toca dos fotos para compararlas."),
 "delete_photo": ("Delete selected photo", "Borrar foto seleccionada"),
 # Settings
 "look": ("Look", "Apariencia"), "palette": ("Color", "Color"), "dark_mode": ("Dark mode", "Modo oscuro"),
 "units": ("Units", "Unidades"), "metric_units": ("Show kg / km / cm", "Mostrar kg / km / cm"),
 "metric_units_sub": ("Only changes what you see. Data stays in lb so it matches Gym & Ñam.",
                      "Solo cambia lo que ves. Los datos se guardan en lb para coincidir con Gym & Ñam."),
 "kg_plates": ("Kilo plates", "Discos en kilos"),
 "kg_plates_sub": ("Plate calculator uses a 20 kg bar and kg plates.", "La calculadora usa barra de 20 kg y discos en kg."),
 "profile": ("Profile", "Perfil"), "male": ("Male", "Hombre"), "female": ("Female", "Mujer"),
 "height": ("Height", "Estatura"), "using_hc_weight": ("Using %1$s from Health Connect", "Usando %1$s de Health Connect"),
 "weekly_goal_days": ("Weekly goal (days)", "Meta semanal (días)"),
 "workout_settings": ("Workout", "Entrenamiento"), "default_rest": ("Default rest", "Descanso normal"),
 "compound": ("Compound", "Compuesto"), "isolation": ("Isolation", "Aislamiento"), "calisthenics": ("Calisthenics", "Calistenia"),
 "keep_screen_on": ("Keep screen on during workouts", "Pantalla encendida al entrenar"),
 "workout_notification": ("Timer notification", "Notificación con el tiempo"),
 "sounds": ("Sounds", "Sonidos"), "vibration": ("Vibration", "Vibración"), "meows": ("Meows", "Maullidos"),
 "meows_sub": ("Plays your meow sounds after some sets and on badges.", "Reproduce tus maullidos después de algunas series y con medallas."),
 "voice_cues": ("Voice cues", "Avisos de voz"),
 "voice_cues_sub": ("Spoken \"rest over\", interval changes and mile splits.", "Dice \"fin del descanso\", cambios de intervalo y cada milla."),
 "health_connect": ("Health Connect", "Health Connect"), "hc_switch": ("Link with Gym & Ñam", "Conectar con Gym & Ñam"),
 "hc_sub": ("Writes workout calories; reads body weight and today's protein.", "Escribe las calorías del entrenamiento; lee peso corporal y proteína de hoy."),
 "hc_double_count": ("Tip: don't also type gym workouts in Gym & Ñam's exercise sheet, or they count twice.",
                     "Consejo: no registres también tus entrenamientos en la hoja de ejercicio de Gym & Ñam, o contarán doble."),
 "hc_unavailable": ("Health Connect isn't installed on this phone.", "Health Connect no está instalado en este teléfono."),
 "privacy_lock": ("Privacy lock", "Bloqueo de privacidad"), "app_lock": ("Lock Gym Cat", "Bloquear Gym Cat"),
 "app_lock_sub": ("PIN (and fingerprint). Locks again after 60 s away.", "PIN (y huella). Se bloquea de nuevo tras 60 s fuera."),
 "fingerprint": ("Fingerprint", "Huella digital"), "set_pin": ("Set a PIN", "Crear PIN"),
 "pin_4_8": ("PIN (4–8 digits)", "PIN (4–8 dígitos)"), "pin_repeat": ("Repeat PIN", "Repite el PIN"),
 "unlock_title": ("Unlock Gym Cat", "Desbloquear Gym Cat"), "use_pin": ("Use PIN", "Usar PIN"),
 "wrong_pin": ("Wrong PIN", "PIN incorrecto"),
 "reminders": ("Reminders", "Recordatorios"), "remind_workout": ("Workout days", "Días de entrenamiento"),
 "remind_measure": ("Measure day", "Día de medidas"),
 "remind_measure_sub": ("1st of each month: tape measures and photos.", "El día 1 de cada mes: medidas y fotos."),
 "backup": ("Backup", "Respaldo"), "backup_now": ("Back up now", "Respaldar ahora"), "restore": ("Restore", "Restaurar"),
 "auto_backup": ("Weekly auto-backup", "Respaldo automático semanal"),
 "auto_backup_sub": ("Saves to Downloads/GymCat. Includes your photos; other apps with file access can see that folder.",
                     "Se guarda en Descargas/GymCat. Incluye tus fotos; otras apps con acceso a archivos pueden ver esa carpeta."),
 "last_backup": ("Last backup: %1$s", "Último respaldo: %1$s"),
 "backup_failed": ("Backup couldn't be saved. Try another folder.", "No se pudo guardar el respaldo. Prueba otra carpeta."),
 "backup_saved": ("Backup saved", "Respaldo guardado"),
 "restore_warning": ("This replaces everything in Gym Cat with the backup.", "Esto reemplaza todo en Gym Cat con el respaldo."),
 "restore_done": ("Backup restored", "Respaldo restaurado"), "restore_failed": ("That file isn't a Gym Cat backup.", "Ese archivo no es un respaldo de Gym Cat."),
 "about": ("About", "Acerca de"),
 "about_privacy": ("No internet, no accounts, no ads, no tracking. Your data stays on this phone.",
                   "Sin internet, sin cuentas, sin anuncios, sin rastreo. Tus datos se quedan en este teléfono."),
 "about_notices": ("Exercise texts and drawings are original. QR codes by ZXing (Apache 2.0). MET values: Compendium of Physical Activities.",
                   "Textos y dibujos de ejercicios originales. Códigos QR con ZXing (Apache 2.0). Valores MET: Compendium of Physical Activities."),
 "privacy_title": ("How Gym Cat uses health data", "Cómo usa Gym Cat los datos de salud"),
 "privacy_body": ("Gym Cat only uses Health Connect if you turn it on.\n\n• Writes: calories burned for each workout, so Gym & Ñam can add them to your day.\n• Reads: your body weight (for calorie estimates and the weight chart) and today's protein total.\n\nNothing is sent anywhere: Gym Cat has no internet access. You can turn this off any time in Settings or in Health Connect.",
                  "Gym Cat solo usa Health Connect si tú lo activas.\n\n• Escribe: las calorías quemadas de cada entrenamiento, para que Gym & Ñam las sume a tu día.\n• Lee: tu peso corporal (para estimar calorías y la gráfica de peso) y la proteína total de hoy.\n\nNada se envía a ningún lado: Gym Cat no tiene acceso a internet. Puedes apagarlo cuando quieras en Ajustes o en Health Connect."),
 # Onboarding
 "welcome_title": ("Hi! I'm Gym Cat", "¡Hola! Soy Gym Cat"),
 "welcome_body": ("A few quick questions and I'll suggest a plan for you.", "Unas preguntas rápidas y te sugiero un plan."),
 "language": ("Language", "Idioma"),
 "time_entry_hint": ("Type 1:30 (min:sec) or just seconds, like 90. For a clock time: 18:30.", "Escribe 1:30 (min:seg) o solo segundos, como 90. Para una hora: 18:30."),
 "lang_phone": ("Phone language", "Idioma del teléfono"),
 "units_imperial": ("lb · in · mi", "lb · in · mi"),
 "units_metric": ("kg · cm · km", "kg · cm · km"),
 "units_change_later": ("You can change these later in Settings.", "Puedes cambiarlos después en Ajustes."),
 "q_level": ("How much have you trained?", "¿Cuánto has entrenado?"),
 "q_goal": ("What's your main goal?", "¿Cuál es tu meta principal?"),
 "q_days": ("How many days a week can you train?", "¿Cuántos días a la semana puedes entrenar?"),
 "days": ("Days", "Días"), "q_equipment": ("What do you have?", "¿Con qué cuentas?"),
 "q_body": ("About you", "Sobre ti"),
 "q_body_why": ("Used for calories and body-fat estimates. Stays on your phone.", "Se usa para calorías y grasa corporal. Se queda en tu teléfono."),
 "read_from_gymnam": ("Read my weight from Gym & Ñam", "Leer mi peso de Gym & Ñam"),
 "q_program": ("Pick a program", "Elige un programa"), "suggested": ("Suggested", "Sugerido"),
 "no_program_free": ("No program — I'll log freely", "Sin programa — registro libre"), "lets_go": ("Let's go!", "¡Vamos!"),
 # Service / notifications / widgets
 "notif_resting": ("Resting", "Descansando"), "notif_training": ("Workout in progress", "Entrenamiento en curso"),
 "notif_gps": ("Running · %1$s mi", "Corriendo · %1$s mi"),
 "voice_rest_over": ("Rest over", "Fin del descanso"), "voice_mile_split": ("Mile %1$d", "Milla %1$d"),
 "channel_workout": ("Workout timer", "Tiempo de entrenamiento"), "channel_reminders": ("Reminders", "Recordatorios"),
 "remind_workout_title": ("Time to train 🐱", "Hora de entrenar 🐱"),
 "remind_workout_text": ("Today is a workout day. Your cat believes in you.", "Hoy toca entrenar. Tu gato cree en ti."),
 "remind_measure_title": ("Measure day 📏", "Día de medidas 📏"),
 "remind_measure_text": ("Take your tape measures and a progress photo.", "Toma tus medidas y una foto de progreso."),
 "widget_small_name": ("Gym Cat streak", "Racha Gym Cat"), "widget_small_desc": ("Your weekly streak", "Tu racha semanal"),
 "widget_medium_name": ("Gym Cat next workout", "Próximo entrenamiento Gym Cat"),
 "widget_medium_desc": ("Next workout with a Start button", "Próximo entrenamiento con botón Empezar"),
}

A = {
 "week_days_short": (["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"], ["Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"]),
 # Order MUST match the Kotlin enums in core/model/Models.kt
 "muscle_names": (["Chest", "Lats", "Upper back", "Traps", "Front delts", "Side delts", "Rear delts", "Biceps", "Triceps",
                   "Forearms", "Abs", "Obliques", "Lower back", "Glutes", "Quads", "Hamstrings", "Calves", "Adductors"],
                  ["Pecho", "Dorsales", "Espalda alta", "Trapecio", "Deltoides frontal", "Deltoides lateral", "Deltoides posterior",
                   "Bíceps", "Tríceps", "Antebrazos", "Abdomen", "Oblicuos", "Espalda baja", "Glúteos", "Cuádriceps",
                   "Isquiotibiales", "Pantorrillas", "Aductores"]),
 "equipment_names": (["Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Band", "Kettlebell", "Smith", "EZ bar",
                      "Trap bar", "Plate", "Other", "None"],
                     ["Barra", "Mancuerna", "Máquina", "Polea", "Peso corporal", "Liga", "Pesa rusa", "Smith", "Barra Z",
                      "Barra hexagonal", "Disco", "Otro", "Ninguno"]),
 "exercise_types": (["Strength", "Calisthenics", "Cardio", "Stretch"], ["Fuerza", "Calistenia", "Cardio", "Estiramiento"]),
 "log_modes": (["Weight × reps", "Bodyweight reps", "Hold (seconds)", "Cardio (time + distance)", "Stretch (seconds)"],
               ["Peso × reps", "Reps con peso corporal", "Sostener (segundos)", "Cardio (tiempo + distancia)", "Estiramiento (segundos)"]),
 "levels": (["New to the gym", "Some experience (6+ months)", "Experienced (2+ years)"],
            ["Nuevo en el gimnasio", "Algo de experiencia (6+ meses)", "Con experiencia (2+ años)"]),
 "goals": (["Get stronger", "Build muscle", "Lose fat", "General fitness"],
           ["Ser más fuerte", "Ganar músculo", "Perder grasa", "Condición general"]),
 "equipment_access": (["Full gym", "Dumbbells at home", "Just my body"],
                      ["Gimnasio completo", "Mancuernas en casa", "Solo mi cuerpo"]),
 "theme_modes": (["Follow phone", "Light", "Dark"], ["Como el teléfono", "Claro", "Oscuro"]),
 "class_types": (["Yoga", "Pilates", "Indoor cycling class", "HIIT class", "Dance class", "Soccer", "Basketball", "Tennis",
                  "Swimming", "Hiking", "Martial arts / boxing", "Other"],
                 ["Yoga", "Pilates", "Clase de ciclismo indoor", "Clase de HIIT", "Clase de baile", "Fútbol", "Básquetbol", "Tenis",
                  "Natación", "Senderismo", "Artes marciales / box", "Otro"]),
}


def esc(t):
    # Android rules: escape &, <, >, apostrophes and double quotes; keep \n as newline escape.
    t = escape(t).replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    return t


def write(lang_idx, path, header):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', f"<!-- {header} -->", "<resources>"]
    for k, v in S.items():
        t = v[lang_idx]
        # A plain "%" (like "90%") with no placeholders must be marked formatted="false".
        extra = ' formatted="false"' if "%" in t and "%1$" not in t else ""
        lines.append(f'    <string name="{k}"{extra}>{esc(t)}</string>')
    for k, v in A.items():
        lines.append(f'    <string-array name="{k}">')
        lines += [f"        <item>{esc(x)}</item>" for x in v[lang_idx]]
        lines.append("    </string-array>")
    lines.append("</resources>")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")


def main():
    for k, (en, es) in A.items():
        assert len(en) == len(es), k
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    res = os.path.join(root, "app", "src", "main", "res")
    write(0, os.path.join(res, "values", "strings.xml"),
          "PLAIN-ENGLISH GUIDE: all English text. Generated by tools/strings/build_strings.py - edit there.")
    write(1, os.path.join(res, "values-es", "strings.xml"),
          "GUÍA: todo el texto en español. Generado por tools/strings/build_strings.py - edita allí.")
    print(f"{len(S)} strings + {len(A)} arrays written (EN + ES)")


if __name__ == "__main__":
    main()
