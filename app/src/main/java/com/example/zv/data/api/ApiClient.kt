package com.example.zv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val VIRUSTOTAL_BASE_URL = "https://www.virustotal.com/vtapi/v2/"
    private const val OTX_BASE_URL = "https://otx.alienvault.com/api/v1/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val virusTotalApi: VirusTotalApi = Retrofit.Builder()
        .baseUrl(VIRUSTOTAL_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VirusTotalApi::class.java)
    
    val otxApi: OtxApi = Retrofit.Builder()
        .baseUrl(OTX_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OtxApi::class.java)
}

object ApiKeys {
    const val VIRUSTOTAL_API_KEY = "2aaabaa0b52488673aa8499944b17a3b9a6643f5ed362f3be46134077b590665"
    const val OTX_API_KEY = "7735eedda3143d1777db24fde7eba98e78ff84f647ae5ebdc92c411c2fc4f9aa"
}

