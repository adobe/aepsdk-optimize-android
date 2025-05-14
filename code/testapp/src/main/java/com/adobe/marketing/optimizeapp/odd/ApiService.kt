package com.adobe.marketing.optimizeapp.odd

import retrofit2.Response
import retrofit2.http.Body;
import retrofit2.http.POST;

interface ApiService {
    @POST("/")
    suspend fun postData(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse>
}