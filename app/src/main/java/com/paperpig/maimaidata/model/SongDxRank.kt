package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongDxRank(@field:DrawableRes val icon: Int, val achieve: Double) {
    RANK0(R.drawable.music_icon_dxstar_detail_0, 0.0),
    RANK1(R.drawable.music_icon_dxstar_detail_1, 0.85),
    RANK2(R.drawable.music_icon_dxstar_detail_2, 0.9),
    RANK3(R.drawable.music_icon_dxstar_detail_3, 0.93),
    RANK4(R.drawable.music_icon_dxstar_detail_4, 0.95),
    RANK5(R.drawable.music_icon_dxstar_detail_5, 0.97);

    companion object {
        fun fromDxScore(dxScore: Int, maxDxScore: Int): SongDxRank {
            val achieve = dxScore.toDouble() / maxDxScore.toDouble()
            return when {
                achieve >= RANK5.achieve -> RANK5
                achieve >= RANK4.achieve -> RANK4
                achieve >= RANK3.achieve -> RANK3
                achieve >= RANK2.achieve -> RANK2
                achieve >= RANK1.achieve -> RANK1
                else -> RANK0
            }
        }
    }
}