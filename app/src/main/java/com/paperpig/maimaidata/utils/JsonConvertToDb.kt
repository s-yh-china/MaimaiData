package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.AliasEntity
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.ChartStatsEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.model.ChartsResponse
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongData

object JsonConvertToDb {
    fun convertSongData(list: List<SongData>): ConversionResult {
        val songList = list.map { song ->
            SongDataEntity(
                id = song.id.toInt(),
                title = song.title,
                titleKana = song.titleKana,
                type = song.type,
                bpm = song.basicInfo.bpm,
                artist = song.basicInfo.artist,
                genre = song.basicInfo.genre,
                catCode = song.basicInfo.catcode,
                from = song.basicInfo.from,
                version = song.basicInfo.version,
                isNew = song.basicInfo.isNew,
                imageUrl = song.basicInfo.imageUrl,
                kanji = song.basicInfo.kanji,
                comment = song.basicInfo.comment,
                buddy = song.basicInfo.buddy
            )
        }

        val chartList = list.flatMap { song ->
            song.charts.mapIndexed { i, chart ->
                val notes = chart.notes
                ChartEntity(
                    songId = song.id.toInt(),
                    difficultyType = getDifficultyType(song.type, i),
                    type = song.type,
                    charter = chart.charter,
                    level = song.level[i],
                    ds = song.ds[i],
                    oldDs = song.oldDs.getOrNull(i),
                    notesTap = notes[0],
                    notesHold = notes[1],
                    notesSlide = notes[2],
                    notesTouch = if (song.type == Constants.CHART_TYPE_SD) 0 else notes[3],
                    notesBreak = if (song.type == Constants.CHART_TYPE_SD) notes[3] else notes[4],
                    notesTotal = notes.sum()
                )
            }
        }

        val aliasList = list.flatMap { song ->
            song.alias?.map { alias ->
                AliasEntity(0, song.id.toInt(), alias)
            } ?: emptyList()
        }

        return ConversionResult(songList, chartList, aliasList)
    }

    fun convertChatStats(response: ChartsResponse): List<ChartStatsEntity> {
        return response.charts.flatMap { id ->
            id.value.mapIndexed { index, it ->
                ChartStatsEntity(
                    0,
                    id.key.toInt(),
                    it.cnt,
                    it.diff,
                    index,
                    it.fitDiff,
                    it.avg,
                    it.avgDx,
                    it.stdDev,
                    it.dist,
                    it.fcDist
                )
            }
        }
    }

    private fun getDifficultyType(songType: String, index: Int): DifficultyType {
        return if (songType == Constants.CHART_TYPE_UTAGE) {
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