package com.lanhnh.airquality.google

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {
    companion object {

        fun create(): GoogleAPIService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(GoogleAPIService::class.java)
        }
    }
}
