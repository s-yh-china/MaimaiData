package com.paperpig.maimaidata.network

import com.google.gson.Gson
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.model.ChartStatsData
import com.paperpig.maimaidata.model.DataVersionModel
import com.paperpig.maimaidata.utils.JsonConvertToDb
import io.reactivex.Observable

/**
 * @author BBS
 * @since  2021/5/13
 */
object MaimaiDataRequests {
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
}