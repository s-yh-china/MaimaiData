package com.paperpig.maimaidata.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.SongDataEntity

@Dao
interface SongDao {
    @Insert
    fun insertAllSongs(songDataList: List<SongDataEntity>)

    @Query("DELETE FROM song_data")
    fun clearSongData()

    @Query("SELECT * FROM song_data")
    fun getAllSong(): List<SongDataEntity>

    /**
     * 根据歌曲标题精确匹配歌曲
     *
     * @param songTitle 歌曲标题
     *
     * @return 歌曲列表，可能包含不同类型的铺面
     */
    @Query(
        """
        SELECT * 
        FROM song_data 
        WHERE title LIKE :songTitle ESCAPE '\'
        """
    )
    fun searchSongsByTitle(songTitle: String): List<SongDataEntity>

    @Query("SELECT * FROM song_data WHERE id = :songId")
    fun getSongWithId(songId: Int): SongDataEntity?
}