package com.paperpig.maimaidata.crawler

import android.util.Log
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.model.DifficultyType
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
        val records = mutableListOf<RecordEntity>()
        for (nameElement in Jsoup.parse(pageData).select("div.music_name_block.t_l.f_13.break")) {
            try {
                var title = nameElement.text().ifEmpty { "　" }
                val isUtage = difficulty == DifficultyType.UTAGE
                val siblings = nameElement.parent()?.children() ?: continue

                val achievements = findText(siblings, "music_score_block") { it.contains("%") }?.replace("%", "")?.toDouble() ?: continue
                val typeImg = nameElement.parent()?.parent()?.parent()?.select("img.music_kind_icon")?.firstOrNull()
                val type = when {
                    isUtage -> SongType.UTAGE
                    typeImg?.attr("src")?.contains("standard") == true -> SongType.SD
                    typeImg?.attr("src")?.contains("dx") == true -> SongType.DX
                    else -> SongType.DX.also {
                        Log.w(TAG, "无法获取铺面类型，推测为DX铺面")
                    }
                }

                val dxScoreText = findText(siblings, "music_score_block") { it.contains("/") } ?: continue
                val dxScoreParts = dxScoreText.replace(",", "").replace(" ", "").split("/")
                val dxScore = dxScoreParts[0].toInt()
                val dxMaxScore = dxScoreParts[1].toInt()
                if (title == "Link" && dxMaxScore == linkCofDxScore[difficulty]) {
                    title = "Link(Cof)"
                }

                val songEntity = songRepository.searchSongsWithTitle(title).firstOrNull { it.type == type }
                if (songEntity == null) {
                    Log.w(TAG, "无法获取 $title($type) ${difficulty.displayName} 的歌曲数据")
                    continue
                }

                val isBuddy = isUtage && songEntity.buddy == true
                val rate = SongRank.fromAchievement(if (isBuddy) achievements / 2 else achievements)

                var fc = SongFC.NONE
                var fs = SongFS.NONE
                for (sibling in siblings) {
                    val img = sibling.select("img[src*=_icon_]").firstOrNull() ?: continue
                    val matcher = icoPattern.matcher(img.attr("src"))
                    if (matcher.find()) {
                        matcher.group(1)?.let {
                            fc = if (fc == SongFC.NONE) SongFC.fromCode(it) else fc
                            fs = if (fs == SongFS.NONE) SongFS.fromCode(it) else fs
                        }
                    }
                }

                records.add(
                    RecordEntity(
                        songId = songEntity.id,
                        achievements = achievements,
                        dxScore = dxScore,
                        fc = fc,
                        fs = fs,
                        difficultyType = difficulty,
                        rate = rate
                    )
                )
                if (isBuddy) {
                    records.add(
                        RecordEntity(
                            songId = songEntity.id,
                            achievements = achievements,
                            dxScore = dxScore,
                            fc = fc,
                            fs = fs,
                            difficultyType = DifficultyType.UTAGE_PLAYER2,
                            rate = rate
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "解析记录时出错: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        return records
    }

    private fun findText(siblings: Elements, clazz: String, predicate: ((String) -> Boolean)?): String? {
        return siblings.firstOrNull { it.hasClass(clazz) && (predicate == null || predicate(it.text())) }?.text()
    }
}