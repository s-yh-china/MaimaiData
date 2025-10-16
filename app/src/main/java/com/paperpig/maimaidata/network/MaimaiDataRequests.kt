package com.paperpig.maimaidata.network

import com.google.gson.Gson
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.model.ChartAliasData
import com.paperpig.maimaidata.model.ChartStatsData
import com.paperpig.maimaidata.model.DataVersionModel
import com.paperpig.maimaidata.model.api.QrCodeBindModel
import com.paperpig.maimaidata.model.api.UserMusicDataModel
import com.paperpig.maimaidata.utils.JsonConvertToDb
import io.reactivex.Observable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * @author BBS
 * @since  2021/5/13
 */
object MaimaiDataRequests {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun fetchUpdateInfo(): Observable<AppUpdateModel> =
        MaimaiDataClient
            .instance
            .getService()
            .getUpdateInfo()
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it, AppUpdateModel::class.java)
                Observable.just(model)
            }

    fun getDataVersion(): Observable<DataVersionModel> =
        MaimaiDataClient
            .instance
            .getService()
            .getDataVersion()
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it.asJsonObject.get(JsonConvertToDb.DATA_STRUCTURE_VERSION.toString()), DataVersionModel::class.java)
                Observable.just(model)
            }

    fun getChartStats(version: String): Observable<ChartStatsData> =
        MaimaiDataClient
            .instance
            .getService()
            .getChartStatus(version)
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it, ChartStatsData::class.java)
                Observable.just(model)
            }

    fun getChartAlias(version: String): Observable<ChartAliasData> =
        MaimaiDataClient
            .instance
            .getService()
            .getChartAlias(version)
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it, ChartAliasData::class.java)
                Observable.just(model)
            }

    fun qrCodeBind(qrCode: String): Observable<QrCodeBindModel> =
        MaimaiDataClient
            .instance
            .getService()
            .apiQrBind("{\"qr_code\":\"$qrCode\"}".toRequestBody(JSON))
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it, QrCodeBindModel::class.java)
                Observable.just(model)
            }

    fun getUserMusicData(userId: String): Observable<UserMusicDataModel> =
        MaimaiDataClient
            .instance
            .getService()
            .apiGetUserMusicData("{\"user_id\":\"$userId\"}".toRequestBody(JSON))
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it, UserMusicDataModel::class.java)
                Observable.just(model)
            }
}