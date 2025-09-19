package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.SongType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "song_data")
data class SongDataEntity(
    @PrimaryKey
    val id: Int,

    @ColumnInfo(name = "sort_id")
    val sortId: Int,

    val title: String,

    @ColumnInfo(name = "title_kana")
    val titleKana: String,

    val type: SongType,

    @ColumnInfo(name = "release_time")
    val releaseTime: Int,

    val artist: String,

    val genre: String,

    val bpm: Int,

    val version: String,

    @ColumnInfo(name = "jp_version")
    val jpVersion: String,

    @ColumnInfo(name = "is_new")
    val isNew: Boolean,

    @ColumnInfo(name = "image_url")
    val imageUrl: String,

    val kanji: String?,

    val comment: String?,

    val buddy: Boolean?,
) : Parcelable {
    @IgnoredOnParcel
    @Ignore
    @ColorRes
    val bgColor: Int = when (genre) {
        "流行&动漫" -> R.color.pop
        "niconico & VOCALOID" -> R.color.vocal
        "东方Project" -> R.color.touhou
        "其他游戏" -> R.color.variety
        "舞萌" -> R.color.maimai
        "宴·会·场" -> R.color.utage
        "音击&中二节奏" -> R.color.gekichuni
        else -> R.color.white
    }

    @IgnoredOnParcel
    @Ignore
    @ColorRes
    val strokeColor: Int = when (genre) {
        "流行&动漫" -> R.color.pop_stroke
        "niconico & VOCALOID" -> R.color.vocal_stroke
        "东方Project" -> R.color.touhou_stroke
        "其他游戏" -> R.color.variety_stroke
        "舞萌" -> R.color.maimai_stroke
        "宴·会·场" -> R.color.utage_stroke
        "音击&中二节奏" -> R.color.gekichuni_stroke
        else -> R.color.white
    }
}


