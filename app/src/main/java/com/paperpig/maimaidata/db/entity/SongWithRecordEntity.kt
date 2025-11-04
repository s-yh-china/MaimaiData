package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongDxRank
import com.paperpig.maimaidata.model.SongFC
import com.paperpig.maimaidata.model.SongFS
import com.paperpig.maimaidata.model.SongRank
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongWithRecordEntity(
    @Embedded val songData: SongDataEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "song_id"
    ) val charts: List<ChartEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "song_id"
    )
    val records: List<RecordEntity>
) : Parcelable {
    @IgnoredOnParcel
    val chartsMap: Map<DifficultyType, ChartEntity> by lazy {
        charts.associateBy { it.difficultyType }
    }

    @IgnoredOnParcel
    val recordsMap: Map<DifficultyType, RecordEntity> by lazy {
        records.associateBy { it.difficultyType }
    }

    fun getClosestDifficulty(difficultyType: DifficultyType): DifficultyType =
        if (charts.size <= difficultyType.difficultyIndex) {
            DifficultyType.from(songData.type, charts.size - 1)
        } else {
            difficultyType
        }

    fun getRecordOrDef(
        difficultyType: DifficultyType,
        def: RecordEntity = RecordEntity(
            songId = songData.id,
            achievements = 0.0,
            rate = SongRank.D,
            dxScore = 0,
            dxRank = SongDxRank.RANK0,
            fc = SongFC.NONE,
            fs = SongFS.NONE,
            difficultyType = difficultyType,
        )
    ): RecordEntity = recordsMap[difficultyType] ?: def
}
