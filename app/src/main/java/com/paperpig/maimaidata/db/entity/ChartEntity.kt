package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.model.DifficultyType
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "chart",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    val charter: String,

    val level: String,

    @ColumnInfo(name = "internal_level")
    val internalLevel: Double,

    @ColumnInfo(name = "old_internal_level")
    val oldInternalLevel: Double?,

    @ColumnInfo(name = "difficulty_type")
    val difficultyType: DifficultyType,

    @ColumnInfo(name = "note_tap")
    val noteTap: Int,

    @ColumnInfo(name = "note_hold")
    val noteHold: Int,

    @ColumnInfo(name = "note_slide")
    val noteSlide: Int,

    @ColumnInfo(name = "note_touch")
    val noteTouch: Int,

    @ColumnInfo(name = "note_break")
    val noteBreak: Int,

    @ColumnInfo(name = "note_total")
    val noteTotal: Int
) : Parcelable

