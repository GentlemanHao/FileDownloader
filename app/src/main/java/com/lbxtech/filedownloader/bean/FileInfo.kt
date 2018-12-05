package com.lbxtech.filedownloader.bean

data class FileInfo(val url: String, var position: Long = 0, var length: Long = 0, var state: DownloadState = DownloadState.WAIT)