/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/components/Components.kt                  ║
║ Small building blocks used on many screens:                        ║
║  • GCard        — rounded 28 dp card (Gym & Ñam family look)        ║
║  • CatArt       — shows a Gym Cat drawing if you added it, else an  ║
║                   emoji (so the app works before images exist)      ║
║  • Stepper      — big  −  value  +  control for weights and reps    ║
║  • LineChart / BarChart — simple charts drawn by hand (no library) ║
║  • RouteView    — draws a GPS route line (offline, no map)         ║
║ "@Composable" = a function that draws part of the screen.          ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.gymcat.app.R
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.theme.CardShape
import com.gymcat.app.ui.theme.GymColors

/** Names of the Gym Cat drawings (see docs/ASSETS_GUIDE.md). Drop PNGs into res/drawable-nodpi/. */
object Cat {
    const val WELCOME = "cat_04_welcome"; const val THINKING = "cat_05_thinking"; const val HOME = "cat_06_home"
    const val PR = "cat_07_pr"; const val STREAK = "cat_08_streak"; const val FREEZE = "cat_09_freeze"
    const val STREAK_LOST = "cat_10_streak_lost"; const val REST = "cat_11_rest"; const val REST_DAY = "cat_12_rest_day"
    const val BADGE = "cat_13_badge"; const val EMPTY = "cat_14_empty"; const val FINISHED = "cat_15_finished"
    const val BADGE_FRAME = "cat_16_badge_frame"
}

@Composable
fun GCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = GymColors.current
    val m = modifier.fillMaxWidth()
    if (onClick != null) Card(onClick = onClick, modifier = m, shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = c.card), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(18.dp), content = content)
    } else Card(modifier = m, shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = c.card), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp), fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, color = GymColors.current.text)

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 13) =
    Text(text, modifier, color = GymColors.current.text2, fontSize = size.sp)

/** Light tile color behind the cats in dark mode (same as the icon background). */
val CatTileLight = Color(0xFFE6F1FB)

/**
 * Looks up res/drawable-nodpi/<name>.png; shows the emoji when the image isn't there yet.
 * DARK MODE: the cats have navy outlines and navy equipment that disappear on a dark
 * background, so each one sits on a soft light rounded tile (like a sticker).
 */
@SuppressLint("DiscouragedApi")
@Composable
fun CatArt(name: String?, fallback: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val dark = GymColors.current.dark
    val id = if (name == null) 0 else ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
    if (id != 0) {
        if (dark) Box(modifier.size(size).clip(RoundedCornerShape(size * 0.22f)).background(CatTileLight).padding(size * 0.05f)) {
            Image(painterResource(id), contentDescription = null, modifier = Modifier.fillMaxSize())
        } else Image(painterResource(id), contentDescription = null, modifier = modifier.size(size))
    } else Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Text(fallback, fontSize = (size.value * 0.55f).sp)
    }
}

/**
 * One badge: the empty badge-frame drawing (cat_16) with the badge's emoji inside its
 * white circle. The circle sits a little above the middle of the frame (ribbon below),
 * so the emoji is nudged up by 6% of the size.
 */
@Composable
fun BadgeIcon(emoji: String, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        CatArt(Cat.BADGE_FRAME, "🏅", size)
        Text(emoji, fontSize = (size.value * 0.36f).sp, modifier = Modifier.offset(y = -size * 0.06f))
    }
}

/**
 * − value + with haptic ticks.
 * Tap the number to TYPE it instead:
 *   onSet    = what to do with the typed number (in the units shown)
 *   editValue = what the box starts with (default: the shown value)
 *   isTime   = the value is a time: type "1:30" (min:sec) or plain seconds ("90")
 * onValueClick = a screen's own tap action (used by the workout set cards).
 */
@Composable
fun Stepper(
    label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier,
    onValueClick: (() -> Unit)? = null, onSet: ((Double) -> Unit)? = null, editValue: String? = null, isTime: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val c = GymColors.current
    var typing by remember { mutableStateOf(false) }
    val tap: (() -> Unit)? = onValueClick ?: if (onSet != null) ({ typing = true }) else null
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Muted(label, size = 12)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onMinus() }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "−")
            }
            // Underlined = you can tap it and type.
            Text(value, Modifier.width(80.dp).let { if (tap != null) it.clickable { tap() } else it },
                textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.text,
                textDecoration = if (tap != null) TextDecoration.Underline else null)
            FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlus() }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "+")
            }
        }
    }
    if (typing && onSet != null) {
        if (isTime) TimeDialog(label, editValue ?: value, onDismiss = { typing = false }) { sec -> typing = false; onSet(sec.toDouble()) }
        else NumberDialog(editValue ?: value, onDismiss = { typing = false }, title = label) { v -> typing = false; onSet(v) }
    }
}

