package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import com.paperpig.maimaidata.model.DifficultyType

interface SongDetailData : Parcelable {
    val songData: SongDataEntity
    val charts: List<ChartEntity>
    val chartsMap: Map<DifficultyType, ChartEntity>
}