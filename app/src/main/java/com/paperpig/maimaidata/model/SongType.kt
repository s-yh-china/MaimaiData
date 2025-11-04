package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongType(@field:DrawableRes val icon: Int) {
    SD(R.drawable.ic_standard),
    DX(R.drawable.ic_deluxe),
    UTAGE(0)
}