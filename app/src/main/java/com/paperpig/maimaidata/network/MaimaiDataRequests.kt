package com.paperpig.maimaidata.network

import com.google.gson.Gson
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.model.ChartAliasData
import com.paperpig.maimaidata.model.ChartStatsData
import com.paperpig.maimaidata.model.DataVersionModel
import com.paperpig.maimaidata.model.api.LoginContextModel
import com.paperpig.maimaidata.model.api.LogoutRequestModel
import com.paperpig.maimaidata.model.api.LogoutResponseModel
import com.paperpig.maimaidata.model.api.QrCodeLoginModel
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

    fun qrCodeLogin(qrCode: QrCodeLoginModel): Observable<LoginContextModel> =
        MaimaiDataClient
            .instance
            .getService()
            .apiQrCodeLogin(Gson().toJson(qrCode).toRequestBody(JSON))
            .compose(MaimaiDataTransformer.handleApiResult())
            .flatMap {
                val model = Gson().fromJson(it, LoginContextModel::class.java)
                Observable.just(model)
            }

    fun logout(loginContext: LoginContextModel): Observable<LogoutResponseModel> =
        MaimaiDataClient
            .instance
            .getService()
            .apiLogout(Gson().toJson(LogoutRequestModel(2, loginContext)).toRequestBody(JSON))
            .compose(MaimaiDataTransformer.handleApiResult())
            .flatMap {
                val model = Gson().fromJson(it, LogoutResponseModel::class.java)
                Observable.just(model)
            }

    fun getUserMusicData(loginContext: LoginContextModel): Observable<UserMusicDataModel> =
        MaimaiDataClient
            .instance
            .getService()
            .apiGetUserMusicData(Gson().toJson(loginContext).toRequestBody(JSON))
            .compose(MaimaiDataTransformer.handleApiResult())
            .flatMap {
                val model = Gson().fromJson(it, UserMusicDataModel::class.java)
                Observable.just(model)
            }
}