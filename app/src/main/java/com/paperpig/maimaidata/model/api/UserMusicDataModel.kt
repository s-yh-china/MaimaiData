package com.paperpig.maimaidata.model.api

import java.io.Serializable

data class UserMusicDataModel(
    val music: List<UserMusicDetail>
) : Serializable {
    data class UserMusicDetail(
        val musicId: Int,
        val level: Int,
        val achievement: Double,
        val fc: Int,
        val fs: Int,
        val dxScore: Int,
        val playCount: Int
    ) : Serializable
}
