package com.lbxtech.filedownloader.utils

import android.os.Environment
import java.io.File

object FileUtil {

    private val DOWNLOAD_PATH = Environment.getExternalStorageDirectory().path + "/Download"

    fun fileExists(file: File?): Boolean {
        file?.apply {
            if (isFile) {
                return exists()
            }
        }
        return false
    }

    fun getDownloadFile(url: String): File {
        return File(DOWNLOAD_PATH, getFileName(url))
    }

    fun getFileName(url: String): String {
        return url.substring(url.lastIndexOf("/") + 1)
    }
}