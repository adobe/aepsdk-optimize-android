package com.adobe.marketing.optimizeapp.odd

import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Offer
import com.adobe.marketing.mobile.optimize.OptimizeProposition
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ODDManager(
    private val decisionScopes: () -> List<DecisionScope>,
    private val dataMap: () -> Map<String, Any>,
    private val coroutineScope: CoroutineScope
) {

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private val _propositionMap = mutableMapOf<DecisionScope, OptimizeProposition>()

    private val _baseUrl = MutableStateFlow("http://10.0.2.2:3000")
    val baseUrl = _baseUrl.asStateFlow()

    private val _inputScope = MutableStateFlow("")
    val inputScope = _inputScope.asStateFlow()

    private var apiService: ApiService = createApiService()

    fun updateBaseUrl(newUrl: String) {
        _baseUrl.value = newUrl
        recreateApiService()
    }

    private fun recreateApiService() {
        apiService = createApiService()
    }

    private fun createApiService(): ApiService {
        return Retrofit.Builder()
            .baseUrl(_baseUrl.value)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .serializeNulls() // Include null values if needed
                        .create()
                )
            )
            .build().create(ApiService::class.java)
    }

    fun fetchResponse() {
        coroutineScope.launch {
            try {
                val decisionScopes: List<String> = decisionScopes().map { it.name }
                val data = dataMap()
                val event = ODDRequestBuilder.buildRequestEvent(
                    decisionScopeNames = decisionScopes,
                    data = data
                )
                coroutineScope.launch(Dispatchers.IO) {
                    val response = apiService.postData(event)
                    if (response.isSuccessful) {
                        val updatedMap = OddEventProcessor().processResponseEvent(response.body())
                        _propositionMap.clear()
                        _propositionMap.putAll(updatedMap)
                        _response.value =
                            "Success: Received Scopes {${updatedMap.keys.joinToString(", ") { it.name }}}"
                    } else {
                        _response.value = "Error: ${response.errorBody()?.string()}"
                    }
                }
            } catch (e: Exception) {
                _response.value = "Error: ${e.message}"
            }
        }
    }

    fun getProposition(matcher: String): List<Offer>? {
        val matchingKey = _propositionMap.keys.filter { it.name == matcher }
        if (matchingKey.isNotEmpty()) {
            val proposition = _propositionMap[matchingKey.first()]
            return proposition?.offers
        } else {
            return null
        }

    }

}