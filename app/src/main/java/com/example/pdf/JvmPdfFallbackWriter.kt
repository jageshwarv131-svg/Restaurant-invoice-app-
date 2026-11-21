package com.example.pdf

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Pure-JVM PDF 1.4 fallback used only when Android's native Skia PdfDocument
 * is unavailable (e.g. Robolectric JVM unit tests). On real Android devices,
 * android.graphics.pdf.PdfDocument always handles rendering directly.
 */
internal object JvmPdfFallbackWriter {
  fun writeTextPdf(file: File, lines: List<String>) {
    val sanitized =
      lines.map { line ->
        line
          .replace("\\", "\\\\")
          .replace("(", "\\(")
          .replace(")", "\\)")
          .filter { it.code in 32..126 }
      }
    val streamBuilder = StringBuilder("BT\n/F1 11 Tf\n40 790 Td\n15 TL\n")
    sanitized.take(45).forEachIndexed { idx, txt ->
      if (idx == 0) streamBuilder.append("($txt) Tj\n")
      else streamBuilder.append("T* ($txt) Tj\n")
    }
    streamBuilder.append("ET\n")
    val contentBytes = streamBuilder.toString().toByteArray(Charsets.US_ASCII)

    val out = ByteArrayOutputStream()
    val offsets = mutableListOf<Int>()
    fun writeStr(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

    writeStr("%PDF-1.4\n")
    offsets.add(out.size())
    writeStr("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    offsets.add(out.size())
    writeStr("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
    offsets.add(out.size())
    writeStr(
      "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
    )
    offsets.add(out.size())
    writeStr("4 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n")
    out.write(contentBytes)
    writeStr("\nendstream\nendobj\n")
    offsets.add(out.size())
    writeStr("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

    val xrefStart = out.size()
    writeStr("xref\n0 ${offsets.size + 1}\n")
    writeStr("0000000000 65535 f \n")
    offsets.forEach { off -> writeStr("%010d 00000 n \n".format(off)) }
    writeStr("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")

    file.parentFile?.mkdirs()
    FileOutputStream(file).use { it.write(out.toByteArray()) }
  }
}
