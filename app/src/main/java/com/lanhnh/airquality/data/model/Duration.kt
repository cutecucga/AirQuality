package com.lanhnh.airquality.data.model

import com.google.gson.annotations.SerializedName

data class Duration(
    @SerializedName("text")
    var text: String?,
    @SerializedName("value")
    var value: Int?
)