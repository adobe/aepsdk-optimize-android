/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.AdobeCallback
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.DataReader
import com.adobe.marketing.mobile.util.DataReaderException

object Optimize {

    private const val SELF_TAG: String = "Optimize"

    @JvmField
    val EXTENSION: Class<out Extension?> = OptimizeExtension::class.java

    @JvmStatic
    fun extensionVersion(): String = OptimizeConstants.EXTENSION_VERSION

    @JvmStatic
    fun onPropositionsUpdate(callback: AdobeCallback<Map<DecisionScope, OptimizeProposition>>) {
        MobileCore.registerEventListener(
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.NOTIFICATION,
            object : AdobeCallbackWithError<Event> {
                override fun fail(error: AdobeError) {
                }

                override fun call(event: Event) {
                    val eventData = event.eventData ?: return

                    try {
                        val propositionsList = DataReader.getTypedListOfMap(
                            Any::class.java,
                            eventData,
                            OptimizeConstants.EventDataKeys.PROPOSITIONS
                        )

                        val propositionsMap = mutableMapOf<DecisionScope, OptimizeProposition>()
                        propositionsList?.forEach { propositionData ->
                            val optimizeProposition =
                                OptimizeProposition.fromEventData(propositionData)
                            if (optimizeProposition != null && !optimizeProposition.scope.isNullOrEmpty()) {
                                val scope = DecisionScope(optimizeProposition.scope)
                                propositionsMap[scope] = optimizeProposition
                            }
                        }

                        if (propositionsMap.isNotEmpty()) {
                            callback.call(propositionsMap)
                        }
                    } catch (ignored: DataReaderException) {
                    }
                }
            }
        )
    }

    @JvmStatic
    fun clearCachedPropositions() {
        val event = Event.Builder(
            OptimizeConstants.EventNames.CLEAR_PROPOSITIONS_REQUEST,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_RESET
        ).build()
        MobileCore.dispatchEvent(event)
    }

    @JvmStatic
    fun getPropositions(
        decisionScopes: List<DecisionScope>,
        callback: AdobeCallback<Map<DecisionScope, OptimizeProposition>>
    ){
        getPropositions(decisionScopes, OptimizeConstants.GET_RESPONSE_CALLBACK_TIMEOUT, callback)
    }

