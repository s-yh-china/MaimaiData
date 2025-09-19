package com.paperpig.maimaidata.db.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.ChartStatsEntity

@Dao
interface ChartStatsDao {

    @Transaction
    fun replaceAllChartStats(chartStatsList: List<ChartStatsEntity>): Boolean {
        try {
            clearChartStats()
            insertAllChartStats(chartStatsList)
            return true
        } catch (e: Exception) {
            Log.e(AppDataBase.DATABASE_NAME, "Transaction replaceAllChartStats failed: ${e.message}")
            return false
        }
    }

    @Query("SELECT * FROM chart_stats WHERE song_id = :songId AND level_index = :index")
    fun getChartStatsBySongIdAndDifficultyIndex(songId: Int, index: Int): LiveData<ChartStatsEntity>

    @Insert
    fun insertAllChartStats(list: List<ChartStatsEntity>)

    @Query("DELETE FROM chart_stats")
    fun clearChartStats()
}