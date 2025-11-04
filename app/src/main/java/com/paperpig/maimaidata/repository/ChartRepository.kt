package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.ChartDao
import com.paperpig.maimaidata.model.MaxNotesStats

class ChartRepository private constructor(private val chartDao: ChartDao) {
    companion object {
        @Volatile
        private var instance: ChartRepository? = null
        fun getInstance(): ChartRepository {
            return instance ?: synchronized(this) {
                instance ?: ChartRepository(AppDataBase.getInstance().chartDao()).also { instance = it }
            }
        }
    }

    fun getMaxNotes(): LiveData<MaxNotesStats> {
        return chartDao.getMaxNotes()
    }
}
