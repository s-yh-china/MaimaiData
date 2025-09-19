package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ChartStatsData(
    val time: Long,
    val stats: List<ChartData>
) {
    data class ChartData(
        val id: Int,
        @SerializedName("fit_difficulty")
        val fitDifficulty: List<Double>
    ) : Serializable
}