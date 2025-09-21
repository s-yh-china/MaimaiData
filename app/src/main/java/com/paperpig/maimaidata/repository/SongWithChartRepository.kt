package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.SongWithChartsDao
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.SpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongWithChartRepository private constructor(private val songChartDao: SongWithChartsDao) {
    companion object {
        @Volatile
        private var instance: SongWithChartRepository? = null
        fun getInstance(): SongWithChartRepository {
            return instance ?: synchronized(this) {
                instance ?: SongWithChartRepository(AppDataBase.getInstance().songWithChartDao()).also { instance = it }
            }
        }
    }

    /**
     * 更新本地歌曲谱面数据库信息
     */
    suspend fun updateDatabase(list: List<SongData>): Boolean {
        return withContext(Dispatchers.IO) {
            val convertToChartEntities = JsonConvertToDb.convertSongData(list)
            songChartDao.replaceAllSongsAndCharts(
                convertToChartEntities.songs,
                convertToChartEntities.charts,
                convertToChartEntities.aliases
            )
        }
    }

    /**
     * 获取所有歌曲和对应谱面信息
     */
    fun getAllSongWithCharts(
        includeUtage: Boolean = false,
        ascending: Boolean = false
    ): LiveData<List<SongWithChartsEntity>> {
        return songChartDao.getAllSongsWithCharts(includeUtage, ascending)
    }

    /**
     * 根据歌曲标题精确匹配歌曲
     *
     * @param songTitle 歌曲标题
     *
     * @return 匹配的歌曲列表，可能包含不同类型的铺面
     */
    fun searchSongsWithTitle(songTitle: String): List<SongWithChartsEntity> {
        return songChartDao.searchSongsByTitle(songTitle)
    }

    /**
     * 根据搜索文本、歌曲类型、版本、难度等级、流派和 定数 值搜索歌曲及其谱面信息。
     *
     * @param searchText 搜索文本，用于匹配歌曲名称。
     * @param genreList 歌曲流派列表，用于筛选特定流派的歌曲。如果为空，则不按流派筛选。
     * @param versionList 版本列表，用于筛选特定版本的歌曲。如果为空，则不按版本筛选。
     * @param selectLevel 可选的难度等级，用于筛选特定难度等级的谱面。可以为 null，表示不按难度等级筛选。
     * @param sequencing 可选的排序信息，用于排序指定难度的歌曲。会提取 "EXPERT" 或 "MASTER" 前缀进行筛选。可以为 null，表示默认排序。
     * @param internalLevel 可选的定数值，用于筛选特定定数值的谱面。可以为 null，表示不按ds值筛选。
     * @param isFavor 是否搜索收藏的歌曲，设置为true时，使用sp文件中收藏歌曲的id进行查询
     * @param isMatchAlias 是否匹配别名搜索，用于匹配歌曲别名
     * @return 包含符合搜索条件的 SongWithChartsEntity 列表的 LiveData。
     */
    fun searchSongsWithCharts(
        searchText: String,
        genreList: List<String>,
        versionList: List<String>,
        selectLevel: String?,
        sequencing: String?,
        internalLevel: Double?,
        isFavor: Boolean,
        isMatchAlias: Boolean,
        isMatchCharter: Boolean,
        isMatchSongId: Boolean
    ): LiveData<List<SongWithChartsEntity>> {
        val initialResult = songChartDao.searchSongsWithCharts(
            searchText = searchText,
            isGenreListEmpty = genreList.isEmpty(),
            genreList = genreList,
            isVersionListEmpty = versionList.isEmpty(),
            versionList = expandFromList(versionList),
            selectLevel = selectLevel,
            sequencing = getDifficultyPrefix(sequencing),
            internalLevel = internalLevel,
            isSearchFavor = isFavor,
            favIdList = SpUtil.getFavIds(),
            isMatchAlias = isMatchAlias,
            isMatchCharter = isMatchCharter,
            isMatchSongId = isMatchSongId
        )
        //根据sequencing指定难度排序
        return initialResult.map { list ->
            if (sequencing.isNullOrBlank()) {
                list.sortedByDescending { it.songData.id }
            } else {
                when (sequencing) {
                    "EXPERT-升序" -> list.sortedBy { it.chartsMap[DifficultyType.EXPERT]?.internalLevel }
                    "EXPERT-降序" -> list.sortedByDescending { it.chartsMap[DifficultyType.EXPERT]?.internalLevel }
                    "MASTER-升序" -> list.sortedBy { it.chartsMap[DifficultyType.MASTER]?.internalLevel }
                    "MASTER-降序" -> list.sortedByDescending { it.chartsMap[DifficultyType.MASTER]?.internalLevel }
                    "RE:MASTER-升序" -> list.sortedWith(remasterAscComparator)
                    "RE:MASTER-降序" -> list.sortedWith(remasterDescComparator)
                    "最高难度-升序" -> list.sortedBy { song -> song.charts.maxByOrNull { it.difficultyType.difficultyIndex }?.internalLevel }
                    "最高难度-降序" -> list.sortedByDescending { song -> song.charts.maxByOrNull { it.difficultyType.difficultyIndex }?.internalLevel }
                    else -> list.sortedByDescending { it.songData.id }
                }
            }
        }
    }

    fun getAllSong(): List<SongDataEntity> = songChartDao.getAllSong()

    private fun getDifficultyPrefix(sequencing: String?): DifficultyType? {
        return sequencing?.let {
            when {
                it.startsWith("EXPERT") -> DifficultyType.EXPERT
                it.startsWith("MASTER") -> DifficultyType.MASTER
                it.startsWith("RE:MASTER") -> DifficultyType.REMASTER
                else -> null
            }
        }
    }

    private fun expandFromList(versionList: List<String>): List<String> {
        return versionList.flatMap { item ->
            if (item == "maimai") {
                listOf(item, "$item PLUS")
            } else {
                listOf(item)
            }
        }
    }

    private val remasterComparator = Comparator<SongWithChartsEntity> { a, b ->
        when {
            a.charts.size < 5 && b.charts.size < 5 -> 0
            a.charts.size < 5 -> 1
            b.charts.size < 5 -> -1
            else -> 0
        }
    }

    private val remasterAscComparator = remasterComparator.thenBy { it.chartsMap[DifficultyType.REMASTER]?.internalLevel }
    private val remasterDescComparator = remasterComparator.thenByDescending { it.chartsMap[DifficultyType.REMASTER]?.internalLevel }
}

