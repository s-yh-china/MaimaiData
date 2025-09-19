package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName

data class AppUpdateModel(
    @SerializedName("apk_version")
    var version: String,

    @SerializedName("apk_url")
    var url: String,

    @SerializedName("apk_info")
    var info: String? = null,
)