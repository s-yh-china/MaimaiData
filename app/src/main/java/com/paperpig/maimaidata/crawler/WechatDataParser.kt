package com.paperpig.maimaidata.crawler

import android.util.Log
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongDxRank
import com.paperpig.maimaidata.model.SongFC
import com.paperpig.maimaidata.model.SongFS
import com.paperpig.maimaidata.model.SongRank
import com.paperpig.maimaidata.model.SongType
import com.paperpig.maimaidata.repository.SongRepository
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.regex.Pattern

object WechatDataParser {
    private const val TAG = "WechatDataParser"
    private val linkCofDxScore = mapOf(
        DifficultyType.BASIC to 834,
        DifficultyType.ADVANCED to 1011,
        DifficultyType.EXPERT to 1275,
        DifficultyType.MASTER to 1839
    )
    private val icoPattern = Pattern.compile("_icon_(.*)\\.png")

    private val songRepository: SongRepository = SongRepository.getInstance()

    fun parsePageToRecordList(pageData: String, difficulty: DifficultyType): List<RecordEntity> {
        return Jsoup.parse(pageData).select("div.music_name_block.t_l.f_13.break")
            .flatMap { nameElement ->
                runCatching {
                    val siblings = nameElement.parent()?.children() ?: return@runCatching emptyList()

                    val achievements = findText(siblings, "music_score_block") { it.contains("%") }
                        ?.replace("%", "")?.toDouble() ?: return@runCatching emptyList()

                    val isUtage = difficulty == DifficultyType.UTAGE
                    val typeImg = nameElement.parent()?.parent()?.parent()?.select("img.music_kind_icon")?.firstOrNull()
                    val type = when {
                        isUtage -> SongType.UTAGE
                        typeImg?.attr("src")?.contains("standard") == true -> SongType.SD
                        typeImg?.attr("src")?.contains("dx") == true -> SongType.DX
                        else -> SongType.DX.also { Log.w(TAG, "无法获取铺面类型，推测为DX铺面") }
                    }

                    val (dxScore, maxDxScore) = findText(siblings, "music_score_block") { it.contains("/") }
                        ?.replace(",", "")?.replace(" ", "")?.split("/")
                        ?.takeIf { it.size == 2 }
                        ?.let { it[0].toInt() to it[1].toInt() } ?: return@runCatching emptyList()

                    var title = nameElement.text().ifEmpty { "　" }
                    if (title == "Link" && maxDxScore == linkCofDxScore[difficulty]) {
                        title = "Link(Cof)"
                    }

                    val songEntity = songRepository.searchSongsWithTitle(title).firstOrNull { it.type == type }
                    if (songEntity == null) {
                        Log.w(TAG, "无法获取 $title($type) ${difficulty.displayName} 的歌曲数据")
                        return@runCatching emptyList()
                    }

                    var fc = SongFC.NONE
                    var fs = SongFS.NONE
                    siblings.forEach { sibling ->
                        val img = sibling.select("img[src*=_icon_]").firstOrNull() ?: return@forEach
                        val matcher = icoPattern.matcher(img.attr("src"))
                        if (matcher.find()) {
                            matcher.group(1)?.let { code ->
                                fc = if (fc == SongFC.NONE) SongFC.fromCode(code) else fc
                                fs = if (fs == SongFS.NONE) SongFS.fromCode(code) else fs
                            }
                        }
                    }

                    val isBuddy = isUtage && songEntity.buddy == true
                    val rateValue = if (isBuddy) achievements / 2 else achievements
                    val rate = SongRank.fromAchievement(rateValue)
                    val dxRank = SongDxRank.fromDxScore(dxScore = dxScore, maxDxScore = maxDxScore)

                    val record = RecordEntity(
                        songId = songEntity.id,
                        achievements = achievements,
                        dxScore = dxScore,
                        dxRank = dxRank,
                        fc = fc,
                        fs = fs,
                        difficultyType = difficulty,
                        rate = rate,
                    )

                    if (isBuddy) {
                        listOf(record, record.copy(difficultyType = DifficultyType.UTAGE_PLAYER2))
                    } else {
                        listOf(record)
                    }
                }.getOrElse { e ->
                    Log.w(TAG, "解析记录时出错: ${e.javaClass.simpleName}: ${e.message}")
                    emptyList()
                }
            }
    }

    private fun findText(siblings: Elements, clazz: String, predicate: ((String) -> Boolean)?): String? {
        return siblings.firstOrNull { it.hasClass(clazz) && (predicate == null || predicate(it.text())) }?.text()
    }
}