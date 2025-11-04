package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongFS(@field:DrawableRes val icon: Int) {
    NONE(R.drawable.music_icon_back),
    FS(R.drawable.music_icon_fs),
    FSP(R.drawable.music_icon_fsp),
    FDX(R.drawable.music_icon_fdx),
    FDXP(R.drawable.music_icon_fdxp);

    companion object {
        fun fromCode(code: String): SongFS {
            return entries.find { it.name.lowercase() == code } ?: NONE
        }
    }
}