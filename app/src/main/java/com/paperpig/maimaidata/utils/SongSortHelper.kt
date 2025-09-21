package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.utils.GroupType.ACHIEVEMENT
import com.paperpig.maimaidata.utils.GroupType.ALL
import com.paperpig.maimaidata.utils.GroupType.GENRE
import com.paperpig.maimaidata.utils.GroupType.LEVEL
import com.paperpig.maimaidata.utils.GroupType.TITLE
import com.paperpig.maimaidata.utils.GroupType.VERSION

object SongSortHelper {
    private val levelList: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "7+", "8", "8+", "9", "9+", "10", "10+", "11", "11+", "12", "12+", "13", "13+", "14", "14+", "15")
    private val genreList: List<String> = listOf()

    fun getSongClosestLocation(song: SongWithRecordEntity, group: GroupType, sort: SortType, allSongs: List<SongWithRecordEntity>): Pair<Int, Int> {
        TODO()
    }

    private fun getGroupSongs(group: GroupType, groupIndex: Int, allSongs: List<SongWithRecordEntity>): List<GameSongObject> {
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
                                record = song.records.firstOrNull { it.difficultyType == chart.difficultyType }
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

    private fun getSongGroupIndex(song: SongWithRecordEntity, difficultyIndex: Int, group: GroupType): Int {
        return when (group) {
            GENRE -> 0
            TITLE -> 0
            LEVEL -> levelList.indexOf(song.charts[difficultyIndex].level)
            VERSION -> 0
            ACHIEVEMENT -> 0
            ALL -> 0
        }
    }
}

enum class GroupType {
    GENRE, TITLE, LEVEL, VERSION, ACHIEVEMENT, ALL
}

enum class SortType {
    RECOMMENDATION, TITLE, LEVEL, RELEASE_DATE, BPM, ACHIEVEMENT, FC, FS, DX_SCORE
}