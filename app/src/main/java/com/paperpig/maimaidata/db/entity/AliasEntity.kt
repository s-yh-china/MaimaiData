package com.paperpig.maimaidata.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "alias",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AliasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    val alias: String
)
