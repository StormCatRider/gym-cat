/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/ShareCodec.kt                           ║
║ Turns a routine into a short text code and back, for sharing with  ║
║ friends WITHOUT accounts or servers:                               ║
║   routine → JSON text → compressed (deflate) → Base64 text          ║
║   code = "GYMCAT1:" + that text                                    ║
║ The same code goes inside a .gymcat file or a QR code.             ║
║ Your own custom exercises used by the routine travel with it.      ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data

import android.util.Base64
import com.gymcat.app.data.db.CustomExerciseEntity
import com.gymcat.app.data.db.RoutineEntity
import com.gymcat.app.data.db.RoutineItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object ShareCodec {
    private const val PREFIX = "GYMCAT1:"

    data class Shared(val name: String, val notes: String, val items: List<RoutineItemEntity>, val custom: List<CustomExerciseEntity>)

    fun encode(r: RoutineEntity, items: List<RoutineItemEntity>, custom: List<CustomExerciseEntity>): String {
        val o = JSONObject()
        o.put("v", 1); o.put("name", r.name); o.put("notes", r.notes)
        o.put("items", JSONArray(items.map { i ->
            JSONObject().put("ex", i.exerciseId).put("s", i.sets).put("a", i.repMin).put("b", i.repMax)
                .put("rpe", i.targetRpe ?: 0.0).put("r", i.restSec).put("w", i.warmups).put("ss", i.superset)
                .put("h", i.holdSec).put("m", i.minutes)
        }))
        o.put("custom", JSONArray(custom.map { e ->
            JSONObject().put("id", e.id).put("name", e.name).put("type", e.type).put("eq", e.equipment)
                .put("p", e.primary).put("s", e.secondary).put("mode", e.mode).put("how", e.howTo).put("mis", e.mistakes)
                .put("bw", e.bwPct).put("c", e.compound).put("emo", e.emoji).put("met", e.met)
        }))
        return PREFIX + Base64.encodeToString(deflate(o.toString().toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    /** Returns null if the text isn't a Gym Cat code. */
    fun decode(code: String): Shared? {
        val t = code.trim()
        if (!t.startsWith(PREFIX)) return null
        return runCatching {
        val json = inflate(Base64.decode(t.removePrefix(PREFIX), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            .toString(Charsets.UTF_8)
        val o = JSONObject(json)
        val items = o.getJSONArray("items").let { a ->
            (0 until a.length()).map { k ->
                val i = a.getJSONObject(k)
                RoutineItemEntity(routineId = 0, sortOrder = k, exerciseId = i.getString("ex"), sets = i.optInt("s", 3),
                    repMin = i.optInt("a", 8), repMax = i.optInt("b", 12),
                    targetRpe = i.optDouble("rpe", 0.0).takeIf { it > 0 }, restSec = i.optInt("r", 0),
                    warmups = i.optBoolean("w", false), superset = i.optInt("ss", 0), holdSec = i.optInt("h", 0),
                    minutes = i.optInt("m", 0))
            }
        }
        val custom = o.optJSONArray("custom")?.let { a ->
            (0 until a.length()).map { k ->
                val e = a.getJSONObject(k)
                CustomExerciseEntity(e.getString("id"), e.getString("name"), e.optString("type", "STRENGTH"),
                    e.optString("eq", "OTHER"), e.optString("p"), e.optString("s"), e.optString("mode", "WEIGHT_REPS"),
                    e.optString("how"), e.optString("mis"), e.optDouble("bw", 0.0), e.optBoolean("c", false),
                    e.optString("emo", "🏋️"), e.optDouble("met", 5.0))
            }
        } ?: emptyList()
        Shared(o.optString("name", "Routine"), o.optString("notes", ""), items, custom)
        }.getOrNull()
    }

    private fun deflate(b: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION); d.setInput(b); d.finish()
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end(); return out.toByteArray()
    }

    private fun inflate(b: ByteArray): ByteArray {
        val i = Inflater(); i.setInput(b)
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!i.finished()) {
            val n = i.inflate(buf)
            if (n == 0 && (i.needsInput() || i.needsDictionary())) break
            out.write(buf, 0, n)
            if (out.size() > 1_000_000) break                         // safety: refuse huge payloads
        }
        i.end(); return out.toByteArray()
    }
}
