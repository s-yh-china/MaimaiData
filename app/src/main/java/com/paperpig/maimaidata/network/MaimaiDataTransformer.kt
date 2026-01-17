package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Response

/**
 * @author BBS
 * @since  2021-05-13
 */
class MaimaiDataTransformer {
    companion object {
        /**
         * switch thread & check err code
         */
        fun handleResult(): ObservableTransformer<JsonElement, JsonElement> {
            return ObservableTransformer { upstream ->
                upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
        }

        fun handleApiResult(): ObservableTransformer<Response<JsonElement>, JsonElement> {
            return ObservableTransformer { upstream ->
                upstream
                    .subscribeOn(Schedulers.io())
                    .flatMap { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body()
                            if (body != null) {
                                Observable.just(body)
                            } else {
                                Observable.error(ApiErrorException(resp.code(), detail = "Empty body"))
                            }
                        } else {
                            val code = resp.code()
                            val raw = try {
                                resp.errorBody()?.string()
                            } catch (_: Throwable) {
                                null
                            }

                            val detail = try {
                                val obj = raw?.let { JsonParser.parseString(it) }?.asJsonObject
                                if (obj != null && obj.has("detail") && !obj.get("detail").isJsonNull) {
                                    obj.get("detail").asString
                                } else null
                            } catch (_: Throwable) {
                                null
                            }

                            Observable.error(ApiErrorException(code = code, detail = detail, rawBody = raw))
                        }
                    }
                    .observeOn(AndroidSchedulers.mainThread())
            }
        }
    }
}