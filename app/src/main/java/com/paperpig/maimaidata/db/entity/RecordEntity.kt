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
        "fc" -> R.drawable.mmd_player_rtsong_fc
        "fcp" -> R.drawable.mmd_player_rtsong_fcp
        "ap" -> R.drawable.mmd_player_rtsong_ap
        "app" -> R.drawable.mmd_player_rtsong_app
        else -> R.drawable.mmd_player_rtsong_stub
    }

    fun getFsIcon() = when (fs) {
        "fs" -> R.drawable.mmd_player_rtsong_fs
        "fsp" -> R.drawable.mmd_player_rtsong_fsp
        "fsd" -> R.drawable.mmd_player_rtsong_fsd
        "fsdp" -> R.drawable.mmd_player_rtsong_fsdp
        else -> R.drawable.mmd_player_rtsong_stub
    }

    fun getRankIcon() = when (rate) {
        "d" -> R.drawable.mmd_player_rtsong_d
        "c" -> R.drawable.mmd_player_rtsong_c
        "b" -> R.drawable.mmd_player_rtsong_b
        "bb" -> R.drawable.mmd_player_rtsong_bb
        "bbb" -> R.drawable.mmd_player_rtsong_bbb
        "a" -> R.drawable.mmd_player_rtsong_a
        "aa" -> R.drawable.mmd_player_rtsong_aa
        "aaa" -> R.drawable.mmd_player_rtsong_aaa
        "s" -> R.drawable.mmd_player_rtsong_s
        "sp" -> R.drawable.mmd_player_rtsong_sp
        "ss" -> R.drawable.mmd_player_rtsong_ss
        "ssp" -> R.drawable.mmd_player_rtsong_ssp
        "sss" -> R.drawable.mmd_player_rtsong_sss
        "sssp" -> R.drawable.mmd_player_rtsong_sssp
        else -> R.drawable.mmd_player_rtsong_d
    }
}

