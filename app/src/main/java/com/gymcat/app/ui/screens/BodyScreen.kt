/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/BodyScreen.kt                     ║
║ Body tab (your choices):                                           ║
║  • Muscle map — front + back, colored by this week's sets.         ║
║  • Measurements — neck, chest, arm, waist, hip, thigh (inches),    ║
║    latest + change vs ~1 month ago, and the U.S. Navy body-fat     ║
║    estimate (formula in core/calc/BodyMath.kt).                    ║
║  • Progress photos — camera or gallery, saved ONLY inside the app  ║
║    (private folder), side-by-side compare. Included in backups.    ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.gymcat.app.GymCatApp
import com.gymcat.app.R
import com.gymcat.app.core.calc.BodyMath
import com.gymcat.app.core.model.Sex
import com.gymcat.app.data.db.MeasureEntity
import com.gymcat.app.data.db.PhotoEntity
import com.gymcat.app.data.today
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.MuscleMap
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.theme.GymColors
import java.io.File
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun BodyScreen(vm: MainViewModel) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val muscles by vm.muscleSetsThisWeek.collectAsState()
    val measures by vm.measures.collectAsState()
    val photos by vm.photos.collectAsState()
    var addMeasure by remember { mutableStateOf(false) }
    val compare = remember { mutableStateListOf<PhotoEntity>() }
    val photoDir = (ctx.applicationContext as GymCatApp).backup.photoDir
    var pendingFile by remember { mutableStateOf<File?>(null) }

    // Camera → file in the app's private photos folder.
    val app = ctx.applicationContext as GymCatApp
    // Resizing a 12-megapixel photo takes ~1 s, so it runs in the background (screen stays smooth).
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingFile
        pendingFile = null
        if (ok && f != null) app.appScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            shrink(f); vm.addPhoto(PhotoEntity(date = today(), fileName = f.name))
        } else f?.delete()
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingFile?.let { takePicture.launch(FileProvider.getUriForFile(ctx, ctx.packageName + ".files", it)) }
    }
    // Gallery → copy into the private folder (the original stays where it was).
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) app.appScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val f = File(photoDir, "p_${System.currentTimeMillis()}.jpg")
            runCatching { ctx.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } } }
            if (f.exists() && f.length() > 0) { shrink(f); vm.addPhoto(PhotoEntity(date = today(), fileName = f.name)) }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tab_body), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.text)

        GCard {
            Text(stringResource(R.string.muscle_map), fontWeight = FontWeight.SemiBold, color = c.text)
            Muted(stringResource(R.string.muscle_map_hint))
            MuscleMap(muscles, stringResource(R.string.front), stringResource(R.string.back_view))
        }

        GCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.measurements), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
                TextButton(onClick = { addMeasure = true }) { Text("＋ " + stringResource(R.string.add)) }
            }
            val latest = measures.firstOrNull()
            if (latest == null) Muted(stringResource(R.string.no_measurements))
            else {
                val old = measures.firstOrNull { it.date <= latest.date - 28 }
                val fields = listOf(
                    R.string.m_neck to { m: MeasureEntity -> m.neck }, R.string.m_chest to { m: MeasureEntity -> m.chest },
                    R.string.m_arm to { m: MeasureEntity -> m.arm }, R.string.m_waist to { m: MeasureEntity -> m.waist },
                    R.string.m_hip to { m: MeasureEntity -> m.hip }, R.string.m_thigh to { m: MeasureEntity -> m.thigh },
                )
                Muted(LocalDate.ofEpochDay(latest.date).toString())
                fields.forEach { (label, get) ->
                    val now = get(latest) ?: return@forEach
                    val diff = old?.let(get)?.let { now - it }
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(stringResource(label), Modifier.weight(1f), color = c.text)
                        Text(Fmt.lengthNum(now, s.metric) + " " + Fmt.lengthUnit(s.metric), color = c.text)
                        if (diff != null) Text("  " + (if (diff >= 0) "+" else "") + Fmt.lengthNum(diff, s.metric), color = c.text2)
                    }
                }
                latest.bodyFat?.let {
                    Text(stringResource(R.string.body_fat_navy, Fmt.num(it)), fontWeight = FontWeight.SemiBold, color = c.text)
                    Muted(stringResource(R.string.body_fat_note), size = 11)
                }
            }
        }

        GCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.progress_photos), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
            }
            Muted(stringResource(R.string.photos_private))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val f = File(photoDir, "p_${System.currentTimeMillis()}.jpg"); pendingFile = f
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        takePicture.launch(FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f))
                    else camPerm.launch(Manifest.permission.CAMERA)
                }) { Text("📷 " + stringResource(R.string.camera)) }
                OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("🖼 " + stringResource(R.string.gallery))
                }
            }
            if (compare.size == 2) {
                Muted(stringResource(R.string.compare))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    compare.sortedBy { it.date }.forEach { p ->
                        Column(Modifier.weight(1f)) {
                            PhotoThumb(File(photoDir, p.fileName), Modifier.fillMaxWidth().aspectRatio(0.75f))
                            Muted(LocalDate.ofEpochDay(p.date).toString(), size = 11)
                        }
                    }
                }
                TextButton(onClick = { compare.clear() }) { Text(stringResource(R.string.clear)) }
            } else if (photos.isNotEmpty()) Muted(stringResource(R.string.tap_two_to_compare), size = 12)
            photos.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    row.forEach { p ->
                        val selected = p in compare
                        PhotoThumb(File(photoDir, p.fileName), Modifier.weight(1f).aspectRatio(0.75f)
                            .let { if (selected) it.border(3.dp, c.accent, RoundedCornerShape(14.dp)) else it }
                            .clickable { if (selected) compare.remove(p) else { if (compare.size == 2) compare.removeAt(0); compare.add(p) } })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (compare.size == 1) TextButton(onClick = { vm.deletePhoto(compare.first()); compare.clear() }) {
                Text(stringResource(R.string.delete_photo), color = c.bad)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (addMeasure) MeasureDialog(vm, s.metric, onDismiss = { addMeasure = false })
}

/** Resizes to max 1600 px and re-saves as JPEG 85 % so backups stay small. */
private fun shrink(f: File) {
    runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, opts)
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / sample > 1600) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bmp.recycle()
    }
}

