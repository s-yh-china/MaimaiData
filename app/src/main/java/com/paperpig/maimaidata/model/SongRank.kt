package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes
import com.paperpig.maimaidata.R

enum class SongRank(@DrawableRes val icon: Int) {
    D(R.drawable.rank_d),
    C(R.drawable.rank_c),
    B(R.drawable.rank_b),
    BB(R.drawable.rank_bb),
    BBB(R.drawable.rank_bbb),
    A(R.drawable.rank_a),
    AA(R.drawable.rank_aa),
    AAA(R.drawable.rank_aaa),
    S(R.drawable.rank_s),
    SP(R.drawable.rank_sp),
    SS(R.drawable.rank_ss),
    SSP(R.drawable.rank_ssp),
    SSS(R.drawable.rank_sss),
    SSSP(R.drawable.rank_sssp);

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