package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.AliasEntity
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.ChartStatsEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.model.ChartStatsData
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.model.SongType

object JsonConvertToDb {
    fun convertSongData(list: List<SongData>): ConversionResult {
        val songList = list.map { song ->
            SongDataEntity(
                id = song.id,
                sortId = song.sortId ?: 0,
                title = song.title,
                titleKana = song.titleKana,
                type = song.type,
                releaseTime = song.releaseTime,
                bpm = song.basicInfo.bpm,
                artist = song.basicInfo.artist,
                genre = song.basicInfo.genre,
                version = song.basicInfo.version,
                jpVersion = song.basicInfo.jpVersion,
                isNew = song.basicInfo.isNew,
                imageUrl = song.basicInfo.imageUrl,
                kanji = song.basicInfo.utageInfo?.kanji,
                comment = song.basicInfo.utageInfo?.comment,
                buddy = song.basicInfo.utageInfo?.buddy,
            )
        }

        val chartList = list.flatMap { song ->
            song.charts.mapIndexed { i, chart ->
                val notes = chart.notes
                ChartEntity(
                    songId = song.id,
                    difficultyType = getDifficultyType(song.type, i),
                    charter = chart.charter,
                    level = chart.level,
                    internalLevel = chart.internalLevel,
                    oldInternalLevel = chart.oldInternalLevel,
                    noteTap = notes.tap,
                    noteHold = notes.hold,
                    noteSlide = notes.slide,
                    noteTouch = notes.torch ?: 0,
                    noteBreak = notes.`break`,
                    noteTotal = notes.tap + notes.hold + notes.slide + notes.`break` + (notes.torch ?: 0)
                )
            }
        }

        val aliasList = list.flatMap { song ->
            song.alias.map { alias ->
                AliasEntity(0, song.id, alias)
            }
        }

        return ConversionResult(songList, chartList, aliasList)
    }

    fun convertChatStats(data: ChartStatsData): List<ChartStatsEntity> {
        return data.stats.flatMap { song ->
            song.fitDifficulty.mapIndexed { i, it ->
                ChartStatsEntity(0, song.id, i, it)
            }
        }
    }

    private fun getDifficultyType(songType: SongType, index: Int): DifficultyType {
        return if (songType == SongType.UTAGE) {
            when (index) {
                0 -> DifficultyType.UTAGE
                1 -> DifficultyType.UTAGE_PLAYER2
                else -> DifficultyType.UNKNOWN
            }
        } else {
            when (index) {
                0 -> DifficultyType.BASIC
                1 -> DifficultyType.ADVANCED
                2 -> DifficultyType.EXPERT
                3 -> DifficultyType.MASTER
                4 -> DifficultyType.REMASTER
                else -> DifficultyType.UNKNOWN
            }
        }
    }

    data class ConversionResult(
        val songs: List<SongDataEntity>,
        val charts: List<ChartEntity>,
        val aliases: List<AliasEntity>
    )
}