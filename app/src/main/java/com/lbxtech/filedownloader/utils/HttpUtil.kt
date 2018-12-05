package com.lbxtech.filedownloader.utils

import android.util.Log
import com.lbxtech.filedownloader.bean.DownloadListener
import com.lbxtech.filedownloader.bean.DownloadState
import com.lbxtech.filedownloader.bean.FileInfo
import okhttp3.*
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

object HttpUtil {

    private val okHttpClient: OkHttpClient

    init {
        okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
    }

    private fun getFileLength(url: String): Long {
        okHttpClient.newCall(Request.Builder().url(url).build()).execute().apply {
            if (isSuccessful) {
                return body()?.contentLength() ?: 0
            }
        }
        return 0
    }

    fun downLoad(fileInfo: FileInfo, listener: DownloadListener) {
        okHttpClient.newCall(Request.Builder().url(fileInfo.url).apply {
            if (fileInfo.position > 0) addHeader("RANGE", "bytes=${fileInfo.position}-")
        }.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onFail(e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val fileLength = response.body()?.contentLength() ?: return

                    Log.d("--wh--", "fileLength:$fileLength     fileInfo.length:${fileInfo.length}       fileInfo.position:${fileInfo.position}")

                    if (fileInfo.length == 0L) {
                        fileInfo.length = getFileLength(fileInfo.url)
                    }

                    val saveFile = FileUtil.getDownloadFile(fileInfo.url)
                    if (FileUtil.fileExists(saveFile) && saveFile.length() == fileInfo.length) {
                        fileInfo.state = DownloadState.SUCCESS
                        fileInfo.position = 0
                        return
                    }

                    fileInfo.state = DownloadState.DOWNLOAD

                    var inputStream: InputStream? = null
                    try {
                        inputStream = response.body()?.byteStream() ?: return
                        val randomAccessFile = RandomAccessFile(saveFile, "rw")
                        randomAccessFile.seek(fileInfo.position)
                        val bytes = ByteArray(1024)
                        var progress = fileInfo.position
                        var len = inputStream.read(bytes)
                        while (len != -1) {
                            if (fileInfo.state == DownloadState.PAUSE) {
                                return
                            }
                            randomAccessFile.write(bytes, 0, len)
                            progress += len
                            fileInfo.position = progress
                            len = inputStream.read(bytes)
                            listener.onProgress((progress * 100 / fileInfo.length).toInt())
                        }
                        listener.onSuccess(saveFile)
                    } catch (e: IOException) {
                        listener.onFail(e.toString())
                    } finally {
                        inputStream?.close()
                    }
                }
            }
        })
    }
}