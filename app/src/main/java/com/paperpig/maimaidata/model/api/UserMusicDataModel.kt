package com.paperpig.maimaidata.model.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserMusicDataModel(
    val music: List<UserMusicDetail>
): Serializable {
    data class UserMusicDetail(
        @SerializedName("music_id")
        val musicId: Int,
        val level: Int,
        val achievement: Double,
        val fc: Int,
        val fs: Int,
        @SerializedName("dx_score")
        val dxScore: Int,
        @SerializedName("play_count")
        val playCount: Int
    ): Serializable
}
