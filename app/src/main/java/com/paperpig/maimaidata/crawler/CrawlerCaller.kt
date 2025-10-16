package com.paperpig.maimaidata.crawler

import com.paperpig.maimaidata.network.vpn.core.LocalVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

object CrawlerCaller {
    private var listener: WechatCrawlerListener? = null

    fun getWechatAuthUrl(): String? {
        return try {
            WechatCrawler.getWechatAuthUrl()
        } catch (error: IOException) {
            writeLog("获取微信登录url时出现错误:")
            onError(error)
            null
        }
    }

    fun writeLog(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onMessageReceived(text)
        }
    }

    fun startAuth() {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onStartAuth()
        }
    }

    fun finishUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onFinishUpdate()
        }
    }

    fun onError(e: Throwable) {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onError(e)
        }
    }

    fun fetchData(authUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Thread.sleep(3000)
                LocalVpnService.IsRunning = false
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                onError(e)
            }
            WechatCrawler.startFetch(authUrl)
        }
    }

    fun setOnWechatCrawlerListener(listener: WechatCrawlerListener) {
        this.listener = listener
    }

    fun removeOnWechatCrawlerListener() {
        this.listener = null
    }
}