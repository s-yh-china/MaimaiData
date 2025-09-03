package com.paperpig.maimaidata.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class SongData(
    @SerializedName("basic_info")
    val basicInfo: BasicInfo,
    val charts: List<Chart>,
    val ds: List<Double>,
    @SerializedName("old_ds")
    var oldDs: List<Double>,
    val id: String,
    var level: List<String>,
    var title: String,
    @SerializedName("title_kana")
    var titleKana: String,
    val type: String,
    var alias: List<String>?,
) : Parcelable {

    class BasicInfo(
        val artist: String,
        val bpm: Int,
        var from: String,
        var genre: String,
        var catcode: String,
        @SerializedName("is_new")
        val isNew: Boolean,
        val title: String,
        @SerializedName("image_url")
        var imageUrl: String,
        var version: String,
        var kanji: String?,
        var comment: String?,
        var buddy: String?
    ) : Serializable

    class Chart(
        val charter: String,
        val notes: List<Int>
    ) : Serializable
}

