package com.paperpig.maimaidata.network

import com.google.gson.Gson
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.model.ChartsResponse
import io.reactivex.Observable

/**
 * @author BBS
 * @since  2021/5/13
 */
object MaimaiDataRequests {
    /**
     * fetch the version info for updating
     */
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

    /**
     * get chart_status json
     */
    fun getChartStatus(): Observable<ChartsResponse> =
        MaimaiDataClient
            .instance
            .getService()
            .getChartStatus()
            .compose(MaimaiDataTransformer.handleResult())
            .flatMap {
                val model = Gson().fromJson(it, ChartsResponse::class.java)
                Observable.just(model)
            }
}