@Composable
private fun PhotoThumb(f: File, modifier: Modifier) {
    val bmp = remember(f.path) {
        runCatching { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }.getOrNull()
    }
    if (bmp != null) Image(bmp, null, modifier.clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
    else Muted("—", modifier)
}

@Composable
private fun MeasureDialog(vm: MainViewModel, metric: Boolean, onDismiss: () -> Unit) {
    val s by vm.settings.collectAsState()
    val labels = listOf(R.string.m_neck, R.string.m_chest, R.string.m_arm, R.string.m_waist, R.string.m_hip, R.string.m_thigh)
    val values = remember { mutableStateListOf("", "", "", "", "", "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measurements) + " (" + Fmt.lengthUnit(metric) + ")") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                labels.forEachIndexed { i, l ->
                    OutlinedTextField(values[i], { values[i] = Fmt.cleanDecimal(it, maxLen = 5) },
                        label = { Text(stringResource(l)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Muted(stringResource(R.string.body_fat_needs))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = values.map { it.toDoubleOrNull()?.let { x -> Fmt.toIn(x, metric) } }
                // Navy body fat needs neck + waist (+ hip for women) and your height (Settings).
                val bf = if (v[0] != null && v[3] != null)
                    BodyMath.navyBodyFat(if (s.sex == "FEMALE") Sex.FEMALE else Sex.MALE, s.heightIn, v[0]!!, v[3]!!, v[4]) else null
                if (v.any { it != null }) vm.addMeasure(MeasureEntity(date = today(), neck = v[0], chest = v[1], arm = v[2],
                    waist = v[3], hip = v[4], thigh = v[5], bodyFat = bf))
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
