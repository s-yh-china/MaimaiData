package com.paperpig.maimaidata.model.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LogoutRequestModel(
    @SerializedName("logout_type")
    val logoutType: Int,
    @SerializedName("login_context")
    val loginContext: LoginContextModel
) : Serializable