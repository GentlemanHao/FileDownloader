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
        if (downloadInfoMap.contains(info.id)) {
            return
        }
        info.state = DownloadState.WAIT
        updateDownloadState(info)
        downloadInfoMap.put(info.id, info)

        val downloadTask = DownloadTask(info)
        ThreadPoolManager.execute(downloadTask)
        downloadTaskMap.put(info.id, downloadTask)
    }

    fun cancleDownload(info: FileInfo) {
        if (downloadInfoMap.contains(info.id)) {
            info.state = DownloadState.PAUSE
            updateDownloadState(info)
            downloadCallMap.get(info.id)?.cancel()
            downloadCallMap.remove(info.id)
            val task = downloadTaskMap.get(info.id)
            if (task is Runnable) {
                ThreadPoolManager.cancle(task)
            }
            downloadTaskMap.remove(info.id)
            downloadInfoMap.remove(info.id)
        }
    }

    class DownloadTask(val info: FileInfo) : Runnable {
        override fun run() {
            val downloadCall = okHttpClient.newCall(Request.Builder().url(info.url).apply {
                if (info.position > 0) addHeader("RANGE", "bytes=${info.position}-")
            }.build())

            downloadCallMap.put(info.id, downloadCall)

            val response = downloadCall.execute()
            if (response.isSuccessful) {

                if (info.length == 0L) {
                    info.length = getFileLength(info.url)
                }

                val saveFile = FileUtil.getDownloadFile(info.url)
                if (FileUtil.fileExists(saveFile) && saveFile.length() == info.length) {
                    info.state = DownloadState.SUCCESS
                    updateDownloadState(info)
                    return
                }

                info.state = DownloadState.DOWNLOAD

                var inputStream: InputStream? = null
                try {
                    inputStream = response.body()?.byteStream() ?: return
                    val randomAccessFile = RandomAccessFile(saveFile, "rw")
                    randomAccessFile.seek(info.position)
                    val bytes = ByteArray(1024)
                    var len = inputStream.read(bytes)
                    while (len != -1) {
                        if (info.state == DownloadState.PAUSE) {
                            return
                        }
                        randomAccessFile.write(bytes, 0, len)
                        info.position += len
                        len = inputStream.read(bytes)
                        updateDownloadState(info)
                    }
                    info.state = DownloadState.SUCCESS
                    updateDownloadState(info)
                } catch (e: IOException) {
                    info.state = DownloadState.FAIL
                    updateDownloadState(info)
                } finally {
                    inputStream?.close()
                }
            } else {
                info.state = DownloadState.FAIL
                updateDownloadState(info)
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