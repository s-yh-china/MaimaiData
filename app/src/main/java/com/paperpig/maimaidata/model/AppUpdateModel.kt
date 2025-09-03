package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName

/**
 * @author BBS
 * @since  2021/9/6
 */
data class AppUpdateModel(
    /**
     * apk version string
     */
    @SerializedName("apk_version")
    var version: String,

    /**
     * newest apk url
     */
    @SerializedName("apk_url")
    var url: String,

    /**
     * update info
     */
    @SerializedName("apk_info")
    var info: String? = null,

    /**
     * dx2025 json data version string
     */
    @SerializedName("data_version")
    var dataVersion: String,

    /**
     * dx2025 json url
     */
    @SerializedName("data_url")
    var dataUrl: String
)