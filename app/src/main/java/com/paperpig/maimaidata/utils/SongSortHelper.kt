package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.model.SongRank
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
import com.paperpig.maimaidata.utils.SortType.LEVEL_INDEX
import com.paperpig.maimaidata.utils.SortType.NAME
import com.paperpig.maimaidata.utils.SortType.RANK
import com.paperpig.maimaidata.utils.SortType.RECOMMENDATION
import com.paperpig.maimaidata.utils.SortType.RELEASE_TIME
import com.paperpig.maimaidata.widgets.Settings

data class ClosestLocation(
    val index: Int,
    val group: GroupType,
    val sort: SortType,
    val groupName: String,
    val difficulty: DifficultyType,
    val isReversed: Boolean
)

object SongSortHelper {
    private val disableRankSortType = listOf(RECOMMENDATION, NAME, LEVEL_INDEX, RELEASE_TIME, BPM)
    private val enabledGroupTypes = listOf(LEVEL, VERSION)

    private val levelList = listOf("1", "2", "3", "4", "5", "6", "6+", "7", "7+", "8", "8+", "9", "9+", "10", "10+", "11", "11+", "12", "12+", "13", "13+", "14", "14+", "15")
    private val genreList = listOf("流行&动漫", "niconico & VOCALOID", "东方Project", "其他游戏", "舞萌", "音击&中二节奏")
    private val versionList = listOf("maimai", "maimai PLUS", "GreeN", "GreeN PLUS", "ORANGE", "ORANGE PLUS", "PiNK", "PiNK PLUS", "MURASAKi", "MURASAKi PLUS", "MiLK", "MiLK PLUS", "FiNALE", "舞萌DX", "舞萌DX 2021", "舞萌DX 2022", "舞萌DX 2023", "舞萌DX 2024", "舞萌DX 2025")

    private val LEVEL_INDEX_MAP = levelList.withIndex().associate { it.value to it.index }

