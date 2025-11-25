package com.example.zv.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OtxApi {
    @GET("indicators/file/{hash}/general")
    suspend fun checkFileHash(
        @Header("X-OTX-API-KEY") apiKey: String,
        @Path("hash") hash: String
    ): Response<OtxResponse>
    
    @GET("indicators/url/{url}/general")
    suspend fun checkUrl(
        @Header("X-OTX-API-KEY") apiKey: String,
        @Path("url") url: String
    ): Response<OtxResponse>
}

data class OtxResponse(
    val pulse_count: Int,
    val indicator: String,
    val type: String,
    val pulses: List<Pulse>? = null
)

data class Pulse(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String>? = null
)

