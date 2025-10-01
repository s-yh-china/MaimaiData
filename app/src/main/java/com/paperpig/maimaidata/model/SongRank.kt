package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongRank(@DrawableRes val icon: Int, val displayName: String) {
    D(R.drawable.rank_d, "D"),
    C(R.drawable.rank_c, "C"),
    B(R.drawable.rank_b, "B"),
    BB(R.drawable.rank_bb, "BB"),
    BBB(R.drawable.rank_bbb, "BBB"),
    A(R.drawable.rank_a, "A"),
    AA(R.drawable.rank_aa, "AA"),
    AAA(R.drawable.rank_aaa, "AAA"),
    S(R.drawable.rank_s, "S"),
    SP(R.drawable.rank_sp, "S+"),
    SS(R.drawable.rank_ss, "SS"),
    SSP(R.drawable.rank_ssp, "SS+"),
    SSS(R.drawable.rank_sss, "SSS"),
    SSSP(R.drawable.rank_sssp, "SSS+");

    companion object {
        fun fromAchievement(achievements: Double): SongRank {
            return when {
                achievements < 50 -> D
                achievements < 60 -> C
                achievements < 70 -> B
                achievements < 75 -> BB
                achievements < 80 -> BBB
                achievements < 90 -> A
                achievements < 94 -> AA
                achievements < 97 -> AAA
                achievements < 98 -> S
                achievements < 99 -> SP
                achievements < 99.5 -> SS
                achievements < 100 -> SSP
                achievements < 100.5 -> SSS
                else -> SSSP
            }
        }
    }
}