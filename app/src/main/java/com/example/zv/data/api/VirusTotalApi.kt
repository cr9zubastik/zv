package com.example.zv.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface VirusTotalApi {
    @POST("file/scan")
    suspend fun scanFile(
        @Query("apikey") apiKey: String,
        @Query("file") file: String
    ): Response<VirusTotalScanResponse>
    
    @GET("file/report")
    suspend fun getFileReport(
        @Query("apikey") apiKey: String,
        @Query("resource") resource: String
    ): Response<VirusTotalReportResponse>
    
    @POST("url/scan")
    suspend fun scanUrl(
        @Query("apikey") apiKey: String,
        @Query("url") url: String
    ): Response<VirusTotalScanResponse>
    
    @GET("url/report")
    suspend fun getUrlReport(
        @Query("apikey") apiKey: String,
        @Query("resource") resource: String
    ): Response<VirusTotalReportResponse>
}

data class VirusTotalScanResponse(
    val response_code: Int,
    val verbose_msg: String,
    val resource: String? = null,
    val scan_id: String? = null
)

data class VirusTotalReportResponse(
    val response_code: Int,
    val verbose_msg: String,
    val resource: String? = null,
    val scan_id: String? = null,
    val permalink: String? = null,
    val sha256: String? = null,
    val sha1: String? = null,
    val md5: String? = null,
    val positives: Int? = null,
    val total: Int? = null,
    val scans: Map<String, ScanResult>? = null
)

data class ScanResult(
    val detected: Boolean,
    val version: String? = null,
    val result: String? = null,
    val update: String? = null
)

