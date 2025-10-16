package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class DifficultyType(
    val difficultyIndex: Int,
    val webDifficultyIndex: Int = difficultyIndex,
    val displayName: String,
    val color: Int,
    val shadowColor: Int,
    val backgroundColor: Int,
    @DrawableRes val rtsongDrawable: Int,
    @DrawableRes val ratingBoard: Int,
    @DrawableRes val displayDrawable: Int,
) {
    BASIC(
        difficultyIndex = 0,
        displayName = "Basic",
        color = R.color.basic,
        shadowColor = R.color.player_rtsong_bsc_dark,
        backgroundColor = R.color.player_rtsong_bsc_main,
        rtsongDrawable = R.drawable.rtsong_difficulty_basic,
        ratingBoard = R.drawable.rating_board_basic,
        displayDrawable = R.drawable.difficulty_basic
    ),
    ADVANCED(
        difficultyIndex = 1,
        displayName = "Advance",
        color = R.color.advanced,
        shadowColor = R.color.player_rtsong_adv_dark,
        backgroundColor = R.color.player_rtsong_adv_main,
        rtsongDrawable = R.drawable.rtsong_difficulty_advanced,
        ratingBoard = R.drawable.rating_board_advance,
        displayDrawable = R.drawable.difficulty_advanced
    ),
    EXPERT(
        difficultyIndex = 2,
        displayName = "Expert",
        color = R.color.expert,
        shadowColor = R.color.player_rtsong_exp_dark,
        backgroundColor = R.color.player_rtsong_exp_main,
        rtsongDrawable = R.drawable.rtsong_difficulty_expert,
        ratingBoard = R.drawable.rating_board_expert,
        displayDrawable = R.drawable.difficulty_expert
    ),
    MASTER(
        difficultyIndex = 3,
        displayName = "Master",
        color = R.color.master,
        shadowColor = R.color.player_rtsong_mst_dark,
        backgroundColor = R.color.player_rtsong_mst_main,
        rtsongDrawable = R.drawable.rtsong_difficulty_master,
        ratingBoard = R.drawable.rating_board_master,
        displayDrawable = R.drawable.difficulty_master
    ),
    REMASTER(
        difficultyIndex = 4,
        displayName = "Re:Master",
        color = R.color.remaster_border,
        shadowColor = R.color.player_rtsong_rem_dark,
        backgroundColor = R.color.player_rtsong_rem_main,
        rtsongDrawable = R.drawable.rtsong_difficulty_remaster,
        ratingBoard = R.drawable.rating_board_remaster,
        displayDrawable = R.drawable.difficulty_remaster
    ),
    UTAGE(
        difficultyIndex = 0,
        webDifficultyIndex = 10,
        displayName = "宴·会·场",
        color = R.color.utage,
        shadowColor = R.color.utage,
        backgroundColor = R.color.utage,
        rtsongDrawable = 0,
        ratingBoard = 0,
        displayDrawable = 0
    ),
    UTAGE_PLAYER2(
        difficultyIndex = 1,
        webDifficultyIndex = 10,
        displayName = "宴·会·场",
        color = R.color.utage,
        shadowColor = R.color.utage,
        backgroundColor = R.color.utage,
        rtsongDrawable = 0,
        ratingBoard = 0,
        displayDrawable = 0
    ),
    UNKNOWN(difficultyIndex = -1, displayName = "UNKNOWN", color = R.color.white, shadowColor = R.color.white, backgroundColor = R.color.white, rtsongDrawable = 0, ratingBoard = 0, displayDrawable = 0);

    companion object {
        fun from(songType: SongType, index: Int): DifficultyType {
            return if (songType == SongType.UTAGE) {
                when (index) {
                    UTAGE.difficultyIndex -> UTAGE
                    UTAGE_PLAYER2.difficultyIndex -> UTAGE_PLAYER2
                    UTAGE.webDifficultyIndex -> UTAGE
                    else -> UNKNOWN
                }
            } else {
                when (index) {
                    BASIC.difficultyIndex -> BASIC
                    ADVANCED.difficultyIndex -> ADVANCED
                    EXPERT.difficultyIndex -> EXPERT
                    MASTER.difficultyIndex -> MASTER
                    REMASTER.difficultyIndex -> REMASTER
                    else -> UNKNOWN
                }
            }
        }
    }
}