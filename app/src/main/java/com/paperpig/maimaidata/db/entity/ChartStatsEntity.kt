package com.paperpig.maimaidata.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.model.DifficultyType

@Entity(
    tableName = "chart_stats",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChartStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    @ColumnInfo(name = "difficulty_type")
    val difficultyType: DifficultyType,

    @ColumnInfo(name = "fit_difficulty")
    val fitDifficulty: Double,
)