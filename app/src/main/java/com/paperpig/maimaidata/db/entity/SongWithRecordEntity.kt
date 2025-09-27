package com.paperpig.maimaidata.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.paperpig.maimaidata.model.DifficultyType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongWithRecordEntity(
    @Embedded override val songData: SongDataEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "song_id"
    )
    override val charts: List<ChartEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "song_id"
    )
    val records: List<RecordEntity>
) : SongDetailData {
    @IgnoredOnParcel
    override val chartsMap: Map<DifficultyType, ChartEntity> by lazy {
        charts.associateBy { it.difficultyType }
    }

    @IgnoredOnParcel
    val recordsMap: Map<DifficultyType, RecordEntity> by lazy {
        records.associateBy { it.difficultyType }
    }
}
