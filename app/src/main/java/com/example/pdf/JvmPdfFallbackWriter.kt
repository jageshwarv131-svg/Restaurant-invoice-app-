package com.example.pdf

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Pure-JVM multi-page PDF 1.4 fallback used only when Android's native Skia PdfDocument
 * is unavailable (e.g. Robolectric JVM unit tests). On real Android devices,
 * android.graphics.pdf.PdfDocument always handles rendering directly.
 */
internal object JvmPdfFallbackWriter {
  fun writeTextPdf(file: File, lines: List<String>, pageWidth: Int = 595, pageHeight: Int = 842) {
    val linesPerPage = 48
    val sanitized =
      lines.map { line ->
        line
          .replace("\\", "\\\\")
          .replace("(", "\\(")
          .replace(")", "\\)")
          .filter { it.code in 32..126 }
      }
    val chunks = if (sanitized.isEmpty()) listOf(listOf("")) else sanitized.chunked(linesPerPage)

    val out = ByteArrayOutputStream()
    val offsets = mutableListOf<Int>()
    fun writeStr(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

    writeStr("%PDF-1.4\n")

    // Obj 1: Catalog, Obj 2: Pages, Obj 3: Font
    // Page i (0-based): pageObjId = 4 + i * 2, contentObjId = 5 + i * 2
    val pageCount = chunks.size
    val fontObjId = 3
    val pageObjIds = (0 until pageCount).map { 4 + it * 2 }

    offsets.add(out.size())
    writeStr("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

    offsets.add(out.size())
    val kidsStr = pageObjIds.joinToString(" ") { "$it 0 R" }
    writeStr("2 0 obj\n<< /Type /Pages /Kids [$kidsStr] /Count $pageCount >>\nendobj\n")

    offsets.add(out.size())
    writeStr("$fontObjId 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

    val startY = pageHeight - 40
    chunks.forEachIndexed { pageIndex, pageLines ->
      val pageObjId = 4 + pageIndex * 2
      val contentObjId = 5 + pageIndex * 2

      val streamBuilder = StringBuilder("BT\n/F1 10 Tf\n36 $startY Td\n14 TL\n")
      pageLines.forEachIndexed { idx, txt ->
        if (idx == 0) streamBuilder.append("($txt) Tj\n")
        else streamBuilder.append("T* ($txt) Tj\n")
      }
      streamBuilder.append("ET\n")
      val contentBytes = streamBuilder.toString().toByteArray(Charsets.US_ASCII)

      offsets.add(out.size())
      writeStr(
        "$pageObjId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] /Contents $contentObjId 0 R /Resources << /Font << /F1 $fontObjId 0 R >> >> >>\nendobj\n"
      )

      offsets.add(out.size())
      writeStr("$contentObjId 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n")
      out.write(contentBytes)
      writeStr("\nendstream\nendobj\n")
    }

    val xrefStart = out.size()
    writeStr("xref\n0 ${offsets.size + 1}\n")
    writeStr("0000000000 65535 f \n")
    offsets.forEach { off -> writeStr("%010d 00000 n \n".format(off)) }
    writeStr("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")

    file.parentFile?.mkdirs()
    FileOutputStream(file).use { it.write(out.toByteArray()) }
  }
}