/** Type a number (accepts negative for "assisted" bodyweight sets). */
@Composable
fun NumberDialog(initial: String, onDismiss: () -> Unit, title: String? = null, onOk: (Double) -> Unit) {
    // Starts with the whole number selected, so typing replaces it.
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = if (title != null) ({ Text(title) }) else null,
        text = {
            OutlinedTextField(field, { field = it.copy(text = Fmt.cleanDecimal(it.text, allowNegative = true)) },
                Modifier.focusRequester(focus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        },
        confirmButton = { TextButton(onClick = { field.text.replace(',', '.').toDoubleOrNull()?.let(onOk) ?: onDismiss() }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }   // keyboard opens right away
}

/** Type a time: "1:30" = 1 min 30 s, or just seconds ("90"). Gives back seconds. */
@Composable
fun TimeDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (Int) -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(field, { v -> field = v.copy(text = v.text.filter { it.isDigit() || it == ':' }.take(6)) },
                    Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true)
                Muted(stringResource(R.string.time_entry_hint), size = 12)
            }
        },
        confirmButton = { TextButton(onClick = { parseSeconds(field.text)?.let(onOk) ?: onDismiss() }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** "1:30" → 90, "90" → 90, "2:" → 120. Anything else → null. */
fun parseSeconds(t: String): Int? {
    val parts = t.trim().split(":")
    return when (parts.size) {
        1 -> parts[0].toIntOrNull()
        2 -> { val m = parts[0].toIntOrNull() ?: 0; val sec = parts[1].ifEmpty { "0" }.toIntOrNull() ?: return null
               if (sec > 59) null else m * 60 + sec }
        else -> null
    }
}

@Composable
fun Pill(text: String, color: Color = GymColors.current.accent, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Text(text, modifier
        .clip(RoundedCornerShape(50))
        .background(color.copy(alpha = 0.18f))
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .padding(horizontal = 10.dp, vertical = 4.dp),
        color = GymColors.current.text, fontSize = 12.sp)
}

/** Week dots Mon–Sun: filled = trained that day. */
@Composable
fun WeekDots(trained: List<Boolean>, labels: List<String>) {
    val c = GymColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        trained.forEachIndexed { i, t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(if (t) c.accent else c.track),
                    contentAlignment = Alignment.Center) { if (t) Text("✓", color = Color.White, fontSize = 13.sp) }
                Spacer(Modifier.height(2.dp)); Muted(labels.getOrElse(i) { "" }, size = 11)
            }
        }
    }
}

/** Simple line chart. series = list of (values, color). Values are drawn left → right. */
@Composable
fun LineChart(series: List<Pair<List<Double>, Color>>, modifier: Modifier = Modifier.fillMaxWidth().height(160.dp), labels: Pair<String, String>? = null) {
    val c = GymColors.current
    val all = series.flatMap { it.first }
    if (all.size < 2) { Muted("—", modifier.padding(8.dp)); return }
    val min = all.min(); val max = all.max(); val span = (max - min).takeIf { it > 1e-9 } ?: 1.0
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Muted(com.gymcat.app.ui.Fmt.num(max), size = 11); labels?.let { Muted(it.second, size = 11) }
        }
        Canvas(modifier) {
            for (i in 0..3) {                                    // light grid lines
                val y = size.height * i / 3f
                drawLine(c.track, Offset(0f, y), Offset(size.width, y), 1f)
            }
            for ((vals, col) in series) {
                if (vals.size < 2) continue
                val path = Path()
                vals.forEachIndexed { i, v ->
                    val x = size.width * i / (vals.size - 1)
                    val y = size.height * (1f - ((v - min) / span).toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, col, style = Stroke(width = 5f, cap = StrokeCap.Round))
                val lx = size.width; val ly = size.height * (1f - ((vals.last() - min) / span).toFloat())
                drawCircle(col, 8f, Offset(lx, ly))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Muted(com.gymcat.app.ui.Fmt.num(min), size = 11); labels?.let { Muted(it.first, size = 11) }
        }
    }
}

/** Horizontal bars with labels (e.g. sets per muscle this week). */
@Composable
fun BarList(items: List<Pair<String, Double>>, valueText: (Double) -> String) {
    val c = GymColors.current
    val max = items.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    items.forEach { (label, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.width(110.dp), fontSize = 13.sp, color = c.text, maxLines = 1)
            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.track)) {
                Box(Modifier.fillMaxWidth((v / max).toFloat().coerceIn(0.02f, 1f)).height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.accent))
            }
            Text(valueText(v), Modifier.width(52.dp), textAlign = TextAlign.End, fontSize = 12.sp, color = c.text2)
        }
    }
}

/** Vertical bars (e.g. weekly volume). */
@Composable
fun ColumnChart(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier.fillMaxWidth().height(140.dp)) {
    val c = GymColors.current
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column {
        Canvas(modifier) {
            val n = values.size.coerceAtLeast(1)
            val w = size.width / n
            values.forEachIndexed { i, v ->
                val h = size.height * (v / max).toFloat()
                drawRoundRect(c.accent, topLeft = Offset(i * w + w * 0.18f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
            }
        }
        Row(Modifier.fillMaxWidth()) { labels.forEach { Muted(it, Modifier.weight(1f), size = 10) } }
    }
}

/** GPS route line from projected 0..1 points (GeoMath.project). */
@Composable
fun RouteView(points: List<Pair<Float, Float>>, modifier: Modifier = Modifier.fillMaxWidth().height(200.dp)) {
    val c = GymColors.current
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(c.track)) {
        if (points.size < 2) return@Canvas
        val pad = 24f
        val s = minOf(size.width, size.height) - 2 * pad
        val ox = (size.width - s) / 2; val oy = (size.height - s) / 2
        val path = Path()
        points.forEachIndexed { i, (x, y) ->
            val px = ox + x * s; val py = oy + y * s
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, c.accent, style = Stroke(width = 8f, cap = StrokeCap.Round))
        val (sx, sy) = points.first(); val (ex, ey) = points.last()
        drawCircle(c.good, 12f, Offset(ox + sx * s, oy + sy * s))
        drawCircle(c.bad, 12f, Offset(ox + ex * s, oy + ey * s))
    }
}

@Composable
fun EmptyState(catName: String, emoji: String, title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CatArt(catName, emoji, 120.dp)
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = GymColors.current.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Muted(body)
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(GymColors.current.track).padding(12.dp)) {
        Muted(label, size = 12)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GymColors.current.text, style = MaterialTheme.typography.titleMedium)
    }
}
