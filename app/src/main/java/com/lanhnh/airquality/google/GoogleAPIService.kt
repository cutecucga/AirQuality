package com.lanhnh.airquality.google

import com.lanhnh.airquality.data.model.DirectionResponses
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface GoogleAPIService {
    @GET
    fun getDirection(@Url url: String): Call<DirectionResponses>
}
