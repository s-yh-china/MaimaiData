package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface MaimaiDataService {
    @GET("/update.json")
    fun getUpdateInfo(): Observable<JsonElement>

    @GET("/data_version.json")
    fun getDataVersion(): Observable<JsonElement>

    @GET("/data/chart_stats/{version}.json")
    fun getChartStatus(@Path("version") version: String): Observable<JsonElement>

    @GET("/data/chart_alias/{version}.json")
    fun getChartAlias(@Path("version") version: String): Observable<JsonElement>
}