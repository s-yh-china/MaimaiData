package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongFC(@DrawableRes val icon: Int) {
    NONE(R.drawable.music_icon_back),
    FC(R.drawable.music_icon_fc),
    FCP(R.drawable.music_icon_fcp),
    AP(R.drawable.music_icon_ap),
    APP(R.drawable.music_icon_app);

    companion object {
        fun fromCode(code: String): SongFC {
            return entries.find { it.name.lowercase() == code } ?: NONE
        }
    }
}