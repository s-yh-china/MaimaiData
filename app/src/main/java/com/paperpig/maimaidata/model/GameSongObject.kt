package com.paperpig.maimaidata.model

import android.os.Parcelable
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class GameSongObject(
    val song: SongDataEntity,
    val chart: ChartEntity,
    val record: RecordEntity?
) : Parcelable {
    companion object {
        fun formSongWithRecord(song: SongWithRecordEntity, difficultyType: DifficultyType): GameSongObject? {
            return song.chartsMap[difficultyType]?.let { chart ->
                GameSongObject(
                    song = song.songData,
                    chart = chart,
                    record = song.recordsMap[difficultyType]
                )
            }
        }
    }
}
