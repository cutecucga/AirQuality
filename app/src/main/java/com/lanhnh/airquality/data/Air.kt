package com.lanhnh.airquality.data

data class Air(
    val id: Long,
    val deviceId: String,
    val dust: Double,
    val airQuality: Double,
    val time: String = "15:30:43 15/05/2020"
)
