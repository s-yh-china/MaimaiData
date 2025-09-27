package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.model.SongFC
import com.paperpig.maimaidata.model.SongFS
import com.paperpig.maimaidata.utils.GroupType.ACHIEVEMENT
import com.paperpig.maimaidata.utils.GroupType.ALL
import com.paperpig.maimaidata.utils.GroupType.GENRE
import com.paperpig.maimaidata.utils.GroupType.LEVEL
import com.paperpig.maimaidata.utils.GroupType.TITLE
import com.paperpig.maimaidata.utils.GroupType.VERSION
import com.paperpig.maimaidata.utils.SortType.BPM
import com.paperpig.maimaidata.utils.SortType.DX_SCORE
import com.paperpig.maimaidata.utils.SortType.FC
import com.paperpig.maimaidata.utils.SortType.FS
import com.paperpig.maimaidata.utils.SortType.RECOMMENDATION
import com.paperpig.maimaidata.utils.SortType.RELEASE_DATE

object SongSortHelper {
    private val levelList: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "7+", "8", "8+", "9", "9+", "10", "10+", "11", "11+", "12", "12+", "13", "13+", "14", "14+", "15")
    private val genreList: List<String> = listOf()

    fun getSongClosestLocation(song: SongWithRecordEntity, group: GroupType, sort: SortType, allSongs: List<SongWithRecordEntity>): Pair<Int, Int> {
        TODO()
    }

    private fun getGroupSongs(group: GroupType, groupIndex: Int, difficultyType: DifficultyType, allSongs: List<SongWithRecordEntity>): List<GameSongObject> {
        return when (group) {
            LEVEL -> {
                val targetLevel = levelList[groupIndex]
                allSongs.flatMap { song ->
                    song.charts
                        .filter { chart -> chart.level == targetLevel }
                        .map { chart ->
                            GameSongObject(
                                song = song.songData,
                                chart = chart,
                                record = song.recordsMap[chart.difficultyType]
                            )
                        }
                }
            }

            GENRE -> TODO()
            TITLE -> TODO()
            VERSION -> TODO()
            ACHIEVEMENT -> TODO()
            ALL -> TODO()
        }
    }

    private fun getSongGroupIndex(song: SongWithRecordEntity, difficultyType: DifficultyType, group: GroupType): Int {
        return when (group) {
            GENRE -> 0
            TITLE -> 0
            LEVEL -> levelList.indexOf(song.chartsMap[difficultyType]?.level)
            VERSION -> 0
            ACHIEVEMENT -> 0
            ALL -> 0
        }
    }

    private fun sortSong(sort: SortType, songs: List<GameSongObject>): List<GameSongObject> {
        return when (sort) {
            RECOMMENDATION -> songs.sortedBy { it.song.sortId }
            SortType.TITLE -> songs.sortedWith(compareBy({ it.song.titleKana }, { it.song.sortId }))
            SortType.LEVEL -> songs.sortedWith(compareBy({ it.chart.level }, { it.song.sortId }))
            RELEASE_DATE -> songs.sortedWith(compareBy({ it.song.releaseTime }, { it.song.sortId }))
            BPM -> songs.sortedWith(compareBy({ it.song.bpm }, { it.song.sortId }))
            SortType.ACHIEVEMENT -> songs.sortedWith(compareBy({ it.record?.achievements ?: 0.0 }, { it.song.sortId }))
            FC -> songs.sortedWith(compareBy({ it.record?.fc ?: SongFC.NONE }, { it.song.sortId }))
            FS -> songs.sortedWith(compareBy({ it.record?.fs ?: SongFS.NONE }, { it.song.sortId }))
            DX_SCORE -> songs.sortedWith(compareBy({ it.record?.dxScore ?: 0 }, { it.song.sortId })) // TODO 对吗
        }
    }
}

enum class GroupType {
    GENRE, TITLE, LEVEL, VERSION, ACHIEVEMENT, ALL
}

enum class SortType {
    RECOMMENDATION, TITLE, LEVEL, RELEASE_DATE, BPM, ACHIEVEMENT, FC, FS, DX_SCORE
}