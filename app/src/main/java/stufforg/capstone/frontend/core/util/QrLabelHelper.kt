package stufforg.capstone.frontend.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import stufforg.capstone.frontend.core.designsystem.LuminaViolet
import stufforg.capstone.frontend.core.designsystem.parseBoxColor
import stufforg.capstone.frontend.core.model.BoxOut
import java.io.File
import java.io.FileOutputStream

object QrLabelHelper {

    private val DigitsRegex = Regex("""(\d+)""")

    /**
     * Parses a Box ID from any supported QR or manual code format:
     * - Raw integer ("4")
     * - Tagged code ("#04", "B-04", "BOX-04", "STUFFORG:BOX:4", "SO-BOX-04")
     * - URL path ending in "/boxes/4" or "/box/4"
     */
    fun parseBoxId(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toIntOrNull()?.let { return it.takeIf { id -> id > 0 } }

        val urlMatch = Regex("""/(?:boxes?|qr)/(\d+)""", RegexOption.IGNORE_CASE)
            .find(trimmed)
        if (urlMatch != null) {
            return urlMatch.groupValues[1].toIntOrNull()?.takeIf { it > 0 }
        }

        return DigitsRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
    }

    fun qrPayloadForBox(box: BoxOut): String = box.id.toString()

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = android.graphics.Color.BLACK,
        backgroundColor: Int = android.graphics.Color.WHITE,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix.get(x, y)) foregroundColor else backgroundColor
            }
        }
        return createBitmap(sizePx, sizePx).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }

    fun renderStickerBitmap(
        box: BoxOut,
        includeSummaryItems: Boolean,
        pureBlackThermal: Boolean,
        showLocationColorStripe: Boolean,
    ): Bitmap {
        val width = 800
        val height = 860
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val accentInt = if (pureBlackThermal) {
            android.graphics.Color.BLACK
        } else {
            parseBoxColor(box.colorCode).toArgb()
        }
        val brandInt = if (pureBlackThermal) {
            android.graphics.Color.BLACK
        } else {
            LuminaViolet.toArgb()
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (pureBlackThermal) android.graphics.Color.BLACK else 0xFFE4E1EE.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 36f, 36f, borderPaint)

        var topY = 56f
        if (showLocationColorStripe) {
            val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentInt
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(40f, 36f, width - 40f, 56f), 10f, 10f, stripePaint)
            topY = 88f
        }

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandInt
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("STUFFORG • BOX ${box.boxTag}", 48f, topY + 20f, headerPaint)

        val qrSize = 440
        val qrBitmap = generateQrBitmap(
            content = qrPayloadForBox(box),
            sizePx = qrSize,
            foregroundColor = if (pureBlackThermal) android.graphics.Color.BLACK else 0xFF1C1A27.toInt(),
        )
        val qrLeft = (width - qrSize) / 2f
        val qrTop = topY + 48f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1A27.toInt()
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val maxTextWidth = width - 96f
        val ellipsizedName = TextUtils.ellipsize(
            box.name,
            titlePaint,
            maxTextWidth,
            TextUtils.TruncateAt.END,
        ).toString()
        val textStartY = qrTop + qrSize + 52f
        canvas.drawText(ellipsizedName, 48f, textStartY, titlePaint)

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (pureBlackThermal) android.graphics.Color.BLACK else 0xFF6B6880.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val locationLine = buildString {
            if (!box.location.isNullOrBlank()) {
                append(box.location)
                append(" • ")
            }
            append("${box.items.size} items (${box.totalUnits} total)")
        }
        val ellipsizedLoc = TextUtils.ellipsize(
            locationLine,
            metaPaint,
            maxTextWidth,
            TextUtils.TruncateAt.END,
        ).toString()
        canvas.drawText(ellipsizedLoc, 48f, textStartY + 42f, metaPaint)

        if (includeSummaryItems && box.items.isNotEmpty()) {
            val itemsSummary = box.items.take(4).joinToString(" • ") { it.name }
            val itemsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentInt
                textSize = 23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val ellipsizedItems = TextUtils.ellipsize(
                itemsSummary,
                itemsPaint,
                maxTextWidth,
                TextUtils.TruncateAt.END,
            ).toString()
            canvas.drawText(ellipsizedItems, 48f, textStartY + 86f, itemsPaint)
        }

        return bitmap
    }

    fun printSticker(context: Context, box: BoxOut, bitmap: Bitmap, copiesHint: Int = 1) {
        val helper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
            colorMode = PrintHelper.COLOR_MODE_COLOR
        }
        val jobName = if (copiesHint > 1) {
            "StuffOrg_Box_${box.id}_x$copiesHint"
        } else {
            "StuffOrg_Box_${box.id}_Label"
        }
        helper.printBitmap(jobName, bitmap)
    }

    fun shareSticker(context: Context, box: BoxOut, bitmap: Bitmap) {
        val file = File(context.cacheDir, "stufforg_box_${box.id}_qr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "StuffOrg QR Label - ${box.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Label"))
    }
}

