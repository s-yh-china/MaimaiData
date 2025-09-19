package com.paperpig.maimaidata.model

data class MaxNotesStats(
    val tap: Int,
    val hold: Int,
    val slide: Int,
    val touch: Int,
    val `break`: Int,
    val total: Int
)