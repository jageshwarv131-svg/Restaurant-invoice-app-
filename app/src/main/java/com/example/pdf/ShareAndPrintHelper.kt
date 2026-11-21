package com.example.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ShareAndPrintHelper {

  fun getShareUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  fun sharePdfDocument(context: Context, pdfFile: File, title: String = "Share Invoice PDF") {
    val uri = getShareUri(context, pdfFile)
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }

  fun shareCsvDocument(context: Context, csvFile: File, title: String = "Share Report CSV") {
    val uri = getShareUri(context, csvFile)
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }

  fun reopenPdfExternal(context: Context, pdfFile: File): Boolean {
    return runCatching {
        val uri = getShareUri(context, pdfFile)
        val viewIntent =
          Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        context.startActivity(Intent.createChooser(viewIntent, "Open PDF"))
      }
      .isSuccess
  }

  fun printPdf(context: Context, pdfFile: File, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter =
      object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
          }
          val info =
            PrintDocumentInfo.Builder(pdfFile.name)
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
              .build()
          callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback,
        ) {
          try {
            FileInputStream(pdfFile).use { input ->
              FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback.onWriteFailed(e.message)
          }
        }
      }
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
  }

  fun writeFileToSafUri(context: Context, sourceFile: File, destUri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(destUri)?.use { out ->
          FileInputStream(sourceFile).use { input -> input.copyTo(out) }
        } ?: return false
        true
      }
      .getOrDefault(false)
  }

  fun writeTextToSafUri(context: Context, content: String, destUri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(destUri)?.use { out ->
          out.write(content.toByteArray(Charsets.UTF_8))
        } ?: return false
        true
      }
      .getOrDefault(false)
  }

  fun readTextFromSafUri(context: Context, sourceUri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(sourceUri)?.bufferedReader(Charsets.UTF_8)?.use {
          it.readText()
        }
      }
      .getOrNull()
  }
}
