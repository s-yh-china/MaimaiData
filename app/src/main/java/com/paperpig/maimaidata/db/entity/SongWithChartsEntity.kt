package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.paperpig.maimaidata.model.DifficultyType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongWithChartsEntity(
    @Embedded val songData: SongDataEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "song_id"
    )
    val charts: List<ChartEntity>
) : Parcelable {
    @IgnoredOnParcel
    val chartsMap: Map<DifficultyType, ChartEntity> by lazy {
        charts.associateBy { it.difficultyType }
    }
}
