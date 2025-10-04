package com.paperpig.maimaidata.widgets

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.paperpig.maimaidata.model.DifficultyType

object Settings {

    private lateinit var settingsPre: SharedPreferences

    fun init(context: Context) {
        if (!::settingsPre.isInitialized) {
            settingsPre = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        }
    }

    // 配置项键值对
    private const val KEY_ALIAS_SEARCH = "enable_alias_search"
    private const val DEFAULT_ALIAS_SEARCH = true

    private const val KEY_CHARTER_SEARCH = "enable_charter_search"
    private const val DEFAULT_CHARTER_SEARCH = false

    private const val KEY_SHOW_ALIAS = "enable_show_alias"
    private const val DEFAULT_SHOW_ALIAS = true

    private const val KEY_VERSION_CHECK_SKIP_SONG = "version_check_skip_song"
    private const val DEFAULT_VERSION_CHECK_SKIP_SONG = true

    private const val KEY_NICKNAME = "nickname"
    private const val DEFAULT_NICKNAME = "maimai"

    private const val KEY_SELECT_DIFFICULTIES = "select_difficulties_v2"
    private val DEFAULT_SELECT = setOf("BASIC", "ADVANCED", "EXPERT", "MASTER", "REMASTER", "UTAGE")

    fun getEnableAliasSearch() = settingsPre.getBoolean(KEY_ALIAS_SEARCH, DEFAULT_ALIAS_SEARCH)

    fun getEnableCharterSearch() = settingsPre.getBoolean(KEY_CHARTER_SEARCH, DEFAULT_CHARTER_SEARCH)

    fun getVersionCheckSkipSong() = settingsPre.getBoolean(KEY_VERSION_CHECK_SKIP_SONG, DEFAULT_VERSION_CHECK_SKIP_SONG)

    fun getEnableShowAlias() = settingsPre.getBoolean(KEY_SHOW_ALIAS, DEFAULT_SHOW_ALIAS)

    fun getNickname(): String = settingsPre.getString(KEY_NICKNAME, DEFAULT_NICKNAME).takeIf { !it.isNullOrBlank() } ?: DEFAULT_NICKNAME

    fun getUpdateDifficulty(): Set<DifficultyType> {
        val selectedSet = settingsPre.getStringSet(KEY_SELECT_DIFFICULTIES, DEFAULT_SELECT) ?: DEFAULT_SELECT
        return selectedSet.mapNotNull { enumValueOf<DifficultyType>(it) }.toSet()
    }
}