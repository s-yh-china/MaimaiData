package com.paperpig.maimaidata.model

import com.paperpig.maimaidata.R

enum class DifficultyType(val difficultyIndex: Int, val webDifficultyIndex: Int = difficultyIndex, val displayName: String, val color: Int) {
    BASIC(difficultyIndex = 0, displayName = "Basic", color = R.color.basic),
    ADVANCED(1, displayName = "Advance", color = R.color.advanced),
    EXPERT(2, displayName = "Expert", color = R.color.expert),
    MASTER(3, displayName = "Master", color = R.color.master),
    REMASTER(4, displayName = "Re:Master", color = R.color.remaster_border),
    UTAGE(0, 10, displayName = "宴·会·场", color = R.color.utage),
    UTAGE_PLAYER2(1, 10, displayName = "宴·会·场", color = R.color.utage),
    UNKNOWN(-1, displayName = "UNKNOWN", color = R.color.white);

    companion object {
        fun from(songType: SongType, index: Int): DifficultyType {
            return if (songType == SongType.UTAGE) {
                when (index) {
                    UTAGE.difficultyIndex -> UTAGE
                    UTAGE_PLAYER2.difficultyIndex -> UTAGE_PLAYER2
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