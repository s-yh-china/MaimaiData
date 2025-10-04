package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DataVersionModel(
    val version: String,
    @SerializedName("song_list_url")
    val songListUrl: String,
    @SerializedName("chart_stats_url")
    val chartStatsUrl: String,
    @SerializedName("chart_alias_url")
    val chartAliasUrl: String
) : Serializable