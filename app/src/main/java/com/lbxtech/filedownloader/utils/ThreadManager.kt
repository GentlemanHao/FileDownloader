package com.lbxtech.filedownloader.utils

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadPoolManager {

    private var threadPoolExecutor: ThreadPoolExecutor? = null

    fun execute(runnable: Runnable) {
        if (threadPoolExecutor == null) {
            initThreadPool(3, 5, 10)
        }
        threadPoolExecutor?.execute(runnable)
    }

    fun cancle(runnable: Runnable) {
        threadPoolExecutor?.queue?.remove(runnable)
    }

    private fun initThreadPool(corePoolSize: Int, maximumPoolSize: Int, keepAliveTime: Long) {
        threadPoolExecutor = ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>(),
                Executors.defaultThreadFactory(),
                ThreadPoolExecutor.AbortPolicy())
    }
}