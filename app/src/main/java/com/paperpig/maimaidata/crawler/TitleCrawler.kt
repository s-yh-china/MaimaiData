package com.paperpig.maimaidata.crawler

import android.net.Uri
import com.paperpig.maimaidata.crawler.CrawlerCaller.finishUpdate
import com.paperpig.maimaidata.crawler.CrawlerCaller.onError
import com.paperpig.maimaidata.crawler.CrawlerCaller.startAuth
import com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog
import com.paperpig.maimaidata.model.api.LoginContextModel
import com.paperpig.maimaidata.model.api.QrCodeLoginModel
import com.paperpig.maimaidata.network.ApiErrorException
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.widgets.Settings
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

object TitleCrawler {

    fun startBind(url: String) {
        if (!Settings.getProberUpdateUseAPI()) {
            writeLog("未启用API")
            finishUpdate()
            return
        }

        val qrCode = extractQrCode(url)
        if (qrCode.isNullOrBlank()) {
            writeLog("链接无效：无法解析二维码参数")
            finishUpdate()
            return
        }

        startAuth()
        writeLog("开始登陆")

        MaimaiDataRequests.qrCodeLogin(QrCodeLoginModel(qrCode))
            .doOnNext { writeLog("登陆成功，开始获取成绩") }
            .flatMap { loginContext ->
                val work: Observable<Int> = MaimaiDataRequests.getUserMusicData(loginContext)
                    .doOnNext { writeLog("已获取数据，正在载入") }
                    .flatMap { data ->
                        Observable.fromCallable {
                            val records = JsonConvertToDb.convertUserRecordData(data, Settings.getUpdateDifficulty())
                            RecordRepository.getInstance().replaceAllRecord(records)
                            records.size
                        }.subscribeOn(Schedulers.io()).doOnNext { count -> writeLog("maimai 数据更新完成，共加载了 $count 条数据") }
                    }

                work.flatMap { count -> logoutSafely(loginContext).map { count } }
                    .onErrorResumeNext { e: Throwable -> logoutSafely(loginContext).flatMap { Observable.error(e) } }
            }
            .doFinally { finishUpdate() }
            .subscribe(
                {},
                {
                    onError(it)
                    logApiError("流程发生错误", it)
                }
            )
    }

    private fun logoutSafely(loginContext: LoginContextModel): Observable<Unit> {
        return MaimaiDataRequests.logout(loginContext)
            .doOnNext { resp ->
                if (resp.returnCode == 1) {
                    writeLog("登出成功")
                } else {
                    writeLog("登出失败，返回值 ${resp.returnCode}")
                }
            }
            .doOnError { logApiError("登出时发生错误", it) }
            .onErrorResumeNext(Observable.empty())
            .map {}
    }

    private fun extractQrCode(url: String): String? {
        val path = runCatching { Uri.parse(url).path }.getOrNull().orEmpty()
        val last = path.substringAfterLast('/').ifBlank { return null }
        return last.removeSuffix(".html").takeIf { it.isNotBlank() }
    }

    private fun logApiError(prefix: String, throwable: Throwable) {
        if (throwable is ApiErrorException) {
            writeLog("$prefix(${throwable.code}) ${throwable.detail}")
        } else {
            writeLog("${prefix}：未知错误")
        }
    }
}