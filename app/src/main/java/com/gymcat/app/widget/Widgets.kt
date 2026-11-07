/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — widget/Widgets.kt                            ║
║ Two home-screen widgets (Glance library), your choice "both":      ║
║  • Small  (2×2): Gym Cat + 🔥 streak weeks + days this week.        ║
║  • Medium (4×2): next workout name + Start button (opens Gym Cat   ║
║    and starts it).                                                 ║
║ Widgets.refresh() redraws them after each workout.                 ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gymcat.app.GymCatApp
import com.gymcat.app.MainActivity
import com.gymcat.app.R
import com.gymcat.app.core.calc.StreakMath
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.data.today
import java.util.Locale

private val BG = ColorProvider(Color(0xFFF1F7FD))
private val ACCENT = ColorProvider(Color(0xFF4A9BE0))
private val TEXT = ColorProvider(Color(0xFF2B2A33))
private val TEXT2 = ColorProvider(Color(0xFF77757F))

const val ACTION_START_NEXT = "com.gymcat.app.START_NEXT"

@SuppressLint("DiscouragedApi")
private fun catRes(c: Context, name: String) = c.resources.getIdentifier(name, "drawable", c.packageName)

class SmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as GymCatApp
        val s = app.repo.settings.current()
        val st = StreakMath.compute(app.repo.dao.finishedWorkoutsNow().map { it.date }, s.weeklyGoal, today())
        val cat = catRes(context, "cat_08_streak")
        val open = Intent(context, MainActivity::class.java)
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(BG).cornerRadius(24.dp).padding(12.dp).clickable(actionStartActivity(open)),
                horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                if (cat != 0) Image(ImageProvider(cat), contentDescription = null, modifier = GlanceModifier.size(56.dp))
                else Text("🐱", style = TextStyle(fontSize = 36.sp))
                Text("🔥 ${st.streakWeeks}", style = TextStyle(color = ACCENT, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                Text(context.getString(R.string.days_of_goal, st.thisWeekDays, st.goal), style = TextStyle(color = TEXT2, fontSize = 12.sp))
            }
        }
    }
}

class MediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as GymCatApp
        val es = Locale.getDefault().language == "es"
        val prog = app.repo.dao.activeProgramNow()
        val p = prog?.let { Programs.byId(it.programId) }
        val next = when {
            p != null -> p.name(es) + " · " + p.days[prog.nextDay % p.days.size].let { if (es) it.nameEs else it.nameEn }
            else -> app.repo.dao.routinesNow().firstOrNull()?.name ?: context.getString(R.string.free_workout)
        }
        val cat = catRes(context, "cat_06_home")
        val start = Intent(context, MainActivity::class.java).setAction(ACTION_START_NEXT)
        provideContent {
            Row(GlanceModifier.fillMaxSize().background(BG).cornerRadius(24.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cat != 0) Image(ImageProvider(cat), contentDescription = null, modifier = GlanceModifier.size(64.dp))
                else Text("🐱", style = TextStyle(fontSize = 40.sp))
                Column(GlanceModifier.defaultWeight().padding(horizontal = 10.dp)) {
                    Text(context.getString(R.string.next_workout), style = TextStyle(color = TEXT2, fontSize = 12.sp))
                    Text(next, style = TextStyle(color = TEXT, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                }
                Button(context.getString(R.string.start), onClick = actionStartActivity(start))
            }
        }
    }
}

class SmallWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = SmallWidget() }
class MediumWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MediumWidget() }

object Widgets {
    suspend fun refresh(c: Context) {
        runCatching { SmallWidget().updateAll(c); MediumWidget().updateAll(c) }
    }
}

