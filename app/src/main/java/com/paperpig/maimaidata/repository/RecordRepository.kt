package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.RecordDao
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.model.DifficultyType

class RecordRepository private constructor(private val recordDao: RecordDao) {
    companion object {
        @Volatile
        private var instance: RecordRepository? = null
        fun getInstance(): RecordRepository {
            return instance ?: synchronized(this) {
                instance ?: RecordRepository(AppDataBase.getInstance().recordDao()).also { instance = it }
            }
        }
    }

    /**
     * 更新本地成绩数据库
     */
    fun replaceAllRecord(list: List<RecordEntity>): Boolean {
        return recordDao.replaceAllRecord(list)
    }

    /**
     * 获取所有成绩
     */
    fun getAllRecord(): LiveData<List<RecordEntity>> {
        return recordDao.getAllRecords()
    }

    fun getRecordsByDifficulty(difficultyType: DifficultyType): LiveData<List<RecordEntity>> {
        return recordDao.getRecordsByDifficulty(difficultyType)
    }

    /**
     * 根据歌曲ID获取成绩
     * @param songId 歌曲ID
     * @return 成绩列表
     */
    fun getRecordsBySongId(songId: Int): LiveData<List<RecordEntity>> {
        return recordDao.getRecordsBySongId(songId)
    }
}