package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.ChartStatsDao
import com.paperpig.maimaidata.db.entity.ChartStatsEntity
import com.paperpig.maimaidata.model.DifficultyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChartStatsRepository private constructor(val chartStatsDao: ChartStatsDao) {
    companion object {
        @Volatile
        private var instance: ChartStatsRepository? = null
        fun getInstance(): ChartStatsRepository {
            return instance ?: synchronized(this) {
                instance ?: ChartStatsRepository(AppDataBase.getInstance().chartStatsDao()).also { instance = it }
            }
        }
    }

    suspend fun replaceAllChartStats(list: List<ChartStatsEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            chartStatsDao.replaceAllChartStats(list)
        }
    }

    fun getChartStatsBySongIdAndDifficulty(songId: Int, difficultyType: DifficultyType): LiveData<ChartStatsEntity> {
        return chartStatsDao.getChartStatsBySongIdAndDifficulty(songId, difficultyType)
    }
}