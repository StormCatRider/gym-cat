/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/QrScreens.kt                      ║
║ Share routines by QR — 100 % on the phone, no internet:            ║
║  • QrShowScreen: turns the routine's share code into a QR picture  ║
║    (ZXing library) for a friend to scan.                           ║
║  • QrScanScreen: opens the camera (asks permission first), reads   ║
║    the QR with ZXing, and imports the routine.                     ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.gymcat.app.R
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.theme.GymColors
import java.util.concurrent.Executors

/** Text → QR picture (black squares on white). */
fun makeQr(text: String, size: Int = 900): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2)
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val px = IntArray(size * size) { i -> if (m[i % size, i / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

@Composable
fun QrShowScreen(vm: MainViewModel, nav: NavHostController, rid: Long) {
    val c = GymColors.current
    val code by produceState<String?>(null, rid) { value = vm.shareCode(rid) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.show_qr), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
        Muted(stringResource(R.string.qr_help))
        val bmp = remember(code) { code?.let { runCatching { makeQr(it) }.getOrNull() } }
        if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = null,
            modifier = Modifier.padding(16.dp).fillMaxWidth().aspectRatio(1f).background(Color.White))
        else Muted(stringResource(R.string.qr_too_big))
        TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.done)) }
    }
}

@Composable
fun QrScanScreen(vm: MainViewModel, nav: NavHostController) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var found by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    val importedFmt = stringResource(R.string.imported_routine)
    val badCode = stringResource(R.string.import_failed)

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.scan_qr), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = GymColors.current.text)
        if (!granted) { Muted(stringResource(R.string.camera_needed)); return@Column }
        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 12.dp)) {
            val executor = remember { Executors.newSingleThreadExecutor() }
            DisposableEffect(Unit) { onDispose { executor.shutdown() } }
            AndroidView(factory = { context ->
                val view = PreviewView(context)
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                    val reader = MultiFormatReader().apply { setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))) }
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) { img: ImageProxy ->
                        val text = decode(reader, img)
                        img.close()
                        if (text != null && !found && text.startsWith("GYMCAT1:")) {
                            found = true
                            ContextCompat.getMainExecutor(context).execute {
                                vm.importCode(text) { name -> vm.toast.value = if (name != null) importedFmt.format(name) else badCode }
                                nav.popBackStack()
                            }
                        }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(context))
                view
            }, modifier = Modifier.fillMaxSize())
        }
        TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.cancel)) }
    }
}

/** Reads the brightness (Y) plane of the camera frame and asks ZXing for a QR code. */
private fun decode(reader: MultiFormatReader, img: ImageProxy): String? {
    val plane = img.planes.firstOrNull() ?: return null
    val buf = plane.buffer; val bytes = ByteArray(buf.remaining()); buf.get(bytes)
    val width = plane.rowStride; val height = img.height
    return runCatching {
        val src = PlanarYUVLuminanceSource(bytes, width, height, 0, 0, img.width, height, false)
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(src))).text
    }.getOrNull().also { reader.reset() }
}
