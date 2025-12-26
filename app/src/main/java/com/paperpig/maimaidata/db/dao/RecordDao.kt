package com.paperpig.maimaidata.db.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity

@Dao
interface RecordDao {

    /**
     * 批量替换所有记录数据。
     * @param recordList 记录列表
     * @return 操作结果
     */
    @Transaction
    fun replaceAllRecord(recordList: List<RecordEntity>): Boolean {
        try {
            clearRecord()
            insertAllRecord(recordList)
            return true
        } catch (e: Exception) {
            Log.e(AppDataBase.DATABASE_NAME, "Transaction replaceAllRecord failed: ${e.message}")
            return false
        }
    }

    @Query("SELECT * FROM record")
    fun getAllRecord(): List<RecordEntity>

    @Insert
    fun insertAllRecord(list: List<RecordEntity>)

    @Query("DELETE FROM record")
    fun clearRecord()
}