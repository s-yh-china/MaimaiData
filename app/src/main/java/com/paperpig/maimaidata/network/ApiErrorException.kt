package com.paperpig.maimaidata.network

class ApiErrorException(
    val code: Int,
    val detail: String? = null,
    val rawBody: String? = null,
) : RuntimeException("API error: code=$code, detail=$detail")