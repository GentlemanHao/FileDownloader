package com.lbxtech.filedownloader.bean

data class FileInfo(val id: Int, val url: String, var position: Long = 0, var length: Long = 0, var progress: Long = 0, var state: DownloadState = DownloadState.UNDO) {
    override fun toString(): String {
        return "FileInfo(id=$id, url='$url', position=$position, length=$length, progress=$progress, state=$state)"
    }
}