package com.paperpig.maimaidata.repository

import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.RecordDao
import com.paperpig.maimaidata.db.entity.RecordEntity

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
}