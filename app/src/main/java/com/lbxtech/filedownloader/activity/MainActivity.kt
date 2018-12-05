package com.lbxtech.filedownloader.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.lbxtech.filedownloader.R
import com.lbxtech.filedownloader.bean.DownloadListener
import com.lbxtech.filedownloader.bean.DownloadState
import com.lbxtech.filedownloader.bean.FileInfo
import com.lbxtech.filedownloader.utils.HttpUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private var weChat = "https://dldir1.qq.com/weixin/android/weixin673android1360.apk"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fileInfo = FileInfo(weChat)

        bt_start.setOnClickListener {
            if (fileInfo.state == DownloadState.WAIT || fileInfo.state == DownloadState.PAUSE || fileInfo.state == DownloadState.FAIL) {

            } else {

            }

            when (fileInfo.state) {
                DownloadState.DOWNLOAD -> {
                    fileInfo.state = DownloadState.PAUSE
                    bt_start.text = "PAUSE"
                }
                DownloadState.WAIT, DownloadState.PAUSE, DownloadState.FAIL -> startDownload(fileInfo)
                DownloadState.SUCCESS -> {

                }
            }
        }
    }

    private fun startDownload(fileInfo: FileInfo) {
        HttpUtil.downLoad(fileInfo, object : DownloadListener {
            override fun onSuccess(file: File) {
                //bt_start.text = "SUCCESS"
            }

            override fun onFail(msg: String) {
                fileInfo.state = DownloadState.FAIL
                //bt_start.text = "FAIL"
            }

            override fun onPause() {
                //bt_start.text = "PAUSE"
            }

            override fun onProgress(progress: Int) {
                if (pb_progress.progress != progress) {
                    pb_progress.progress = progress
                    Log.d("--wh--", "progress:$progress")
                }
            }
        })
    }
}
