package com.paperpig.maimaidata.model.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class QrCodeBindModel(
    @SerializedName("user_id")
    val userId: String = "",
    @SerializedName("error_id")
    val errorId: Int = 0
): Serializable