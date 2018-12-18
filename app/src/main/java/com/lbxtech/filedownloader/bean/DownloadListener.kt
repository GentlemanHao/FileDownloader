package com.lbxtech.filedownloader.bean

import java.io.File

interface DownloadListener {
    /*fun onSuccess(fileInfo: FileInfo)
    fun onFail(fileInfo: FileInfo)
    fun onPause(fileInfo: FileInfo)
    fun onProgress(fileInfo: FileInfo)*/
    fun onUpdate(fileInfo: FileInfo)
}