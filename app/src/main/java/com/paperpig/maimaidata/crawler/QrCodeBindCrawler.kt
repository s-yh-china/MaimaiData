package com.paperpig.maimaidata.crawler

import android.annotation.SuppressLint
import com.paperpig.maimaidata.crawler.CrawlerCaller.finishUpdate
import com.paperpig.maimaidata.crawler.CrawlerCaller.onError
import com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.network.vpn.core.LocalVpnService
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object QrCodeBindCrawler {
    private val errorIdMessage = mapOf(1 to "二维码已过期", 2 to "二维码无效", 50 to "服务端错误")

    @SuppressLint("CheckResult")
    fun startBind(url: String) {
        if (!Settings.getProberUpdateUseAPI()) {
            writeLog("未启用API，不进行绑定流程")
            finishUpdate()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            CrawlerCaller.startAuth()
            writeLog("开始尝试绑定用户")
            val qrCode = url.substringBefore('?').substringBeforeLast(".html").substringAfterLast('/')
            MaimaiDataRequests.qrCodeBind(qrCode).subscribe(
                {
                    if (it.errorId == 0) {
                        SpUtil.saveUserId(it.userId)
                        writeLog("已成功绑定用户")
                    } else {
                        writeLog("绑定失败：${errorIdMessage.getOrDefault(it.errorId, "未知错误码")}")
                    }
                    finishUpdate()
                },
                { e ->
                    onError(e)
                    writeLog("因意外原因绑定失败")
                }
            )
        }
    }
}