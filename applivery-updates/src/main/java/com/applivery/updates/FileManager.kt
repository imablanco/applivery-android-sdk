package com.applivery.updates

import android.content.Context
import android.content.Context.MODE_WORLD_READABLE
import android.os.Build
import com.applivery.base.util.AppliveryLog
import com.applivery.updates.domain.DownloadInfo
import okhttp3.ResponseBody
import java.io.*
import kotlin.math.pow
import kotlin.math.roundToInt

class FileManager(private val context: Context) {

    internal fun writeResponseBodyToDisk(
        body: ResponseBody,
        file: File,
        onUpdate: (DownloadInfo) -> Unit,
        onFinish: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                val fileReader = ByteArray(4096)

                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                val totalFileSize = (fileSize / 1024.0.pow(2.0)).toInt()

                inputStream = body.byteStream()
                outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileOutputStream(file)
                } else {
                    context.openFileOutput(file.name, MODE_WORLD_READABLE)
                }

                val startTime = System.currentTimeMillis()
                var timeCount = 1

                onUpdate(DownloadInfo(totalFileSize = totalFileSize))

                while (true) {
                    val read = inputStream!!.read(fileReader)
                    if (read == -1) {
                        break
                    }

                    outputStream?.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()

                    val current = (fileSizeDownloaded / 1024.0.pow(2.0)).roundToInt().toDouble()
                    val progress = (fileSizeDownloaded * 100 / fileSize).toInt()
                    val currentTime = System.currentTimeMillis() - startTime

                    if (currentTime > 1000 * timeCount) {
                        onUpdate(
                            DownloadInfo(
                                progress = progress,
                                currentFileSize = current.toInt(),
                                totalFileSize = totalFileSize
                            )
                        )
                        timeCount++
                    }

                    AppliveryLog.debug("File download: $fileSizeDownloaded of $fileSize")
                }

                outputStream?.flush()
                onFinish()
            } catch (e: IOException) {
                onError(e)
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            onError(e)
        }
    }
}