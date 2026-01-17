package com.paperpig.maimaidata.model.api

import java.io.Serializable

data class UserMusicDataModel(
    val music: List<UserMusicDetail>
) : Serializable {
    data class UserMusicDetail(
        val musicId: Int,
        val level: Int,
        val playCount: Int,
        val achievement: Int,
        val deluxscoreMax: Int,
        val comboStatus: Int,
        val syncStatus: Int,
        val scoreRank: Int,
        val extNum1: Int
    ) : Serializable
}
