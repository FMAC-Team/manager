package me.nekosu.aqnya.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.content.ClipData

object LogUtils {
    fun exportLogs(context: Context) {
        val logFile = File(context.filesDir, "debug.log")
        if (logFile.exists() && logFile.length() > 0) {
            shareLogFile(context, logFile)
        } else {
            Toast.makeText(context, "日志为空", Toast.LENGTH_SHORT).show()
        }
    }


    private fun shareLogFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, "debug.log", contentUri)
            }

        context.startActivity(Intent.createChooser(intent, "分享/导出日志"))
    }
}
