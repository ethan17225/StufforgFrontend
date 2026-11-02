package stufforg.capstone.frontend.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaHelper {

    fun createTempCameraCapture(context: Context): Pair<File, Uri> {
        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return file to uri
    }

    suspend fun copyUriToTempFile(context: Context, uri: Uri): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val ext = when {
                    "png" in mime -> "png"
                    "webp" in mime -> "webp"
                    else -> "jpg"
                }
                val dest = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@runCatching null
                dest.takeIf { it.length() > 0L }
            }.getOrNull()
        }

    fun startQrCodeScan(
        context: Context,
        onQrValue: (String) -> Unit,
        onCancelled: () -> Unit = {},
        onError: (String) -> Unit,
    ) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue
                if (!raw.isNullOrBlank()) {
                    onQrValue(raw)
                } else {
                    onError("Scanned QR code had no readable text")
                }
            }
            .addOnCanceledListener { onCancelled() }
            .addOnFailureListener { err ->
                onError(
                    err.localizedMessage
                        ?: "Google Code Scanner unavailable on this device. Use manual Box ID lookup below.",
                )
            }
    }
}

