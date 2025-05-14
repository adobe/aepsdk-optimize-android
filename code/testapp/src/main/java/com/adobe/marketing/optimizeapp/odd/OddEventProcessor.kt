package com.adobe.marketing.optimizeapp.odd

import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.OptimizeProposition

class OddEventProcessor {
    fun processResponseEvent(response: ApiResponse?) : Map<DecisionScope, OptimizeProposition> {
        val map = mutableMapOf<DecisionScope, OptimizeProposition>()
        response?.handle?.forEach { event ->
            event.payload?.forEach { payload ->
                try {
                    val proposition = OptimizeProposition.fromEventData(payload)
                    val scope = DecisionScope(proposition.scope)
                    map[scope] = proposition
                } catch (ex: Exception) {
                    // Handle the exception if needed
                    println("Error processing payload: ${ex.message}")
                }
            }
        }

        return map
    }

}