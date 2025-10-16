package com.paperpig.maimaidata.repository

import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.SongDao
import com.paperpig.maimaidata.db.entity.SongDataEntity

class SongRepository private constructor(private val songDao: SongDao) {
    companion object {
        @Volatile
        private var instance: SongRepository? = null
        fun getInstance(): SongRepository {
            return instance ?: synchronized(this) {
                instance ?: SongRepository(AppDataBase.getInstance().songDao()).also { instance = it }
            }
        }
    }

    /**
     * 根据歌曲标题精确匹配歌曲
     *
     * @param songTitle 歌曲标题
     *
     * @return 匹配的歌曲列表，可能包含不同类型的铺面
     */
    fun searchSongsWithTitle(songTitle: String): List<SongDataEntity> {
        return songDao.searchSongsByTitle(songTitle)
    }

    fun getSongWithId(songId: Int): SongDataEntity? {
        return songDao.getSongWithId(songId)
    }
}