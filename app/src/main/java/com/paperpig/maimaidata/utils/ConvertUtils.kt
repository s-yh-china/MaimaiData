package com.paperpig.maimaidata.utils

object ConvertUtils {
    /**
     * 通过定数和达成率计算单曲rating
     */
    fun achievementToRating(level: Int, achi: Int): Int {
        val i = when {
            achi >= 1005000 -> 22.4
            achi == 1004999 -> 22.2
            achi >= 1000000 -> 21.6
            achi == 999999 -> 21.4
            achi >= 995000 -> 21.1
            achi >= 990000 -> 20.8
            achi >= 980000 -> 20.3
            achi >= 970000 -> 20.0
            achi >= 940000 -> 16.8
            achi >= 900000 -> 15.2
            achi >= 800000 -> 13.6
            achi >= 750000 -> 12.0
            achi >= 700000 -> 11.2
            achi >= 600000 -> 9.6
            achi >= 500000 -> 8.0
            else -> 0.0
        }

        val temp = achi.coerceAtMost(1005000) * level * i
        return (temp / 10000000).toInt()
    }
}