package com.paperpig.maimaidata.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.utils.Constants

@Dao
interface SongWithRecordDao : ChartDao, SongDao, RecordDao {
    @Query(
        """
        SELECT * FROM song_data
        WHERE (:includeUtage = 1 OR type != '${Constants.UTAGE_TYPE}')
        """
    )
    fun getAllSongWithRecord(
        includeUtage: Boolean = false
    ): LiveData<List<SongWithRecordEntity>>
}