/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/programs/Programs.kt                    ║
║ Built-in training plans and calisthenics skill ladders.            ║
║ All plans are ORIGINAL generic templates (not copies of branded    ║
║ programs). Exercise ids match assets/exercises.json.               ║
║                                                                    ║
║ A Program has Days; a Day has Items (exercise, sets, rep range,    ║
║ target RPE, rest). "ladder:pushup" means "use the step of the       ║
║ push-up ladder you are on right now".                              ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.programs

import com.gymcat.app.core.model.EquipmentAccess

data class TemplateItem(
    val exerciseId: String,
    val sets: Int,
    val repMin: Int = 0,
    val repMax: Int = 0,
    val rpe: Double? = null,
    val restSec: Int = 0,          // 0 = use the default for that exercise type
    val warmups: Boolean = false,
    val holdSec: Int = 0,          // for planks and stretches
    val minutes: Int = 0,          // for cardio
)

data class ProgramDay(val nameEn: String, val nameEs: String, val items: List<TemplateItem>)

data class Program(
    val id: String,
    val nameEn: String, val nameEs: String,
    val descEn: String, val descEs: String,
    val daysPerWeek: Int,
    val days: List<ProgramDay>,
) {
    fun name(es: Boolean) = if (es) nameEs else nameEn
    fun desc(es: Boolean) = if (es) descEs else descEn
}

/** One step of a ladder and the target that "unlocks" the next step. */
data class LadderStep(val exerciseId: String, val sets: Int, val reps: Int = 0, val holdSec: Int = 0)
data class Ladder(val id: String, val nameEn: String, val nameEs: String, val emoji: String, val steps: List<LadderStep>) {
    fun name(es: Boolean) = if (es) nameEs else nameEn
}

object Programs {

    private fun lift(id: String, sets: Int, min: Int, max: Int, rpe: Double, warm: Boolean = false) =
        TemplateItem(id, sets, min, max, rpe, warmups = warm)
    private fun hold(id: String, sets: Int, sec: Int) = TemplateItem(id, sets, holdSec = sec)
    private fun cardio(id: String, min: Int) = TemplateItem(id, 1, minutes = min)
    private fun stretch(id: String, sec: Int = 30) = TemplateItem(id, 1, holdSec = sec)

    private val stretchSet = listOf(
        stretch("st_hamstring_fold"), stretch("st_quad_standing"), stretch("st_hip_flexor"),
        stretch("st_calf_wall"), stretch("st_chest_doorway"), stretch("st_child_pose"),
    )

    val all: List<Program> = listOf(
        Program(
            "beginner_full_body", "Beginner full body", "Cuerpo completo principiante",
            "3 days a week. Alternate A and B. Add weight when your sets feel easier than RPE 8.",
            "3 días por semana. Alterna A y B. Sube peso cuando las series se sientan más fáciles que RPE 8.",
            3,
            listOf(
                ProgramDay("Day A", "Día A", listOf(
                    lift("bb_back_squat", 3, 5, 5, 7.5, true),
                    lift("bb_bench_press", 3, 5, 5, 7.5, true),
                    lift("bb_row", 3, 8, 10, 8.0),
                    hold("plank", 3, 30),
                )),
                ProgramDay("Day B", "Día B", listOf(
                    lift("bb_deadlift", 2, 5, 5, 7.5, true),
                    lift("bb_ohp", 3, 5, 6, 7.5, true),
                    lift("lat_pulldown_wide", 3, 8, 12, 8.0),
                    lift("leg_curl_lying", 2, 10, 12, 8.0),
                    lift("hanging_knee_raise", 2, 8, 12, 8.0),
                )),
            ),
        ),
        Program(
            "dumbbell_full_body", "Dumbbell full body (home)", "Cuerpo completo con mancuernas (casa)",
            "3 days a week with only dumbbells. Alternate A and B.",
            "3 días por semana solo con mancuernas. Alterna A y B.",
            3,
            listOf(
                ProgramDay("Day A", "Día A", listOf(
                    lift("db_goblet_squat", 3, 8, 12, 8.0),
                    lift("db_bench_press", 3, 8, 12, 8.0),
                    lift("db_row_one_arm", 3, 8, 12, 8.0),
                    lift("db_curl", 2, 10, 15, 8.0),
                )),
                ProgramDay("Day B", "Día B", listOf(
                    lift("db_rdl", 3, 8, 12, 8.0),
                    lift("db_shoulder_press_seated", 3, 8, 12, 8.0),
                    lift("db_lunge_walking", 2, 10, 12, 8.0),
                    hold("dead_bug", 2, 30),
                )),
            ),
        ),
        Program(
            "calisthenics", "Calisthenics ladders", "Escaleras de calistenia",
            "3 days a week, bodyweight only. Each ladder exercise uses the step you are on.",
            "3 días por semana, solo peso corporal. Cada escalera usa el nivel en el que estás.",
            3,
            listOf(
                ProgramDay("Push + core", "Empuje + abdomen", listOf(
                    TemplateItem("ladder:pushup", 3, 6, 12, 8.0),
                    TemplateItem("ladder:dip", 3, 5, 12, 8.0),
                    TemplateItem("ladder:handstand", 3, 5, 10, 8.0),
                    TemplateItem("ladder:core", 3),
                )),
                ProgramDay("Pull + legs", "Jalón + piernas", listOf(
                    TemplateItem("ladder:pullup", 3, 3, 10, 8.0),
                    lift("inverted_row", 3, 8, 12, 8.0),
                    TemplateItem("ladder:squat", 3, 8, 15, 8.0),
                    lift("glute_bridge_bw", 2, 12, 20, 8.0),
                )),
                ProgramDay("Skills", "Habilidades", listOf(
                    TemplateItem("ladder:muscleup", 3, 3, 8, 8.0),
                    TemplateItem("ladder:pushup", 2, 8, 15, 8.0),
                    TemplateItem("ladder:squat", 2, 8, 15, 8.0),
                    hold("hollow_hold", 3, 30),
                )),
            ),
        ),
        Program(
            "cardio_stretch", "Cardio + stretch", "Cardio + estiramiento",
            "3 days a week. Easy cardio you can talk through, then a guided stretch.",
            "3 días por semana. Cardio suave (puedes hablar), luego estiramiento guiado.",
            3,
            listOf(
                ProgramDay("Run + stretch", "Correr + estirar", listOf(cardio("treadmill_run", 20)) + stretchSet),
                ProgramDay("Bike + mobility", "Bici + movilidad", listOf(
                    cardio("stationary_bike", 30),
                    stretch("st_cat_cow"), stretch("st_thoracic_open_book"), stretch("st_pigeon"),
                    stretch("st_down_dog"), stretch("st_figure4"),
                )),
                ProgramDay("Row + rope + stretch", "Remo + cuerda + estirar", listOf(
                    cardio("rowing_machine", 15), cardio("jump_rope", 5),
                ) + stretchSet),
            ),
        ),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }

