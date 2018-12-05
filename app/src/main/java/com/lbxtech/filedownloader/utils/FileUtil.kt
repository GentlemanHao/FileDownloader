package com.lbxtech.filedownloader.utils

import java.io.File

object FileUtil {
    fun fileExists(file: File?): Boolean {
        file?.apply {
            if (isFile) {
                return exists()
            }
        }
        return false
    }
}