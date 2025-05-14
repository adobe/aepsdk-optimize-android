package com.adobe.marketing.optimizeapp.odd

data class ApiResponse(
    val requestId: String?,
    val handle: List<Handle>?
)

data class Handle(
    val payload: List<Map<String, Any>>?,
    val type: String?,
    val eventIndex: Int? = null
)