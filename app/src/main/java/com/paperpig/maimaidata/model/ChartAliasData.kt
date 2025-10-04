package com.paperpig.maimaidata.model

import java.io.Serializable

data class ChartAliasData(
    val time: Long,
    val aliases: List<AliasData>
) {
    data class AliasData(
        val id: Int,
        val alias: List<String>
    ) : Serializable
}
