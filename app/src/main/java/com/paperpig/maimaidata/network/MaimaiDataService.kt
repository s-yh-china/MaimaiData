package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import com.paperpig.maimaidata.BuildConfig
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.Response
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
    @POST("/api/title/user/login/qr_code_login")
    fun apiQrCodeLogin(@Body body: RequestBody): Observable<Response<JsonElement>>

    @Headers(
        "AppID: 368059",
        "User-Agent: MaimaiData#${BuildConfig.VERSION_NAME}"
    )
    @POST("/api/title/user/login/logout")
    fun apiLogout(@Body body: RequestBody): Observable<Response<JsonElement>>

    @Headers(
        "AppID: 368059",
        "User-Agent: MaimaiData#${BuildConfig.VERSION_NAME}"
    )
    @POST("/api/title/user/data/music")
    fun apiGetUserMusicData(@Body body: RequestBody): Observable<Response<JsonElement>>
}