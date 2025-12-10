package com.paperpig.maimaidata.model.api

import java.io.Serializable

data class QrCodeBindModel(
    val userId: String = "",
    val errorId: Int = 0
) : Serializable