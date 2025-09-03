package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers

/**
 * @author BBS
 * @since  2021-05-13
 */
interface MaimaiDataService {
    /**
     * fetch update info from a noob's server
     */
    @Headers("urlName:https://maimaidata.violetc.net/")
    @GET("/update.json")
    fun getUpdateInfo(): Observable<JsonElement>

    /**
     * get chart_status from diving-fish.com
     */
    @GET("/api/maimaidxprober/chart_stats")
    fun getChartStatus(): Observable<JsonElement>
}