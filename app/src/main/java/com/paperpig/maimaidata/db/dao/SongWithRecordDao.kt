package com.paperpig.maimaidata.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.utils.Constants

@Dao
interface SongWithRecordDao : ChartDao, SongDao, RecordDao {
    @Query(
        """
        SELECT * FROM song_data
        WHERE (type != '${Constants.UTAGE_TYPE}')
        """
    )
    fun getAllSongWithRecord(): List<SongWithRecordEntity>
}