    /** Onboarding picks a program from the equipment you have and your goal. */
    fun suggest(access: EquipmentAccess, cardioFocus: Boolean): Program = when {
        cardioFocus -> byId("cardio_stretch")!!
        access == EquipmentAccess.FULL_GYM -> byId("beginner_full_body")!!
        access == EquipmentAccess.HOME_DUMBBELLS -> byId("dumbbell_full_body")!!
        else -> byId("calisthenics")!!
    }

    // ── Skill ladders. Target to move up = all working sets reach the reps (or seconds).
    val ladders: List<Ladder> = listOf(
        Ladder("pushup", "Push-up", "Lagartija", "🙌", listOf(
            LadderStep("pu_wall", 3, 15), LadderStep("pu_incline", 3, 15), LadderStep("pu_knee", 3, 15),
            LadderStep("pu_standard", 3, 12), LadderStep("pu_diamond", 3, 12),
            LadderStep("pu_archer", 3, 8), LadderStep("pu_one_arm", 3, 5),
        )),
        Ladder("pullup", "Pull-up", "Dominada", "🧗", listOf(
            LadderStep("pl_dead_hang", 3, holdSec = 30), LadderStep("pl_scap_pull", 3, 10),
            LadderStep("pl_negative", 3, 5), LadderStep("pl_band", 3, 8), LadderStep("pl_pullup", 3, 10),
            LadderStep("pl_lsit_pullup", 3, 6), LadderStep("pl_archer", 3, 5),
        )),
        Ladder("squat", "Squat → pistol", "Sentadilla → pistol", "🦵", listOf(
            LadderStep("sq_assisted", 3, 15), LadderStep("sq_bodyweight", 3, 20),
            LadderStep("sq_bulgarian_bw", 3, 12), LadderStep("sq_shrimp", 3, 8), LadderStep("sq_pistol", 3, 5),
        )),
        Ladder("core", "Core", "Abdomen", "🧱", listOf(
            LadderStep("plank", 3, holdSec = 60), LadderStep("hollow_hold", 3, holdSec = 30),
            LadderStep("tuck_lsit", 3, holdSec = 15), LadderStep("lsit", 3, holdSec = 15),
        )),
        Ladder("handstand", "Handstand", "Parada de manos", "🤸", listOf(
            LadderStep("pike_pushup", 3, 10), LadderStep("pike_pushup_elevated", 3, 10),
            LadderStep("wall_handstand_hold", 3, holdSec = 45), LadderStep("hspu_wall", 3, 5),
        )),
        Ladder("dip", "Dip", "Fondos", "⬇️", listOf(
            LadderStep("dip_bench", 3, 15), LadderStep("dip_parallel", 3, 12), LadderStep("dip_weighted", 3, 8),
        )),
        Ladder("muscleup", "Muscle-up", "Muscle-up", "🚀", listOf(
            LadderStep("pl_high_pullup", 3, 6), LadderStep("muscle_up_jumping", 3, 5), LadderStep("muscle_up", 3, 3),
        )),
    )

    fun ladder(id: String) = ladders.firstOrNull { it.id == id }

    /**
     * Ready to move up? Every working set of the last session hit the target.
     * reps/secs = the working sets you did on the current step.
     * Example: target 3 × 12; you did 12, 13, 12 → true. 12, 11, 12 → false.
     */
    fun readyToLevelUp(step: LadderStep, reps: List<Int>, secs: List<Int>): Boolean =
        if (step.holdSec > 0) secs.size >= step.sets && secs.take(step.sets).all { it >= step.holdSec }
        else reps.size >= step.sets && reps.sortedDescending().take(step.sets).all { it >= step.reps }
}
