/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/components/MuscleMap.kt                   ║
║ Front + back body figure. Each muscle is a simple oval (original   ║
║ drawing) colored by how many sets it got this week:                ║
║   color strength = sets ÷ 10  (10+ sets = full accent color)       ║
║ Sets count: primary muscle +1 per working set, secondary +0.5.     ║
║ Example: 3 bench sets → chest +3, front delts/triceps +1.5 each.   ║
║ Shapes are in a 100 × 200 box and scaled to fit the screen.        ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.gymcat.app.core.model.Muscle
import com.gymcat.app.core.model.Muscle.*
import com.gymcat.app.ui.theme.GymColors

private data class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float)

private fun pair(cx: Float, cy: Float, rx: Float, ry: Float) = listOf(Oval(cx, cy, rx, ry), Oval(100 - cx, cy, rx, ry))

private val FRONT: Map<Muscle, List<Oval>> = mapOf(
    TRAPS to pair(40f, 30f, 6f, 3f),
    CHEST to pair(41f, 43f, 9f, 7f),
    FRONT_DELTS to pair(29f, 36f, 6f, 6f),
    SIDE_DELTS to pair(24.5f, 41f, 3.5f, 6f),
    BICEPS to pair(24f, 57f, 5f, 10f),
    FOREARMS to pair(21f, 81f, 4f, 11f),
    ABS to listOf(Oval(50f, 61f, 8f, 8f), Oval(50f, 79f, 8f, 8f)),
    OBLIQUES to pair(37f, 72f, 4f, 10f),
    ADDUCTORS to pair(47f, 113f, 3f, 10f),
    QUADS to pair(41f, 122f, 7f, 20f),
    CALVES to pair(42f, 166f, 5f, 13f),
)

private val BACK: Map<Muscle, List<Oval>> = mapOf(
    TRAPS to listOf(Oval(50f, 34f, 14f, 7f)),
    REAR_DELTS to pair(29f, 38f, 6f, 6f),
    SIDE_DELTS to pair(24.5f, 42f, 3.5f, 6f),
    UPPER_BACK to listOf(Oval(50f, 48f, 12f, 6f)),
    LATS to pair(38f, 61f, 8f, 13f),
    TRICEPS to pair(24f, 57f, 5f, 10f),
    FOREARMS to pair(21f, 81f, 4f, 11f),
    LOWER_BACK to listOf(Oval(50f, 83f, 9f, 7f)),
    GLUTES to pair(43f, 100f, 8f, 8f),
    HAMSTRINGS to pair(42f, 127f, 7f, 17f),
    CALVES to pair(42f, 164f, 6f, 13f),
)

private fun DrawScope.body(base: Color) {
    val k = size.width / 100f
    fun rr(l: Float, t: Float, w: Float, h: Float, r: Float) =
        drawRoundRect(base, Offset(l * k, t * k), Size(w * k, h * k), CornerRadius(r * k, r * k))
    drawCircle(base, 10f * k, Offset(50f * k, 15f * k))          // head
    rr(46f, 23f, 8f, 8f, 3f)                                       // neck
    rr(29f, 29f, 42f, 72f, 12f)                                    // torso
    rr(18f, 33f, 11f, 62f, 6f); rr(71f, 33f, 11f, 62f, 6f)         // arms
    rr(32f, 98f, 16f, 90f, 8f); rr(52f, 98f, 16f, 90f, 8f)         // legs
}

private fun DrawScope.muscles(map: Map<Muscle, List<Oval>>, sets: Map<Muscle, Double>, empty: Color, full: Color) {
    val k = size.width / 100f
    for ((m, ovals) in map) {
        val t = ((sets[m] ?: 0.0) / 10.0).toFloat().coerceIn(0f, 1f)
        val col = lerp(empty, full, t)
        ovals.forEach { o -> drawOval(col, Offset((o.cx - o.rx) * k, (o.cy - o.ry) * k), Size(2 * o.rx * k, 2 * o.ry * k)) }
    }
}

@Composable
fun MuscleMap(sets: Map<Muscle, Double>, frontLabel: String, backLabel: String) {
    val c = GymColors.current
    val base = if (c.dark) Color(0xFF2A3340) else Color(0xFFE3E8EE)
    val empty = if (c.dark) Color(0xFF3A4554) else Color(0xFFCCD5DF)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        for ((label, map) in listOf(frontLabel to FRONT, backLabel to BACK)) {
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(0.5f)) { body(base); muscles(map, sets, empty, c.accent) }
                Muted(label)
            }
        }
    }
}
