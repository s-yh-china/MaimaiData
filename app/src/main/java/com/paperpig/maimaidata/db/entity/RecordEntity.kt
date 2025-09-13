package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.paperpig.maimaidata.R
import kotlinx.parcelize.Parcelize

@Entity(tableName = "record")
@Parcelize
data class RecordEntity(
    // 主键（自增长，默认值 0）
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 完成率
    @ColumnInfo(name = "achievements")
    val achievements: Double,

    // DX分数
    @ColumnInfo(name = "dx_score")
    val dxScore: Int,

    // 全连状态
    @ColumnInfo(name = "fc")
    val fc: String,

    // 同步状态
    @ColumnInfo(name = "fs")
    val fs: String,

    // 等级
    @ColumnInfo(name = "level")
    val level: String,

    // 等级索引
    @SerializedName("level_index")
    @ColumnInfo(name = "level_index")
    val levelIndex: Int,

    // 评级
    @ColumnInfo(name = "rate")
    val rate: String,

    // 歌曲ID
    @SerializedName("song_id")
    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,
) : Parcelable {
    fun getFcIcon() = when (fc) {
        "fc" -> R.drawable.music_icon_fc
        "fcp" -> R.drawable.music_icon_fcp
        "ap" -> R.drawable.music_icon_ap
        "app" -> R.drawable.music_icon_app
        else -> R.drawable.music_icon_back
    }

    fun getFsIcon() = when (fs) {
        "fs" -> R.drawable.music_icon_fs
        "fsp" -> R.drawable.music_icon_fsp
        "fsd" -> R.drawable.music_icon_fdx
        "fsdp" -> R.drawable.music_icon_fdxp
        else -> R.drawable.music_icon_back
    }

    fun getRankIcon() = when (rate) {
        "d" -> R.drawable.rank_d
        "c" -> R.drawable.rank_c
        "b" -> R.drawable.rank_b
        "bb" -> R.drawable.rank_bb
        "bbb" -> R.drawable.rank_bbb
        "a" -> R.drawable.rank_a
        "aa" -> R.drawable.rank_aa
        "aaa" -> R.drawable.rank_aaa
        "s" -> R.drawable.rank_s
        "sp" -> R.drawable.rank_sp
        "ss" -> R.drawable.rank_ss
        "ssp" -> R.drawable.rank_ssp
        "sss" -> R.drawable.rank_sss
        "sssp" -> R.drawable.rank_sssp
        else -> R.drawable.rank_d
    }
}

