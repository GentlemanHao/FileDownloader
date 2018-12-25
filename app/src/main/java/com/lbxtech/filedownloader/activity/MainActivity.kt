package com.lbxtech.filedownloader.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.lbxtech.filedownloader.R
import com.lbxtech.filedownloader.bean.DownloadListener
import com.lbxtech.filedownloader.bean.DownloadState
import com.lbxtech.filedownloader.bean.FileInfo
import com.lbxtech.filedownloader.utils.DownloadUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var weChat = "https://dldir1.qq.com/weixin/android/weixin673android1360.apk"
    private var aliPay = "https://t.alipayobjects.com/L1/71/100/and/alipay_wap_main.apk"
    private var qq = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk"
    private val list = arrayListOf(FileInfo(1, weChat), FileInfo(2, qq), FileInfo(3, aliPay))
    private var adapter: DownloadAdapter? = null
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 0) {
                //adapter?.notifyItemChanged(msg.arg1)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        downloadList.layoutManager = LinearLayoutManager(this)
        adapter = DownloadAdapter()
        downloadList.adapter = adapter
    }

    private val downloadListener = object : DownloadListener {
        override fun onUpdate(fileInfo: FileInfo) {
            /*list.forEachIndexed { position, info ->
                if (info.id == fileInfo.id) {
                    handler.sendMessage(Message.obtain().apply {
                        what = 0
                        arg1 = position
                    })
                }
            }*/
            handler.sendEmptyMessage(0)
        }
    }

    inner class DownloadAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        init {
            DownloadUtil.registerDownloadListener(downloadListener)
        }

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false))
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as ViewHolder).apply {
                val info = list[position]
                pb.progress = info.progress.toInt()
                tv.text = "${info.progress}%"
                bt.text = when (info.state) {
                    DownloadState.SUCCESS -> "SUCCESS"
                    DownloadState.FAIL -> "FAIL"
                    DownloadState.PAUSE -> "PAUSE"
                    DownloadState.DOWNLOAD -> "DOWNLOAD"
                    DownloadState.WAIT -> "WAIT"
                    DownloadState.UNDO -> "UNDO"
                }
                bt.setOnClickListener {
                    when (info.state) {
                        DownloadState.SUCCESS -> info.state = DownloadState.SUCCESS
                        DownloadState.DOWNLOAD -> {
                            info.state = DownloadState.PAUSE
                            DownloadUtil.cancelDownload(info)
                        }
                        DownloadState.WAIT, DownloadState.FAIL, DownloadState.PAUSE, DownloadState.UNDO -> {
                            DownloadUtil.startDownLoad(info)
                        }
                    }
                }
            }
        }

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pb = view.findViewById<ProgressBar>(R.id.pb_progress)
        val bt = view.findViewById<Button>(R.id.bt_start)
        val tv = view.findViewById<TextView>(R.id.tv_progress)
    }
}
