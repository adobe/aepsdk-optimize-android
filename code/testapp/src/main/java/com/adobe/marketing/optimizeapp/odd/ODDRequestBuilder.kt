package com.adobe.marketing.optimizeapp.odd

object ODDRequestBuilder {

    fun buildRequestEvent(
        decisionScopeNames: List<String>,
        data: Map<String, Any> = emptyMap(),
        xdm: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {

        val event = mutableMapOf<String, Any>()
        event[OddConstants.TYPE] = OddConstants.REQUEST_TYPE_ODD_FETCH

        val personalization = mutableMapOf<String, Any>()

        if(decisionScopeNames.isNotEmpty())
            personalization.put(OddConstants.OPTIMIZE_DECISION_SCOPE, decisionScopeNames)

        personalization.put(OddConstants.SEND_DISPLAY_EVENT, false)

        event[OddConstants.PERSONALIZATION] = personalization

        if(data.isNotEmpty())
            event[OddConstants.DATA] = data

        if(xdm.isNotEmpty())
            event[OddConstants.XDM] = xdm

        return event
    }

}