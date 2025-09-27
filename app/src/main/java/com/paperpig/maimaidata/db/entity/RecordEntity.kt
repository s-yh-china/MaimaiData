package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongFC
import com.paperpig.maimaidata.model.SongFS
import com.paperpig.maimaidata.model.SongRank
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "record",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Parcelize
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    @ColumnInfo(name = "achievements")
    val achievements: Double,

    @ColumnInfo(name = "rate")
    val rate: SongRank,

    @ColumnInfo(name = "dx_score")
    val dxScore: Int,

    @ColumnInfo(name = "fc")
    val fc: SongFC,

    @ColumnInfo(name = "fs")
    val fs: SongFS,

    @ColumnInfo(name = "difficulty_type")
    val difficultyType: DifficultyType,
) : Parcelable

