package com.bajarlist.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.min

/**
 * Hater lekha khatar moto (notebook paper) A4 PDF.
 * Font: assets/fonts/hand.ttf ar hand_bold.ttf (build-er shomoy GitHub Actions download kore).
 * Font na pele normal font diye PDF toiri hoy, app crash kore na.
 */
object PdfExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN_X = 64f
    private const val ROW_H = 34f
    private const val FIRST_Y0 = 132f
    private const val NEXT_Y0 = 60f
    private const val BOTTOM = 62f
    private const val NUM_X = 70f
    private const val NAME_X = 106f
    private const val NAME_MAX_W = 266f
    private const val QTY_X = 394f
    private const val QTY_MAX_W = 104f
    private const val BOX_X = 516f
    private const val BOX_S = 20f

    private val PAPER = 0xFFFFFBEF.toInt()
    private val INK = 0xFF1F3A93.toInt()
    private val PENCIL = 0xFF6B6B6B.toInt()
    private val RULE = 0xFFBBD4EE.toInt()
    private val MARGIN_RED = 0xFFE57373.toInt()
    private val TICK_GREEN = 0xFF2E7D32.toInt()
    private val RED_PEN = 0xFFD84315.toInt()
    private val DOT = 0xFFA0A0A0.toInt()

    fun create(
        context: Context,
        title: String,
        subtitle: String,
        items: List<BazarItem>,
        fileName: String
    ): File {
        val hand = loadFont(context, "fonts/hand.ttf", Typeface.NORMAL)
        val handBold = loadFont(context, "fonts/hand_bold.ttf", Typeface.BOLD)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = handBold; textSize = 32f; color = INK
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = hand; textSize = 18f; color = PENCIL
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = hand; textSize = 22f; color = INK
        }
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = hand; textSize = 16f; color = PENCIL
        }
        val totalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = handBold; textSize = 21f; color = INK
        }
        val footPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = hand; textSize = 13f; color = PENCIL
        }

        val paperPaint = Paint().apply { color = PAPER }
        val rulePaint = Paint().apply { color = RULE; strokeWidth = 1f }
        val marginPaint = Paint().apply { color = MARGIN_RED; strokeWidth = 1.4f }
        val holeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEDE7D3.toInt() }
        val holeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.2f; color = 0xFFCFC6A8.toInt()
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DOT }
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.8f; color = INK
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = TICK_GREEN
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val penLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2.4f; color = RED_PEN
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }

        // Random fixed seed: protibar ekoi list-e ekoi "haater lekha" hoy
        val rnd = Random(2024)

        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var canvas = page.canvas
        var y0 = FIRST_Y0

        fun drawPaper() {
            canvas.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), paperPaint)
            var ly = y0
            while (ly < PAGE_H - BOTTOM + 4f) {
                canvas.drawLine(0f, ly, PAGE_W.toFloat(), ly, rulePaint)
                ly += ROW_H
            }
            canvas.drawLine(MARGIN_X, 0f, MARGIN_X, PAGE_H.toFloat(), marginPaint)
            for (hy in floatArrayOf(120f, 421f, 722f)) {
                canvas.drawCircle(26f, hy, 9f, holeFill)
                canvas.drawCircle(26f, hy, 9f, holeStroke)
            }
        }

        fun footer() {
            canvas.drawText(
                "বাজার লিস্ট • পৃষ্ঠা $pageNo".bn(),
                MARGIN_X + 14f, PAGE_H - 26f, footPaint
            )
        }

        fun newPage() {
            footer()
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y0 = NEXT_Y0
            drawPaper()
        }

        // ---- prothom page: shirshak ----
        drawPaper()
        canvas.save()
        canvas.rotate(-1.2f, MARGIN_X + 16f, 62f)
        canvas.drawText(title, MARGIN_X + 16f, 62f, titlePaint)
        wobbleLine(
            canvas, MARGIN_X + 14f, 74f,
            MARGIN_X + 22f + titlePaint.measureText(title), rnd, 2.0f, penLine
        )
        canvas.restore()
        canvas.drawText(subtitle, MARGIN_X + 16f, 104f, subPaint)

        // ---- item gulo ----
        var rowTop = y0
        items.forEachIndexed { index, item ->
            if (rowTop + ROW_H > PAGE_H - BOTTOM) {
                newPage()
                rowTop = y0
            }
            val base = rowTop + ROW_H - 10f
            val rot = (rnd.nextFloat() - 0.5f) * 0.7f
            val dy = (rnd.nextFloat() - 0.5f) * 2f

            canvas.save()
            canvas.rotate(rot, NAME_X, base)

            canvas.drawText((index + 1).toString().bn() + ".", NUM_X, base + dy, numPaint)

            val nameText = fitText(textPaint, item.name, NAME_MAX_W, 22f, 14f)
            val nameW = textPaint.measureText(nameText)
            canvas.drawText(nameText, NAME_X, base + dy, textPaint)

            // nam theke poriman porjonto dotted line
            var dx = NAME_X + nameW + 8f
            val endX = QTY_X - 8f
            while (dx < endX) {
                canvas.drawCircle(dx, base - 3f + dy, 0.9f, dotPaint)
                dx += 6f
            }

            val qtyFull = (formatQty(item.quantity) + " " + item.unit).bn()
            val qtyText = fitText(textPaint, qtyFull, QTY_MAX_W, 22f, 14f)
            canvas.drawText(qtyText, QTY_X, base + dy, textPaint)

            canvas.restore()

            val by = rowTop + (ROW_H - BOX_S) / 2f - 1f
            handBox(canvas, BOX_X, by, BOX_S, rnd, boxPaint)
            if (item.bought) handTick(canvas, BOX_X, by, BOX_S, tickPaint)

            rowTop += ROW_H
        }

        // ---- mot item ----
        if (rowTop + ROW_H > PAGE_H - BOTTOM) {
            newPage()
            rowTop = y0
        }
        val totalBase = rowTop + ROW_H - 10f
        val totalText = "মোট আইটেম: ${items.size}".bn()
        canvas.drawText(totalText, NAME_X, totalBase, totalPaint)
        wobbleLine(
            canvas, NAME_X, totalBase + 5f,
            NAME_X + totalPaint.measureText(totalText), rnd, 1.6f, penLine
        )

        footer()
        doc.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun loadFont(context: Context, path: String, fallbackStyle: Int): Typeface =
        try {
            Typeface.createFromAsset(context.assets, path)
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, fallbackStyle)
        }

    /** Text boro hole chhoto kore fit kore; tao na hole ... diye kete dey. */
    private fun fitText(
        paint: TextPaint,
        text: String,
        maxW: Float,
        startSize: Float,
        minSize: Float
    ): String {
        paint.textSize = startSize
        while (paint.measureText(text) > maxW && paint.textSize > minSize) {
            paint.textSize -= 0.5f
        }
        return if (paint.measureText(text) > maxW) {
            TextUtils.ellipsize(text, paint, maxW, TextUtils.TruncateAt.END).toString()
        } else {
            text
        }
    }

    /** Kalam diye tana ektu ebor-kheabor line. */
    private fun wobbleLine(
        canvas: Canvas,
        x1: Float,
        y: Float,
        x2: Float,
        rnd: Random,
        amp: Float,
        paint: Paint
    ) {
        val p = Path()
        p.moveTo(x1, y)
        var x = x1
        while (x < x2) {
            val nx = min(x + 16f, x2)
            p.quadTo(
                (x + nx) / 2f, y + (rnd.nextFloat() * 2f - 1f) * amp,
                nx, y + (rnd.nextFloat() * 2f - 1f) * amp * 0.5f
            )
            x = nx
        }
        canvas.drawPath(p, paint)
    }

    /** Haate aanka chhoto box. */
    private fun handBox(canvas: Canvas, x: Float, y: Float, s: Float, rnd: Random, paint: Paint) {
        fun j() = rnd.nextFloat() * 1.6f - 0.8f
        val p = Path()
        p.moveTo(x + j(), y + j())
        p.lineTo(x + s + j(), y + j())
        p.lineTo(x + s + j(), y + s + j())
        p.lineTo(x + j(), y + s + j())
        p.lineTo(x + j(), y + 1.5f + j())
        canvas.drawPath(p, paint)
    }

    /** Haate aanka tick; box-er baire ektu beriye jay. */
    private fun handTick(canvas: Canvas, x: Float, y: Float, s: Float, paint: Paint) {
        val p = Path()
        p.moveTo(x + 3f, y + s * 0.55f)
        p.lineTo(x + s * 0.42f, y + s - 2f)
        p.lineTo(x + s + 5f, y - 6f)
        canvas.drawPath(p, paint)
    }
}
