package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongRank(@field:DrawableRes val icon: Int, val displayName: String) {
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

        fun achievementToRating(level: Double, achievements: Double) = achievementToRating((level * 10).toInt(), (achievements * 10000).toInt());

        fun achievementToRating(level: Int, achievements: Int): Int {
            val i = when {
                achievements >= 1005000 -> 22.4
                achievements == 1004999 -> 22.2
                achievements >= 1000000 -> 21.6
                achievements == 999999 -> 21.4
                achievements >= 995000 -> 21.1
                achievements >= 990000 -> 20.8
                achievements >= 980000 -> 20.3
                achievements >= 970000 -> 20.0
                achievements >= 940000 -> 16.8
                achievements >= 900000 -> 15.2
                achievements >= 800000 -> 13.6
                achievements >= 750000 -> 12.0
                achievements >= 700000 -> 11.2
                achievements >= 600000 -> 9.6
                achievements >= 500000 -> 8.0
                else -> 0.0
            }

            val temp = achievements.coerceAtMost(1005000) * level * i
            return (temp / 10000000).toInt()
        }
    }
}