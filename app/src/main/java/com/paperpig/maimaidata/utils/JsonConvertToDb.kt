package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.AliasEntity
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.ChartStatsEntity
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.model.ChartAliasData
import com.paperpig.maimaidata.model.ChartStatsData
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.model.SongFC
import com.paperpig.maimaidata.model.SongFS
import com.paperpig.maimaidata.model.SongRank
import com.paperpig.maimaidata.model.SongType
import com.paperpig.maimaidata.model.api.UserMusicDataModel
import com.paperpig.maimaidata.repository.SongRepository

object JsonConvertToDb {

    const val DATA_STRUCTURE_VERSION = 3

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
                    difficultyType = DifficultyType.from(song.type, i),
                    charter = chart.charter,
                    level = chart.level,
                    internalLevel = chart.internalLevel,
                    oldInternalLevel = chart.oldInternalLevel,
                    noteTap = notes.tap,
                    noteHold = notes.hold,
                    noteSlide = notes.slide,
                    noteTouch = notes.touch ?: 0,
                    noteBreak = notes.`break`,
                    noteTotal = notes.tap + notes.hold + notes.slide + notes.`break` + (notes.touch ?: 0)
                )
            }
        }

        return ConversionResult(songList, chartList)
    }

    fun convertChartStats(data: ChartStatsData, allSongs: List<SongDataEntity>): List<ChartStatsEntity> {
        return data.stats.flatMap { song ->
            allSongs.firstOrNull { it.id == song.id }?.let { songData ->
                song.fitDifficulty.mapIndexed { i, it ->
                    ChartStatsEntity(songId = song.id, difficultyType = DifficultyType.from(songData.type, i), fitDifficulty = it)
                }
            }.orEmpty()
        }
    }

    fun convertChartAlias(data: ChartAliasData): List<AliasEntity> {
        return data.aliases.flatMap { (id, alias) -> alias.map { AliasEntity(songId = id, alias = it) } }
    }

    fun convertUserRecordData(data: UserMusicDataModel, selectDifficulties: Set<DifficultyType>): List<RecordEntity> {
        return data.music.flatMap { record ->
            SongRepository.getInstance().getSongWithId(record.musicId)?.let { song ->
                val difficultyType = DifficultyType.from(song.type, record.level)
                if (difficultyType in selectDifficulties) {
                    val isBuddy = song.type == SongType.UTAGE && song.buddy == true

                    val record = RecordEntity(
                        songId = record.musicId,
                        achievements = record.achievement,
                        dxScore = record.dxScore,
                        fc = SongFC.entries[record.fc],
                        fs = SongFS.entries[if (record.fs == 5) 0 else record.fs],
                        rate = SongRank.fromAchievement(if (isBuddy) record.achievement / 2 else record.achievement),
                        difficultyType = difficultyType,
                        playCount = record.playCount
                    )

                    if (isBuddy) {
                        listOf(record, record.copy(difficultyType = DifficultyType.UTAGE_PLAYER2))
                    } else {
                        listOf(record)
                    }
                } else {
                    emptyList()
                }

            } ?: emptyList()
        }
    }

    data class ConversionResult(
        val songs: List<SongDataEntity>,
        val charts: List<ChartEntity>
    )
}