    @JvmStatic
    fun getPropositions(
        decisionScopes: List<DecisionScope>,
        timeoutSeconds: Double = OptimizeConstants.GET_RESPONSE_CALLBACK_TIMEOUT,
        callback: AdobeCallback<Map<DecisionScope, OptimizeProposition>>
    ) {
        if (decisionScopes.isNullOrEmpty()) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Cannot get propositions, provided list of decision scopes is null or empty."
            )
            OptimizeUtils.failWithError(callback, AdobeError.UNEXPECTED_ERROR)
            return
        }

        val validScopes = decisionScopes.filter { it.isValid() }

        if (validScopes.isEmpty()) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Cannot get propositions, provided list of decision scopes has no valid scope."
            )
            OptimizeUtils.failWithError(callback, AdobeError.UNEXPECTED_ERROR)
            return
        }

        val flattenedDecisionScopes = validScopes.map { it.toEventData() }

        val eventData = mutableMapOf<String, Any>().apply {
            put(
                OptimizeConstants.EventDataKeys.REQUEST_TYPE,
                OptimizeConstants.EventDataValues.REQUEST_TYPE_GET
            )
            put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes)
        }

        val event = Event.Builder(
            OptimizeConstants.EventNames.GET_PROPOSITIONS_REQUEST,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONTENT
        ).setEventData(eventData).build()

        val timeoutMillis = (timeoutSeconds * OptimizeConstants.TIMEOUT_CONVERSION_FACTOR).toLong()

        MobileCore.dispatchEventWithResponseCallback(
            event,
            timeoutMillis,
            object : AdobeCallbackWithError<Event> {
                override fun fail(adobeError: AdobeError) {
                    OptimizeUtils.failWithError(callback, adobeError)
                }

                override fun call(event: Event) {
                    try {
                        val eventData = event.eventData ?: run {
                            OptimizeUtils.failWithError(callback, AdobeError.UNEXPECTED_ERROR)
                            return
                        }

                        if (eventData.containsKey(OptimizeConstants.EventDataKeys.RESPONSE_ERROR)) {
                            val errorCode = DataReader.getInt(
                                eventData,
                                OptimizeConstants.EventDataKeys.RESPONSE_ERROR
                            )
                            OptimizeUtils.failWithError(
                                callback,
                                OptimizeUtils.convertToAdobeError(errorCode)
                            )
                            return
                        }

                        val propositionsList = DataReader.getTypedListOfMap(
                            Any::class.java,
                            eventData,
                            OptimizeConstants.EventDataKeys.PROPOSITIONS
                        )

                        val propositionsMap = mutableMapOf<DecisionScope, OptimizeProposition>()
                        propositionsList?.forEach { propositionData ->
                            val optimizeProposition =
                                OptimizeProposition.fromEventData(propositionData)
                            if (optimizeProposition != null && !optimizeProposition.scope.isNullOrEmpty()) {
                                val scope = DecisionScope(optimizeProposition.scope)
                                propositionsMap[scope] = optimizeProposition
                            }
                        }

                        callback.call(propositionsMap)
                    } catch (e: DataReaderException) {
                        OptimizeUtils.failWithError(callback, AdobeError.UNEXPECTED_ERROR)
                    }
                }
            }
        )
    }

    @JvmStatic
    @JvmOverloads
    fun updatePropositions(
        decisionScopes: List<DecisionScope>,
        xdm: Map<String, Any>? = null,
        data: Map<String, Any>? = null,
        callback: AdobeCallback<Map<DecisionScope, OptimizeProposition>>
    ){
        updatePropositions(decisionScopes, xdm, data, OptimizeConstants.EDGE_CONTENT_COMPLETE_RESPONSE_TIMEOUT, callback)
    }

    @JvmStatic
    fun updatePropositions(
        decisionScopes: List<DecisionScope>,
        xdm: Map<String, Any>? = null,
        data: Map<String, Any>? = null,
        timeoutSeconds: Double = OptimizeConstants.EDGE_CONTENT_COMPLETE_RESPONSE_TIMEOUT,
        callback: AdobeCallback<Map<DecisionScope, OptimizeProposition>>
    ) {
        if (decisionScopes.isNullOrEmpty()) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Cannot update propositions, provided list of decision scopes is null or empty."
            )

            val aepOptimizeError = AEPOptimizeError.getUnexpectedError()
            OptimizeUtils.failWithOptimizeError(callback, aepOptimizeError)
            return
        }

        val validScopes = decisionScopes.filter { it.isValid() }

        if (validScopes.isEmpty()) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Cannot update propositions, provided list of decision scopes has no valid scope."
            )
            return
        }

        val flattenedDecisionScopes = validScopes.map { it.toEventData() }

        val eventData = mutableMapOf<String, Any>().apply {
            put(
                OptimizeConstants.EventDataKeys.REQUEST_TYPE,
                OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE
            )
            put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes)
            xdm?.let { put(OptimizeConstants.EventDataKeys.XDM, it) }
            data?.let { put(OptimizeConstants.EventDataKeys.DATA, it) }
            put(
                OptimizeConstants.EventDataKeys.TIMEOUT,
                (timeoutSeconds * OptimizeConstants.TIMEOUT_CONVERSION_FACTOR).toLong()
            )
        }

        val event = Event.Builder(
            OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONTENT
        ).setEventData(eventData).build()

        MobileCore.dispatchEventWithResponseCallback(
            event,
            (timeoutSeconds * OptimizeConstants.TIMEOUT_CONVERSION_FACTOR).toLong(),
            object : AdobeCallbackWithError<Event> {
                override fun fail(adobeError: AdobeError) {
                    val aepOptimizeError = when (adobeError) {
                        AdobeError.CALLBACK_TIMEOUT -> AEPOptimizeError.getTimeoutError()
                        else -> AEPOptimizeError.getUnexpectedError()
                    }
                    OptimizeUtils.failWithOptimizeError(callback, aepOptimizeError)
                }

                override fun call(event: Event) {
                    try {
                        val eventData = event.eventData ?: run {
                            OptimizeUtils.failWithOptimizeError(
                                callback,
                                AEPOptimizeError.getUnexpectedError()
                            )
                            return
                        }

                        if (eventData.containsKey(OptimizeConstants.EventDataKeys.RESPONSE_ERROR)) {
                            val error = eventData[OptimizeConstants.EventDataKeys.RESPONSE_ERROR]
                            if (error is Map<*, *>) {
                                OptimizeUtils.failWithOptimizeError(
                                    callback,
                                    AEPOptimizeError.toAEPOptimizeError(error as Map<String, Any>)
                                )
                            }
                        }

                        if (!eventData.containsKey(OptimizeConstants.EventDataKeys.PROPOSITIONS)) return

                        val propositionsList = DataReader.getTypedListOfMap(
                            Any::class.java,
                            eventData,
                            OptimizeConstants.EventDataKeys.PROPOSITIONS
                        )

                        val propositionsMap = mutableMapOf<DecisionScope, OptimizeProposition>()
                        propositionsList?.forEach { propositionData ->
                            val optimizeProposition =
                                OptimizeProposition.fromEventData(propositionData)
                            if (optimizeProposition != null && !optimizeProposition.scope.isNullOrEmpty()) {
                                val scope = DecisionScope(optimizeProposition.scope)
                                propositionsMap[scope] = optimizeProposition
                            }
                        }

                        callback?.call(propositionsMap)
                    } catch (e: DataReaderException) {
                        OptimizeUtils.failWithOptimizeError(
                            callback,
                            AEPOptimizeError.getUnexpectedError()
                        )
                    }
                }
            }
        )
    }
}
