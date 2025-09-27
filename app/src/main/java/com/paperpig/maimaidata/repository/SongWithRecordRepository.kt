package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.SongWithRecordDao
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity

class SongWithRecordRepository private constructor(private val songWithRecordDao: SongWithRecordDao) {
    companion object {
        @Volatile
        private var instance: SongWithRecordRepository? = null
        fun getInstance(): SongWithRecordRepository {
            return instance ?: synchronized(this) {
                instance ?: SongWithRecordRepository(AppDataBase.getInstance().songWithRecordDao()).also { instance = it }
            }
        }
    }

    fun getAllSongWithRecord(
        includeUtage: Boolean = false,
    ): LiveData<List<SongWithRecordEntity>> {
        return songWithRecordDao.getAllSongWithRecord(includeUtage)
    }
}