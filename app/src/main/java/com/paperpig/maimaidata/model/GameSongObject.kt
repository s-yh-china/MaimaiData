package com.paperpig.maimaidata.model

import android.os.Parcelable
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.utils.ConvertUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class GameSongObject(
    val song: SongDataEntity,
    val chart: ChartEntity,
    val record: RecordEntity?
) : Parcelable {

    @IgnoredOnParcel
    val recordOrDef: RecordEntity by lazy {
        record ?: RecordEntity(
            songId = song.id,
            achievements = 0.0,
            rate = SongRank.D,
            fc = SongFC.NONE,
            fs = SongFS.NONE,
            dxScore = 0,
            difficultyType = chart.difficultyType,
            playCount = -1
        )
    }

    fun withRating(): GameSongObjectWithRating = GameSongObjectWithRating(
        obj = this,
        rating = ConvertUtils.achievementToRating(
            (chart.internalLevel * 10).toInt(),
            (recordOrDef.achievements * 10000).toInt()
        )
    )

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

        fun formSongWithRecordClosest(song: SongWithRecordEntity, difficultyType: DifficultyType): GameSongObject {
            val closestDifficulty = song.getClosestDifficulty(difficultyType)
            return GameSongObject(
                song = song.songData,
                chart = song.chartsMap[closestDifficulty]!!,
                record = song.recordsMap[closestDifficulty]
            )
        }
    }
}

data class GameSongObjectWithRating(
    val obj: GameSongObject,
    val rating: Int
)