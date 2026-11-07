/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/csv/Csv.kt                              ║
║ Reads and writes CSV text (spreadsheet-style rows), same style as  ║
║ Gym & Ñam backups: UTF-8 with BOM, "\r\n" line endings, commas.    ║
║ Text containing a comma, quote or line break is wrapped in quotes, ║
║ and quotes inside are doubled:  He said "hi"  →  "He said ""hi"""  ║
║ Dates are written as YYYY-MM-DD (converted from epochDay).         ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.csv

object Csv {
    const val BOM = "﻿"

    fun escape(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\""
        else v

    /** rows → full file text (BOM + header + rows, CRLF). */
    fun write(header: List<String>, rows: List<List<String>>): String =
        BOM + (listOf(header) + rows).joinToString("\r\n") { r -> r.joinToString(",") { escape(it) } } + "\r\n"

    /** file text → rows (first row = header). Handles quotes, CRLF and BOM. */
    fun parse(text: String): List<List<String>> {
        val t = text.removePrefix(BOM)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < t.length) {
            val c = t[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < t.length && t[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false
                } else cell.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row += cell.toString(); cell.clear() }
                '\r' -> {}
                '\n' -> { row += cell.toString(); cell.clear(); rows += row; row = mutableListOf() }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        return rows
    }

    // ── Dates. epochDay ↔ "YYYY-MM-DD" using the civil-calendar algorithm
    //    (Howard Hinnant, "days_from_civil"), so no Android/Java date classes are needed.
    // Example: epochDay 0 → "1970-01-01"; epochDay 20_397 → "2025-11-05"
    fun dateOf(epochDay: Long): String {
        val z = epochDay + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val year = if (m <= 2) y + 1 else y
        return "$year-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    fun epochDayOf(date: String): Long {
        val p = date.trim().split('-')
        var y = p[0].toLong(); val m = p[1].toLong(); val d = p[2].toLong()
        if (m <= 2) y -= 1
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = if (m > 2) m - 3 else m + 9
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }
}
