package com.paperpig.maimaidata.crawler

import android.util.Log
import com.paperpig.maimaidata.crawler.CrawlerCaller.finishUpdate
import com.paperpig.maimaidata.crawler.CrawlerCaller.onError
import com.paperpig.maimaidata.crawler.CrawlerCaller.startAuth
import com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.widgets.Settings
import okhttp3.Call
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object WechatCrawler {
    private const val TAG = "Crawler"
    private val jar = SimpleCookieJar()
    private var client: OkHttpClient? = null

    private val recordRepository: RecordRepository = RecordRepository.getInstance()

    init {
        buildHttpClient(false)
    }

    private fun buildHttpClient(followRedirect: Boolean) {
        val builder = OkHttpClient.Builder().apply {
            connectTimeout(120, TimeUnit.SECONDS)
            readTimeout(120, TimeUnit.SECONDS)
            writeTimeout(120, TimeUnit.SECONDS)
            followRedirects(followRedirect)
            followSslRedirects(followRedirect)
            cookieJar(jar)
            cache(null)
            addInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("Cache-Control", "no-cache").build()
                chain.proceed(request)
            }
            connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                        .allEnabledCipherSuites()
                        .build(),
                    ConnectionSpec.CLEARTEXT
                )
            )
            pingInterval(3, TimeUnit.SECONDS)
        }
        client = builder.build()
    }

    fun getWechatAuthUrl(): String {
        buildHttpClient(true)
        val request = Request.Builder()
            .addHeader("Host", "tgk-wcaime.wahlap.com")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; IN2010 Build/RKQ1.211119.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.99 XWEB/4317 MMWEBSDK/20220903 Mobile Safari/537.36 MMWEBID/363 MicroMessenger/8.0.28.2240(0x28001C57) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/wxpic,image/tpg,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .addHeader("X-Requested-With", "com.tencent.mm")
            .addHeader("Sec-Fetch-Site", "none")
            .addHeader("Sec-Fetch-Mode", "navigate")
            .addHeader("Sec-Fetch-User", "?1")
            .addHeader("Sec-Fetch-Dest", "document")
            .addHeader("Accept-Encoding", "gzip, deflate")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .url("https://tgk-wcaime.wahlap.com/wc_auth/oauth/authorize/maimai-dx")
            .build()

        val response = client!!.newCall(request).execute()
        val url = response.request.url.toString().replace("redirect_uri=https", "redirect_uri=http")
        Log.d(TAG, "Auth url:$url")
        return url
    }

    private fun fetchMaimaiData(difficulties: Set<DifficultyType>) {
        val tasks = difficulties.map { difficulty -> CompletableFuture.supplyAsync { fetchData(difficulty) } }
        val allRecords = tasks.flatMap { it.join() }
        Log.d(TAG, "共获取到${allRecords.size}条数据")
        recordRepository.replaceAllRecord(allRecords)
        writeLog("maimai 数据更新完成，共加载了 ${allRecords.size} 条数据")
    }

    private fun fetchData(difficulty: DifficultyType): List<RecordEntity> {
        writeLog("开始获取 ${difficulty.displayName} 难度的数据")
        Log.d(TAG, "开始获取 ${difficulty.displayName} 难度的数据")
        val request = Request.Builder()
            .url("https://maimai.wahlap.com/maimai-mobile/record/musicGenre/search/?genre=99&diff=${difficulty.webDifficultyIndex}")
            .build()

        val call: Call = client!!.newCall(request)
        return try {
            val response = call.execute()
            var data = response.body?.string() ?: ""
            val matcher = Pattern.compile("<html.*>([\\s\\S]*)</html>").matcher(data)
            if (matcher.find()) {
                data = matcher.group(1) ?: ""
            }
            data = Pattern.compile("\\s+").matcher(data).replaceAll(" ")
            writeLog("${difficulty.displayName} 难度的数据已获取，正在处理")
            Log.d(TAG, "${difficulty.displayName} 难度的数据已获取，正在处理")
            WechatDataParser.parsePageToRecordList(data, difficulty)
        } catch (e: Exception) {
            writeLog("获取 ${difficulty.displayName} 难度数据时出现错误: $e")
            Log.d(TAG, "获取 ${difficulty.displayName} 难度数据时出现错误: $e")
            emptyList()
        }
    }

    fun startFetch(authUrl: String) {
        val wechatAuthUrl = authUrl.replaceFirst("http", "https")
        jar.clearCookieStroe()
        try {
            startAuth()
            writeLog("开始登录net，请稍后...")
            loginWechat(wechatAuthUrl)
            writeLog("登陆完成")
        } catch (error: Exception) {
            writeLog("登陆时出现错误:\n")
            onError(error)
            return
        }

        try {
            buildHttpClient(false)
            fetchMaimaiData(Settings.getUpdateDifficulty())
            finishUpdate()
        } catch (error: Exception) {
            writeLog("maimai 数据更新时出现错误: $error")
            onError(error)
        }
    }

    private fun loginWechat(wechatAuthUrl: String) {
        buildHttpClient(true)
        Log.d(TAG, wechatAuthUrl)
        val request = Request.Builder()
            .addHeader("Host", "tgk-wcaime.wahlap.com")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; IN2010 Build/RKQ1.211119.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.99 XWEB/4317 MMWEBSDK/20220903 Mobile Safari/537.36 MMWEBID/363 MicroMessenger/8.0.28.2240(0x28001C57) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/wxpic,image/tpg,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .addHeader("X-Requested-With", "com.tencent.mm")
            .addHeader("Sec-Fetch-Site", "none")
            .addHeader("Sec-Fetch-Mode", "navigate")
            .addHeader("Sec-Fetch-User", "?1")
            .addHeader("Sec-Fetch-Dest", "document")
            .addHeader("Accept-Encoding", "gzip, deflate")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .get()
            .url(wechatAuthUrl)
            .build()

        val response = client!!.newCall(request).execute()
        response.body?.string()
        Log.d(TAG, "登陆成功")
        val code = response.code
        writeLog("登陆状态 $code")
        if (code >= 400) {
            val exception = Exception("登陆时出现错误，请重试！")
            onError(exception)
            throw exception
        }

        val location = response.headers["Location"]
        if (code >= 300 && location != null) {
            val redirectRequest = Request.Builder().url(location).get().build()
            client!!.newCall(redirectRequest).execute().close()
        }
    }
}