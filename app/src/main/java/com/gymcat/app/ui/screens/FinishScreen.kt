/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/FinishScreen.kt                   ║
║ "Workout finished" screen (your choices): summary stats, records   ║
║ (PRs), new badges, skill-ladder level-up offers ("you hit 3 × 12 —  ║
║ move to the next step?"), today's protein from Health Connect, and ║
║ the Share button that makes an image card with the cat.            ║
║ ShareCard.render() draws that image with plain Android drawing.    ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavHostController
import com.gymcat.app.R
import com.gymcat.app.core.calc.Badges
import com.gymcat.app.core.calc.StrengthMath
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.SetType
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.data.FinishResult
import com.gymcat.app.data.db.SetEntity
import com.gymcat.app.data.db.WorkoutEntity
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.Cat
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.StatBox
import com.gymcat.app.ui.theme.GymColorSet
import com.gymcat.app.ui.theme.GymColors
import java.io.File

@Composable
fun FinishScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val r by vm.lastFinish.collectAsState()
    val s by vm.settings.collectAsState()
    val result = r
    if (result == null) { androidx.compose.runtime.LaunchedEffect(Unit) { nav.popBackStack() }; return }
    val workout by produceState<WorkoutEntity?>(null, result.workoutId) { value = vm.repo.dao.workout(result.workoutId) }
    val sets by produceState(emptyList<SetEntity>(), result.workoutId) { value = vm.repo.dao.setsOfNow(result.workoutId) }
    val handled = remember { mutableStateListOf<String>() }
    val es = vm.es

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        CatArt(if (result.prs.isNotEmpty()) Cat.PR else Cat.FINISHED, if (result.prs.isNotEmpty()) "🏆" else "🐱", 140.dp)
        Text(stringResource(R.string.workout_done), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.text)
        workout?.let { Muted(it.title) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(stringResource(R.string.duration), Fmt.duration((result.minutes * 60).toLong()), Modifier.weight(1f))
            StatBox(stringResource(R.string.volume), Fmt.weight(result.volumeLb, s.metric), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(stringResource(R.string.sets), result.setsDone.toString(), Modifier.weight(1f))
            StatBox(stringResource(R.string.calories), Fmt.kcal(result.kcal), Modifier.weight(1f))
        }
        result.proteinG?.let { GCard { Text("🥚 " + stringResource(R.string.protein_today, it.toInt()), color = c.text) } }

        if (result.prs.isNotEmpty()) GCard {
            Text(stringResource(R.string.new_records), fontWeight = FontWeight.SemiBold, color = c.text)
            result.prs.forEach { pr -> Text("🏆 " + prText(vm, pr.exerciseId, pr.kind, pr.value, s.metric), color = c.text) }
        }
        if (result.newBadges.isNotEmpty()) GCard {
            Text(stringResource(R.string.new_badges), fontWeight = FontWeight.SemiBold, color = c.text)
            result.newBadges.mapNotNull { Badges.byId(it) }.forEach { b ->
                Row(verticalAlignment = Alignment.CenterVertically) { CatArt(Cat.BADGE, b.emoji, 36.dp); Text(b.title(es), color = c.text) }
            }
        }
        result.ladderUps.filter { it.first !in handled }.forEach { (ladderId, next) ->
            val l = Programs.ladder(ladderId) ?: return@forEach
            GCard {
                Text(stringResource(R.string.level_up_title, l.name(es)), fontWeight = FontWeight.SemiBold, color = c.text)
                Muted(stringResource(R.string.level_up_body, vm.exName(l.steps[next].exerciseId)))
                Row {
                    TextButton(onClick = { vm.levelUp(ladderId, next); handled += ladderId }) { Text(stringResource(R.string.level_up_yes)) }
                    TextButton(onClick = { handled += ladderId }) { Text(stringResource(R.string.not_yet)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                workout?.let { w -> ShareCard.share(ctx, ShareCard.render(ctx, c, vm, w, sets, result, s.metric)) }
            }) { Text("📤 " + stringResource(R.string.share)) }
            Button(onClick = { vm.lastFinish.value = null; nav.navigate("home") { popUpTo("home") { inclusive = true } } }) {
                Text(stringResource(R.string.done))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun prText(vm: MainViewModel, exId: String, kind: String, v: Double, metric: Boolean): String {
    val name = vm.exName(exId)
    return when (kind) {
        "weight" -> stringResource(R.string.pr_weight, name, Fmt.weight(v, metric))
        "e1rm" -> stringResource(R.string.pr_e1rm, name, Fmt.weight(v, metric))
        "reps" -> stringResource(R.string.pr_reps, name, v.toInt())
        "hold" -> stringResource(R.string.pr_hold, name, Fmt.duration(v.toLong()))
        "distance" -> stringResource(R.string.pr_distance, name, Fmt.dist(v, metric))
        else -> stringResource(R.string.pr_pace, name, Fmt.pace(v, metric))
    }
}

/** Draws the share image (1080 × 1350 px) with the cat, stats, top sets and records. Weights are shown. */
object ShareCard {
    @SuppressLint("DiscouragedApi")
    fun render(ctx: Context, c: GymColorSet, vm: MainViewModel, w: WorkoutEntity, sets: List<SetEntity>, r: FinishResult, metric: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawColor(c.bg.toArgb())
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.card.toArgb() }
        cv.drawRoundRect(RectF(48f, 48f, 1032f, 1302f), 72f, 72f, card)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.text.toArgb(); textSize = 64f; typeface = Typeface.DEFAULT_BOLD }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.text.toArgb(); textSize = 40f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.text2.toArgb(); textSize = 34f }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.accent.toArgb(); textSize = 44f; typeface = Typeface.DEFAULT_BOLD }

        // Cat drawing (your PNG if added) or emoji
        val id = ctx.resources.getIdentifier(Cat.FINISHED, "drawable", ctx.packageName)
        val d = if (id != 0) ResourcesCompat.getDrawable(ctx.resources, id, null) else null
        if (d != null) {
            // Dark mode: light rounded tile behind the cat so its navy outlines stay visible.
            if (c.dark) cv.drawRoundRect(RectF(745f, 75f, 1005f, 335f), 56f, 56f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(0xE6, 0xF1, 0xFB) })
            d.setBounds(760, 90, 990, 320); d.draw(cv)
        }
        else cv.drawText("🐱", 820f, 260f, Paint().apply { textSize = 160f })

        cv.drawText(ctx.getString(R.string.app_name), 110f, 170f, accent)
        cv.drawText(w.title.take(26), 110f, 250f, title)
        val date = java.time.LocalDate.ofEpochDay(w.date).toString()
        cv.drawText(date, 110f, 310f, muted)

        var y = 420f
        val stats = listOf(
            ctx.getString(R.string.duration) to Fmt.duration((r.minutes * 60).toLong()),
            ctx.getString(R.string.volume) to Fmt.weight(r.volumeLb, metric),
            ctx.getString(R.string.sets) to r.setsDone.toString(),
            ctx.getString(R.string.calories) to Fmt.kcal(r.kcal),
        )
        stats.forEachIndexed { i, (k, v) ->
            val x = 110f + (i % 2) * 450f
            if (i == 2) y += 130f
            cv.drawText(k, x, y, muted); cv.drawText(v, x, y + 56f, accent)
        }
        y += 170f
        // Top set of each exercise (heaviest by Epley)
        sets.filter { it.type != SetType.WARMUP.name }.groupBy { it.exerciseOrder }.toSortedMap().values.take(8).forEach { g ->
            val e = vm.ex(g.first().exerciseId)
            val top = g.maxByOrNull { StrengthMath.epley(it.lb, it.reps) + it.seconds + it.miles * 100 } ?: return@forEach
            val txt = when (e?.mode) {
                LogMode.WEIGHT_REPS -> "${Fmt.weight(top.lb, metric)} × ${top.reps}"
                LogMode.BODYWEIGHT_REPS -> "${top.reps} reps"
                LogMode.CARDIO -> Fmt.dist(top.miles, metric) + " · " + Fmt.duration(top.seconds.toLong())
                else -> Fmt.duration(top.seconds.toLong())
            }
            cv.drawText((e?.name(vm.es) ?: "").take(30), 110f, y, body)
            cv.drawText(txt, 700f, y, body)
            y += 60f
        }
        if (r.prs.isNotEmpty() || r.newBadges.isNotEmpty()) {
            y += 20f
            cv.drawText("🏆 ${r.prs.size}   🏅 ${r.newBadges.size}", 110f, y, accent)
        }
        return bmp
    }

    fun share(ctx: Context, bmp: Bitmap) {
        val f = File(ctx.cacheDir, "gymcat-share.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
    }
}