    fun getSongClosestLocation(song: SongWithRecordEntity, allSongs: List<SongWithRecordEntity>): ClosestLocation {
        var minClosestIndex = Int.MAX_VALUE
        var bestLocation: ClosestLocation? = null

        enabledGroupTypes.forEach { group ->
            song.charts.forEach { chart ->
                val difficultyType = chart.difficultyType
                val currentGroup = getSongGroupIndex(song, difficultyType, group)
                val currentGroupIndex = currentGroup.first

                if (currentGroupIndex != -1) {
                    val groupSongs = getGroupSongs(group, currentGroupIndex, difficultyType, allSongs)
                    (if (Settings.getSongFindDisableRank()) disableRankSortType else SortType.entries)
                        .associateWith { sortType -> sortSong(sortType, group, groupSongs) }
                        .forEach { (sortType, sortedGroup) ->
                            val targetSongObject = GameSongObject(
                                song = song.songData,
                                chart = chart,
                                record = song.recordsMap[difficultyType]
                            )

                            val songIndex = sortedGroup.indexOfFirst {
                                it.song.id == targetSongObject.song.id && it.chart.difficultyType == targetSongObject.chart.difficultyType
                            }

                            if (songIndex != -1) {
                                val groupSize = sortedGroup.size
                                val distanceFromEnd = groupSize - 1 - songIndex

                                val closestIndex = minOf(songIndex, distanceFromEnd)
                                val reversed = distanceFromEnd < songIndex

                                if (closestIndex < minClosestIndex) {
                                    minClosestIndex = closestIndex
                                    bestLocation = ClosestLocation(
                                        group = group,
                                        groupName = currentGroup.second,
                                        index = closestIndex,
                                        sort = sortType,
                                        difficulty = difficultyType,
                                        isReversed = reversed
                                    )
                                }
                            }
                        }
                }
            }
        }

        return bestLocation ?: ClosestLocation(
            group = ALL,
            groupName = "?",
            index = 0,
            sort = RECOMMENDATION,
            difficulty = song.charts.firstOrNull()?.difficultyType ?: DifficultyType.BASIC,
            isReversed = false
        )
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

            GENRE -> {
                val targetGenre = genreList[groupIndex]
                allSongs
                    .filter { it.songData.genre == targetGenre }
                    .map { GameSongObject.formSongWithRecordClosest(it, difficultyType) }
            }

            VERSION -> {
                val targetVersion = versionList[groupIndex]
                allSongs
                    .filter { it.songData.version == targetVersion }
                    .map { GameSongObject.formSongWithRecordClosest(it, difficultyType) }
            }

            TITLE -> {
                val targetGroup = TitleGroup.entries[groupIndex]
                allSongs
                    .filter {
                        if (targetGroup != TitleGroup.OTHER) {
                            it.songData.titleKana.first() in targetGroup.startChar..targetGroup.endChar
                        } else {
                            it.songData.titleKana.first() !in TitleGroup.ALL_NON_OTHER_CHARS
                        }
                    }
                    .map { GameSongObject.formSongWithRecordClosest(it, difficultyType) }
            }

            ACHIEVEMENT -> {
                val targetRank = SongRank.entries[groupIndex]
                allSongs.flatMap { song ->
                    song.charts
                        .filter { chart ->
                            val record = song.getRecordOrDef(chart.difficultyType)
                            @Suppress("KotlinConstantConditions") // Android Studio bug
                            when (targetRank) {
                                SongRank.BBB -> record.rate <= SongRank.BBB
                                SongRank.AAA -> record.rate >= SongRank.A && record.rate <= SongRank.AAA
                                else -> targetRank == record.rate
                            }
                        }
                        .map { chart ->
                            GameSongObject(
                                song = song.songData,
                                chart = chart,
                                record = song.recordsMap[chart.difficultyType]
                            )
                        }
                }
            }

            ALL -> allSongs.map { GameSongObject.formSongWithRecordClosest(it, difficultyType) }
        }
    }

    private fun getSongGroupIndex(song: SongWithRecordEntity, difficultyType: DifficultyType, group: GroupType): Pair<Int, String> {
        return when (group) {
            GENRE -> genreList.indexOf(song.songData.genre) to song.songData.genre
            LEVEL -> LEVEL_INDEX_MAP[song.chartsMap[difficultyType]!!.level]!! to song.chartsMap[difficultyType]!!.level
            VERSION -> versionList.indexOf(song.songData.version) to song.songData.version

            TITLE -> {
                val group = TitleGroup.entries.find { group -> song.songData.titleKana.first() in group.startChar..group.endChar } ?: TitleGroup.OTHER
                group.ordinal to group.displayName
            }

            ACHIEVEMENT -> {
                val rank = song.getRecordOrDef(difficultyType).rate
                when {
                    rank <= SongRank.BBB -> SongRank.BBB.ordinal to SongRank.BBB.displayName
                    rank <= SongRank.AAA -> SongRank.AAA.ordinal to SongRank.AAA.displayName
                    else -> rank.ordinal to rank.displayName
                }
            }

            ALL -> 0 to "全部乐曲"
        }
    }

    private fun sortSong(sort: SortType, group: GroupType, songs: List<GameSongObject>): List<GameSongObject> {
        return when (sort) {
            RECOMMENDATION -> {
                when (group) {
                    GENRE, ALL -> songs.sortedWith(compareBy({ it.song.sortId }, { it.chart.difficultyType }))
                    TITLE -> sortSong(NAME, group, songs)
                    LEVEL -> sortSong(LEVEL_INDEX, group, songs)
                    VERSION -> sortSong(RELEASE_TIME, group, songs)
                    ACHIEVEMENT -> sortSong(RANK, group, songs)
                }
            }

            NAME -> {
                songs.sortedWith(
                    compareBy(
                        {
                            when (it.song.titleKana.first()) {
                                in TitleGroup.H_A_O.startChar..TitleGroup.H_WA_N.endChar -> 1
                                in TitleGroup.A_A_D.startChar..TitleGroup.A_T_Z.endChar -> 2
                                else -> 3
                            }
                        },
                        { it.song.titleKana },
                        { it.song.sortId }
                    )
                )
            }

            LEVEL_INDEX -> {
                songs.sortedWith(compareBy({ LEVEL_INDEX_MAP[it.chart.level]!! }, { it.song.sortId }))
            }

            RELEASE_TIME -> songs.sortedWith(compareBy({ it.song.releaseTime }, { it.song.sortId }))
            BPM -> songs.sortedWith(compareBy({ it.song.bpm }, { it.song.sortId }))

            RANK -> {
                songs.sortedWith(Comparator { a, b ->
                    val aRecord = a.record
                    val bRecord = b.record

                    when {
                        aRecord != null && bRecord == null -> -1
                        aRecord == null && bRecord != null -> 1

                        aRecord == null -> {
                            val diffComp = a.chart.difficultyType.compareTo(b.chart.difficultyType)
                            if (diffComp != 0) diffComp else a.song.sortId.compareTo(b.song.sortId)
                        }

                        else -> {
                            val scoreComp = aRecord.achievements.compareTo(bRecord!!.achievements)
                            if (scoreComp != 0) scoreComp else a.song.sortId.compareTo(b.song.sortId)
                        }
                    }
                })
            }

            FC -> songs.sortedWith(compareBy({ it.recordOrDef.fc }, { LEVEL_INDEX_MAP[it.chart.level] }, { it.recordOrDef.achievements }, { it.song.sortId }))
            FS -> songs.sortedWith(compareBy({ it.recordOrDef.fs }, { LEVEL_INDEX_MAP[it.chart.level] }, { it.recordOrDef.achievements }, { it.song.sortId }))
            DX_SCORE -> songs.sortedWith(compareBy({ it.recordOrDef.dxScore * 100 / (it.chart.noteTotal * 3) }, { LEVEL_INDEX_MAP[it.chart.level] }, { it.recordOrDef.achievements }, { it.song.sortId }))
        }
    }
}

