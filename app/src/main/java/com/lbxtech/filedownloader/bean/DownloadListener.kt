package com.lbxtech.filedownloader.bean

import java.io.File

interface DownloadListener {
    fun onSuccess(file: File)
    fun onFail(msg: String)
    fun onPause()
    fun onProgress(progress: Int)
}