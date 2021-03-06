package com.lbxtech.filedownloader.utils

import com.lbxtech.filedownloader.bean.DownloadListener
import com.lbxtech.filedownloader.bean.DownloadState
import com.lbxtech.filedownloader.bean.FileInfo
import okhttp3.*
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DownloadUtil {

    private val downloadInfoMap = ConcurrentHashMap<Int, FileInfo>()
    private val downloadCallMap = ConcurrentHashMap<Int, Call>()
    private val downloadTaskMap = ConcurrentHashMap<Int, DownloadTask>()
    private val downloadListeners = ArrayList<DownloadListener>()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    fun startDownLoad(info: FileInfo) {
        if (downloadInfoMap.contains(info)) {
            return
        }
        info.state = DownloadState.WAIT
        updateDownloadState(info)

        downloadInfoMap[info.id] = info

        val downloadTask = DownloadTask(info)
        ThreadPoolManager.execute(downloadTask)

        downloadTaskMap[info.id] = downloadTask
    }

    fun cancelDownload(info: FileInfo) {
        if (downloadInfoMap.contains(info)) {
            updateDownloadState(info)

            downloadCallMap[info.id]?.cancel()
            downloadCallMap.remove(info.id)

            ThreadPoolManager.cancle(downloadTaskMap[info.id] as Runnable)
            downloadTaskMap.remove(info.id)

            downloadInfoMap.remove(info.id)
        }
    }

    class DownloadTask(val info: FileInfo) : Runnable {

        override fun run() {
            var inputStream: InputStream? = null
            try {
                val downloadCall = okHttpClient.newCall(Request.Builder().url(info.url).apply {
                    if (info.position > 0) addHeader("RANGE", "bytes=${info.position}-")
                }.build())

                downloadCallMap[info.id] = downloadCall

                val response = downloadCall.execute()
                if (response.isSuccessful) {

                    if (info.length == 0L) {
                        val length = getFileLength(info.url)
                        if (length == 0L) {
                            info.state = DownloadState.FAIL
                            cancelDownload(info)
                            return
                        }
                        info.length = length
                    }

                    val saveFile = FileUtil.getDownloadFile(info.url)
                    if (FileUtil.fileExists(saveFile) && saveFile.length() == info.length) {
                        info.state = DownloadState.SUCCESS
                        info.position = info.length
                        info.progress = 100
                        updateDownloadState(info)
                        return
                    }

                    info.state = DownloadState.DOWNLOAD

                    inputStream = response.body()?.byteStream() ?: return

                    val randomAccessFile = RandomAccessFile(saveFile, "rw")
                    randomAccessFile.seek(info.position)

                    info.progress = info.position * 100 / info.length

                    val bytes = ByteArray(1024)
                    var len = inputStream.read(bytes)

                    while (len != -1) {
                        randomAccessFile.write(bytes, 0, len)
                        len = inputStream.read(bytes)
                        info.position += len

                        val tmp = info.position * 100 / info.length
                        if (info.progress != tmp) {
                            info.progress = tmp
                            updateDownloadState(info)
                        }
                    }
                    info.state = DownloadState.SUCCESS
                    info.progress = 100
                    updateDownloadState(info)
                } else {
                    info.state = DownloadState.FAIL
                    cancelDownload(info)
                }
            } catch (e: IOException) {
                if (info.state != DownloadState.PAUSE) {
                    info.state = DownloadState.FAIL
                    cancelDownload(info)
                }
            } finally {
                inputStream?.close()
            }
        }
    }

    private fun getFileLength(url: String): Long {
        okHttpClient.newCall(Request.Builder().url(url).build()).execute().apply {
            if (isSuccessful) {
                return body()?.contentLength() ?: 0
            }
        }
        return 0
    }

    private fun updateDownloadState(info: FileInfo) {
        downloadListeners.forEach {
            it.onUpdate(info)
        }
    }

    fun registerDownloadListener(listener: DownloadListener) {
        if (!downloadListeners.contains(listener)) {
            downloadListeners.add(listener)
        }
    }

    fun unRegisterDownloadListener(listener: DownloadListener) {
        if (downloadListeners.contains(listener)) {
            downloadListeners.remove(listener)
        }
    }
}