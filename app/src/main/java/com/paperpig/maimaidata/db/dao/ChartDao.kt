package com.paperpig.maimaidata.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.model.MaxNotesStats

@Dao
interface ChartDao {

    /**
     * 查询 chart 表中各类音符的最大值（排除 UTAGE 和 UTAGE_PLAYER2）。
     * 返回一个包含最大值的 MaxNotesStats。
     */
    @Query(
        """
        SELECT 
            MAX(note_tap) AS tap,
            MAX(note_hold) AS hold,
            MAX(note_slide) AS slide,
            MAX(note_touch) AS touch,
            MAX(note_break) AS `break`,
            MAX(note_total) AS total
        FROM chart
        WHERE difficulty_type NOT IN ('UTAGE', 'UTAGE_PLAYER2')
    """
    )
    fun getMaxNotes(): LiveData<MaxNotesStats>

    @Insert
    fun insertAllCharts(chartList: List<ChartEntity>)

    @Query("DELETE FROM chart")
    fun clearCharts()
}