enum class GroupType(val displayName: String) {
    GENRE("流派"),
    TITLE("乐曲名"),
    LEVEL("等级"),
    VERSION("版本"),
    ACHIEVEMENT("评价"),
    ALL("所有乐曲")
}

enum class SortType(val displayName: String) {
    RECOMMENDATION("推荐度"),
    NAME("乐曲名"),
    LEVEL_INDEX("等级"),
    RELEASE_TIME("添加日期"),
    BPM("BPM"),
    RANK("评价"),
    FC("AP/FC图标"),
    FS("同步率"),
    DX_SCORE("DX分数")
}

enum class TitleGroup(val displayName: String, val startChar: Char, val endChar: Char) {
    H_A_O("あいうえお", 'ア', 'オ'),
    H_KA_KO("かきくけこ", 'カ', 'コ'),
    H_SA_SO("さしすせそ", 'サ', 'ソ'),
    H_TA_TO("たちつてと", 'タ', 'ト'),
    H_NA_NO("なにぬねの", 'ナ', 'ノ'),
    H_HA_HO("はひふへほ", 'ハ', 'ホ'),
    H_MA_MO("まみむめも", 'マ', 'モ'),
    H_YA_YO("やゆよ", 'ヤ', 'ヨ'),
    H_RA_RO("らりるれろ", 'ラ', 'ロ'),
    H_WA_N("わをん", 'ワ', 'ン'),
    A_A_D("ABCD", 'A', 'D'),
    A_E_J("EFGHIJ", 'E', 'J'),
    A_K_O("KLMNO", 'K', 'O'),
    A_P_S("PQRS", 'P', 'S'),
    A_T_Z("TUVWXYZ", 'T', 'Z'),
    OTHER("数字・其他", '0', '9');

    companion object {
        val ALL_NON_OTHER_CHARS: Set<Char> by lazy {
            val allChars = mutableSetOf<Char>()
            for (group in entries) {
                if (group != OTHER) {
                    for (char in group.startChar..group.endChar) {
                        allChars.add(char)
                    }
                }
            }
            allChars.toSet()
        }
    }
}