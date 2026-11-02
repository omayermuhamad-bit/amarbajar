package com.bajarlist.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f
    private const val ROW_H = 30f
    private const val QTY_X = 360f

    /** A4 PDF banay. File ta app-er cache folder e thake (share korar jonno). */
    fun create(
        context: Context,
        title: String,
        subtitle: String,
        items: List<BazarItem>,
        fileName: String
    ): File {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f; isFakeBoldText = true; color = 0xFF1B5E20.toInt()
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; color = 0xFF616161.toInt()
        }
        val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; isFakeBoldText = true; color = Color.WHITE
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f; color = 0xFF212121.toInt()
        }
        val footPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; color = 0xFF757575.toInt()
        }
        val headBg = Paint().apply { color = 0xFF2E7D32.toInt() }
        val zebra = Paint().apply { color = 0xFFF1F8E9.toInt() }
        val line = Paint().apply { color = 0xFFC8E6C9.toInt(); strokeWidth = 1f }
        val box = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f; color = 0xFF424242.toInt()
        }
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2.4f
            color = 0xFF2E7D32.toInt(); strokeCap = Paint.Cap.ROUND
        }

        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var canvas = page.canvas

        fun tableHeader(y: Float) {
            canvas.drawRect(LEFT, y, RIGHT, y + ROW_H, headBg)
            canvas.drawText("ক্রম", LEFT + 10f, y + 20f, headPaint)
            canvas.drawText("আইটেম", LEFT + 60f, y + 20f, headPaint)
            canvas.drawText("পরিমাণ", QTY_X, y + 20f, headPaint)
            canvas.drawText("কেনা", RIGHT - 48f, y + 20f, headPaint)
        }

        fun footer() {
            canvas.drawText("বাজার লিস্ট • পৃষ্ঠা $pageNo".bn(), LEFT, PAGE_H - 25f, footPaint)
        }

        canvas.drawText(title, LEFT, 60f, titlePaint)
        canvas.drawText(subtitle, LEFT, 84f, subPaint)
        var y = 104f
        tableHeader(y)
        y += ROW_H

        items.forEachIndexed { index, item ->
            if (y + ROW_H > PAGE_H - 70f) {
                footer()
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                canvas = page.canvas
                y = 50f
                tableHeader(y)
                y += ROW_H
            }
            if (index % 2 == 1) canvas.drawRect(LEFT, y, RIGHT, y + ROW_H, zebra)

            canvas.drawText((index + 1).toString().bn(), LEFT + 14f, y + 20f, textPaint)

            val nameText = TextUtils.ellipsize(item.name, textPaint, 290f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(nameText, LEFT + 60f, y + 20f, textPaint)

            val qtyFull = (formatQty(item.quantity) + " " + item.unit).bn()
            val qtyText = TextUtils.ellipsize(qtyFull, textPaint, 120f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(qtyText, QTY_X, y + 20f, textPaint)

            val bx = RIGHT - 44f
            val by = y + 6f
            canvas.drawRect(bx, by, bx + 18f, by + 18f, box)
            if (item.bought) {
                canvas.drawLine(bx + 3.5f, by + 9.5f, bx + 7.5f, by + 14f, tick)
                canvas.drawLine(bx + 7.5f, by + 14f, bx + 15f, by + 4f, tick)
            }
            canvas.drawLine(LEFT, y + ROW_H, RIGHT, y + ROW_H, line)
            y += ROW_H
        }

        y += 22f
        canvas.drawText("মোট আইটেম: ${items.size}".bn(), LEFT, y, subPaint)
        footer()
        doc.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
