package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import com.paperpig.maimaidata.BuildConfig
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
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

    @Headers(
        "AppID: 368059",
        "User-Agent: MaimaiData#${BuildConfig.VERSION_NAME}"
    )
    @POST("/api/user/wc_aime/qr_code_bind")
    fun apiQrBind(@Body body: RequestBody): Observable<JsonElement>

    @Headers(
        "AppID: 368059",
        "User-Agent: MaimaiData#${BuildConfig.VERSION_NAME}"
    )
    @POST("/api/user/data/music")
    fun apiGetUserMusicData(@Body body: RequestBody): Observable<JsonElement>
}