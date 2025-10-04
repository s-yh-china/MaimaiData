package com.paperpig.maimaidata.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class SongData(
    val id: Int,
    @SerializedName("sort_id")
    val sortId: Int?,
    var title: String,
    @SerializedName("title_kana")
    var titleKana: String,
    val type: SongType,
    @SerializedName("release_time")
    val releaseTime: Int,
    @SerializedName("basic_info")
    val basicInfo: BasicInfo,
    val charts: List<Chart>,
) : Parcelable {
    data class BasicInfo(
        val bpm: Int,
        val artist: String,
        val genre: String,
        val version: String,
        @SerializedName("jp_version")
        val jpVersion: String,
        @SerializedName("is_new")
        val isNew: Boolean,
        @SerializedName("image_url")
        val imageUrl: String,
        @SerializedName("utage_info")
        val utageInfo: UtageInfo?
    ) : Serializable {
        data class UtageInfo(
            val kanji: String,
            val comment: String,
            val buddy: Boolean
        ) : Serializable
    }

    data class Chart(
        val charter: String,
        val level: String,
        @SerializedName("internal_level")
        val internalLevel: Double,
        @SerializedName("old_internal_level")
        val oldInternalLevel: Double?,
        val notes: NoteInfo
    ) : Serializable {
        data class NoteInfo(
            val tap: Int,
            val hold: Int,
            val slide: Int,
            val touch: Int?,
            @SerializedName("break")
            val `break`: Int
        ) : Serializable
    }
}

