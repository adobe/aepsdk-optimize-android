package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApiV2
import com.adobe.marketing.mobile.ExtensionApiV2Builder
import com.adobe.marketing.mobile.ExtensionV2
import com.adobe.marketing.mobile.ExtensionV2Delegate
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateStatus
import com.adobe.marketing.mobile.optimize.AEPOptimizeError.Companion.getUnexpectedError
import com.adobe.marketing.mobile.optimize.ConfigUtils.retrieveOptimizeRequestTimeout
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.DataReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private typealias PropositionReceiver = (Map<DecisionScope, OptimizeProposition>) -> Unit

private object PropositionManager {
    private val mutex = Mutex()
    private val map = mutableMapOf<DecisionScope, OptimizeProposition?>()
    private val receivers = ConcurrentHashMap<PropositionReceiver, List<DecisionScope>>()

    suspend fun updatePending(keys: List<DecisionScope>) =
        mutex.withLock { map.putAll(keys.map { it to null }) }

    suspend fun clearAll() = mutex.withLock {
        map.clear()
        receivers.clear()
    }

    suspend fun update(updates: Map<DecisionScope, OptimizeProposition>) = mutex.withLock {
        map.putAll(updates)
        notifyReceiverIfReady()
    }

    private fun notifyReceiverIfReady() = receivers.forEach { (receiver, keys) ->
        if (keys.all { key -> map.containsKey(key) && map[key] != null }) {
            receiver(keys.associateWith { key -> map[key]!! })
            receivers.remove(receiver)
        }
    }

    suspend fun get(keys: List<DecisionScope>): Map<DecisionScope, OptimizeProposition> =
        mutex.withLock {
            return if (keys.all { key -> map.containsKey(key) && map[key] != null }) {
                keys.associateWith { key -> map[key]!! }
            } else {
                suspendCancellableCoroutine {
                    register(keys) { map -> it.resume(map) }
                }
            }
        }

    suspend fun getAllOrNull(keys: List<DecisionScope>): Map<DecisionScope, OptimizeProposition>? =
        mutex.withLock {
            return if (keys.all { key -> map.containsKey(key) && map[key] != null }) {
                keys.associateWith { key -> map[key]!! }
            } else {
                return null
            }
        }


    private fun register(keys: List<DecisionScope>, receiver: PropositionReceiver) {
        receivers[receiver] = keys
    }
}

class OptimizeExtensionDelegate : ExtensionV2Delegate() {
    override fun getExtensionV2Class(): Class<out ExtensionV2> {
        return OptimizeExtensionV2::class.java
    }
}

class OptimizeExtensionV2 : ExtensionV2() {
    override val metadata: Map<String, String>?
        get() = null

    override val name: String
        get() = OptimizeConstants.EXTENSION_NAME

    override val version: String
        get() = OptimizeConstants.EXTENSION_VERSION

    override val friendlyName: String
        get() = OptimizeConstants.FRIENDLY_NAME

    private val updateDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(updateDispatcher)

    companion object {
        private const val SELF_TAG = "OptimizeExtension"

        // List containing the schema strings for the proposition items supported by the SDK, sent in
        // the personalization query request.
        val supportedSchemas: List<String> = Arrays.asList( // Target schemas
            OptimizeConstants.JsonValues.SCHEMA_TARGET_HTML,
            OptimizeConstants.JsonValues.SCHEMA_TARGET_JSON,
            OptimizeConstants.JsonValues.SCHEMA_TARGET_DEFAULT,  // Offer Decisioning schemas

            OptimizeConstants.JsonValues.SCHEMA_OFFER_HTML,
            OptimizeConstants.JsonValues.SCHEMA_OFFER_JSON,
            OptimizeConstants.JsonValues.SCHEMA_OFFER_IMAGE,
            OptimizeConstants.JsonValues.SCHEMA_OFFER_TEXT
        )

        // List containing recoverable network error codes being retried by Edge Network Service
        private val recoverableNetworkErrorCodes: List<Int> = Arrays.asList(
            OptimizeConstants.HTTPResponseCodes.clientTimeout,
            OptimizeConstants.HTTPResponseCodes.tooManyRequests,
            OptimizeConstants.HTTPResponseCodes.badGateway,
            OptimizeConstants.HTTPResponseCodes.serviceUnavailable,
            OptimizeConstants.HTTPResponseCodes.gatewayTimeout
        )

        private val updateRequestEventIdsErrors: MutableMap<String, AEPOptimizeError> =
            ConcurrentHashMap()
    }

    private var previewCachedPropositions: MutableMap<DecisionScope?, OptimizeProposition?> =
        ConcurrentHashMap()

    private var api: ExtensionApiV2? = null

    override fun onRegistered(buildExtensionApi: ExtensionApiV2Builder) {
        api = buildExtensionApi { event ->
            if (OptimizeConstants.EventType.OPTIMIZE.equals(event.type, ignoreCase = true)
                && OptimizeConstants.EventSource.REQUEST_CONTENT.equals(
                    event.source, ignoreCase = true
                )
            ) {
                val configurationSharedState =
                    api?.getSharedState(
                        OptimizeConstants.Configuration.EXTENSION_NAME,
                        event,
                        false,
                        SharedStateResolution.ANY
                    )
                return@buildExtensionApi configurationSharedState != null
                        && configurationSharedState.status == SharedStateStatus.SET
            }
            return@buildExtensionApi true
        }
        api?.registerEventListener(
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONTENT
        ) { event: Event ->
            this.handleOptimizeRequestContent(
                event
            )
        }

        api?.registerEventListener(
            OptimizeConstants.EventType.EDGE,
            OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS
        ) { event: Event -> this.handleEdgeResponse(event) }

        api?.registerEventListener(
            OptimizeConstants.EventType.EDGE,
            OptimizeConstants.EventSource.ERROR_RESPONSE_CONTENT
        ) { event: Event ->
            this.handleEdgeErrorResponse(
                event
            )
        }

        api?.registerEventListener(
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_RESET
        ) { event: Event ->
            this.handleClearPropositions(
                event
            )
        }

        // Register listener - Mobile Core `resetIdentities()` API dispatches generic identity
        // request reset event.
        api?.registerEventListener(
            OptimizeConstants.EventType.GENERIC_IDENTITY,
            OptimizeConstants.EventSource.REQUEST_RESET
        ) { event: Event ->
            this.handleClearPropositions(
                event
            )
        }

        api?.registerEventListener(
            EventType.SYSTEM,
            OptimizeConstants.EventSource.DEBUG
        ) { event: Event -> this.handleDebugEvent(event) }

    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     *
     * This method handles the event based on the value of {@value
     * * OptimizeConstants.EventDataKeys#REQUEST_TYPE} in the event data of current `event`
     *
     * @param event incoming [Event] object to be processed.
     */
    private suspend fun handleOptimizeRequestContent(event: Event) {
        if (OptimizeUtils.isNullOrEmpty(event.eventData)) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleOptimizeRequestContent - Ignoring the Optimize request event, either"
                        + " event is null or event data is null/ empty."
            )
            return
        }

        val eventData = event.eventData
        val requestType =
            DataReader.optString(eventData, OptimizeConstants.EventDataKeys.REQUEST_TYPE, "")

        when (requestType) {
            OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE -> handleUpdatePropositions(event)
            OptimizeConstants.EventDataValues.REQUEST_TYPE_GET -> try {
                // Fetch decision scopes from the event
                val decisionScopesData =
                    DataReader.getTypedListOfMap(
                        Any::class.java,
                        eventData,
                        OptimizeConstants.EventDataKeys.DECISION_SCOPES
                    )
                val eventDecisionScopes =
                    retrieveValidDecisionScopes(decisionScopesData)

                if (OptimizeUtils.isNullOrEmpty(eventDecisionScopes)) {
                    Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        ("handleOptimizeRequestContent - Cannot process the get propositions"
                                + " request event, provided list of decision scopes has no"
                                + " valid scope.")
                    )
                    api?.dispatch(
                        createResponseEventWithError(
                            event, AdobeError.UNEXPECTED_ERROR
                        )
                    )
                    return
                }
                val validScopes = retrieveValidDecisionScopes(decisionScopesData)
                if (OptimizeUtils.isNullOrEmpty(validScopes)) {
                    Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleGetPropositions - Cannot process the get propositions request event,"
                                + " provided list of decision scopes has no valid scope."
                    )
                    api?.dispatch(createResponseEventWithError(event, AdobeError.UNEXPECTED_ERROR))
                    return
                }
                scope.launch {
                    val fetchedPropositions = withTimeoutOrNull(5000) {
                        PropositionManager.get(validScopes!!)
                    }
                    if (fetchedPropositions != null) {
                        Log.trace(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "handleOptimizeRequestContent - All scopes are cached and none are"
                                    + " in progress, dispatching event directly."
                        )

                        try {

                            val propositionsList: MutableList<Map<String, Any>> = ArrayList()
                            fetchedPropositions.forEach { (_, proposition) ->
                                propositionsList.add(proposition.toEventData())
                            }
                            val previewPropositionsList: MutableList<Map<String, Any>> = ArrayList()
                            for (scope in validScopes!!) {
                                if (previewCachedPropositions.containsKey(scope)) {
                                    val optimizeProposition =
                                        previewCachedPropositions[scope]
                                    previewPropositionsList.add(optimizeProposition!!.toEventData())
                                }
                            }

                            val responseEventData: MutableMap<String, Any> = HashMap()

                            if (!previewPropositionsList.isEmpty()) {
                                Log.debug(
                                    OptimizeConstants.LOG_TAG,
                                    SELF_TAG,
                                    "Preview Mode is enabled."
                                )
                                responseEventData[OptimizeConstants.EventDataKeys.PROPOSITIONS] =
                                    previewPropositionsList
                            } else {
                                responseEventData[OptimizeConstants.EventDataKeys.PROPOSITIONS] =
                                    propositionsList
                            }

                            val responseEvent =
                                Event.Builder(
                                    OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                                    OptimizeConstants.EventType.OPTIMIZE,
                                    OptimizeConstants.EventSource.RESPONSE_CONTENT
                                )
                                    .setEventData(responseEventData)
                                    .inResponseToEvent(event)
                                    .build()

                            api?.dispatch(responseEvent)
                        } catch (e: Exception) {
                            Log.warning(
                                OptimizeConstants.LOG_TAG,
                                SELF_TAG,
                                "handleGetPropositions - Failed to process get propositions request event due"
                                        + " to an exception (%s)!",
                                e.localizedMessage
                            )
                            api?.dispatch(
                                createResponseEventWithError(
                                    event,
                                    AdobeError.UNEXPECTED_ERROR
                                )
                            )
                        }
                    } else {
                        api?.dispatch(
                            createResponseEventWithError(
                                event,
                                AdobeError.UNEXPECTED_ERROR
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleOptimizeRequestContent - Failed to process get propositions"
                            + " request event due to an exception (%s)!",
                    e.localizedMessage
                )
            }

            OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK -> handleTrackPropositions(event)
            else -> Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleOptimizeRequestContent - Ignoring the Optimize request event,"
                        + " provided request type (%s) is not handled by this extension.",
                requestType
            )
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     *
     * This method dispatches an event to the Edge network extension to send personalization
     * query request to the Experience Edge network. The dispatched event contains additional XDM
     * and/ or free-form data, read from the incoming event, to be attached to the Edge request.
     *
     * @param event incoming [Event] object to be processed.
     */
    private suspend fun handleUpdatePropositions(event: Event) {
        val eventData = event.eventData

        val configData = retrieveConfigurationSharedState(event)
        if (OptimizeUtils.isNullOrEmpty(configData)) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleUpdatePropositions - Cannot process the update propositions request"
                        + " event, Configuration shared state is not available."
            )
            return
        }

        val decisionScopesData =
            DataReader.getTypedListOfMap(
                Any::class.java,
                eventData,
                OptimizeConstants.EventDataKeys.DECISION_SCOPES
            )
        val validScopes = retrieveValidDecisionScopes(decisionScopesData)
        if (OptimizeUtils.isNullOrEmpty(validScopes)) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleUpdatePropositions - Cannot process the update propositions request"
                        + " event, provided list of decision scopes has no valid scope."
            )
            return
        }

        val edgeEventData: MutableMap<String, Any> = HashMap()

        // Add query
        val queryPersonalization: MutableMap<String, Any> = HashMap()
        queryPersonalization[OptimizeConstants.JsonKeys.SCHEMAS] = supportedSchemas

        val validScopeNames: MutableList<String> = ArrayList()
        for (scope in validScopes!!) {
            validScopeNames.add(scope!!.name)
        }
        queryPersonalization[OptimizeConstants.JsonKeys.DECISION_SCOPES] = validScopeNames

        val query: MutableMap<String, Any> = HashMap()
        query[OptimizeConstants.JsonKeys.QUERY_PERSONALIZATION] = queryPersonalization
        edgeEventData[OptimizeConstants.JsonKeys.QUERY] = query

        // Add xdm
        val xdm: MutableMap<String?, Any?> = HashMap()
        if (eventData.containsKey(OptimizeConstants.EventDataKeys.XDM)) {
            val inputXdm =
                DataReader.getTypedMap(
                    Any::class.java, eventData, OptimizeConstants.EventDataKeys.XDM
                )
            if (!OptimizeUtils.isNullOrEmpty(inputXdm)) {
                xdm.putAll(inputXdm)
            }
        }
        xdm[OptimizeConstants.JsonKeys.EXPERIENCE_EVENT_TYPE] =
            OptimizeConstants.JsonValues.EE_EVENT_TYPE_PERSONALIZATION
        edgeEventData[OptimizeConstants.JsonKeys.XDM] = xdm

        // Add data
        val data: MutableMap<String?, Any?> = HashMap()
        if (eventData.containsKey(OptimizeConstants.EventDataKeys.DATA)) {
            val inputData =
                DataReader.getTypedMap(
                    Any::class.java, eventData, OptimizeConstants.EventDataKeys.DATA
                )
            if (!OptimizeUtils.isNullOrEmpty(inputData)) {
                data.putAll(inputData)
                edgeEventData[OptimizeConstants.JsonKeys.DATA] = data
            }
        }

        // Add the flag to request sendCompletion
        val request: MutableMap<String, Any> = HashMap()
        request[OptimizeConstants.JsonKeys.REQUEST_SEND_COMPLETION] = true
        edgeEventData[OptimizeConstants.JsonKeys.REQUEST] = request

        // Add override datasetId
        if (configData.containsKey(
                OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID
            )
        ) {
            val overrideDatasetId =
                DataReader.getString(
                    configData,
                    OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID
                )
            if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                edgeEventData[OptimizeConstants.JsonKeys.DATASET_ID] = overrideDatasetId
            }
        }

        val edgeEvent =
            Event.Builder(
                OptimizeConstants.EventNames.EDGE_PERSONALIZATION_REQUEST,
                OptimizeConstants.EventType.EDGE,
                OptimizeConstants.EventSource.REQUEST_CONTENT
            )
                .setEventData(edgeEventData)
                .chainToParentEvent(event)
                .build()

        val timeoutMillis = retrieveOptimizeRequestTimeout(event, configData)

        PropositionManager.updatePending(validScopes)

        val edgeResponseEvent: Event? = suspendCoroutine {
            MobileCore.dispatchEventWithResponseCallback(
                edgeEvent,
                timeoutMillis,
                object : AdobeCallbackWithError<Event?> {
                    override fun call(callbackEvent: Event?) {
                        it.resume(callbackEvent)
                    }

                    override fun fail(error: AdobeError) {
                        it.resume(null)
                    }
                })
        }

        if (edgeResponseEvent == null || OptimizeUtils.isNullOrEmpty(
                OptimizeUtils.getRequestEventId(
                    edgeResponseEvent
                )
            )
        ) {
            api?.dispatch(createResponseEventWithError(event, getUnexpectedError()))
            return
        }


        val responseEventData: MutableMap<String, Any> = java.util.HashMap()
        val aepOptimizeError =
            updateRequestEventIdsErrors[OptimizeUtils.getRequestEventId(edgeResponseEvent)]
        if (aepOptimizeError != null) {
            responseEventData[OptimizeConstants.EventDataKeys.RESPONSE_ERROR] =
                aepOptimizeError.toEventData()
        }

        val propositionsList: MutableList<Map<String, Any>> = java.util.ArrayList()

        validScopes.forEach { scope ->
            val map = PropositionManager.getAllOrNull(listOf(scope))
            if (map != null) {
                propositionsList.add(map[scope]!!.toEventData())
            }
        }

        responseEventData[OptimizeConstants.EventDataKeys.PROPOSITIONS] = propositionsList

        val responseEvent =
            Event.Builder(
                OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.RESPONSE_CONTENT
            )
                .setEventData(responseEventData)
                .inResponseToEvent(event)
                .build()

        api?.dispatch(responseEvent)
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value
     * * OptimizeConstants.EventSource#EDGE_PERSONALIZATION_DECISIONS}.
     *
     *
     * This method caches the propositions, returned in the Edge response, in the SDK. It also
     * dispatches a personalization notification event with the received propositions.
     *
     * @param event incoming [Event] object to be processed.
     */
    private suspend fun handleEdgeResponse(event: Event) {
        try {
            val eventData = event.eventData
            val requestEventId = OptimizeUtils.getRequestEventId(event)

            if (!OptimizeUtils.isPersonalizationDecisionsResponse(event) || OptimizeUtils.isNullOrEmpty(
                    requestEventId
                )
            ) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    ("handleEdgeResponse - Ignoring Edge event, either handle type is not"
                            + " personalization:decisions, or the response isn't intended for this"
                            + " extension.")
                )
                return
            }

            val payload =
                DataReader.getTypedListOfMap(
                    Any::class.java, eventData, OptimizeConstants.Edge.PAYLOAD
                )
            if (OptimizeUtils.isNullOrEmpty(payload)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    ("handleEdgeResponse - Cannot process the Edge personalization:decisions"
                            + " event, propositions list is either null or empty in the Edge"
                            + " response.")
                )
                return
            }

            val propositionsMap: MutableMap<DecisionScope, OptimizeProposition> = HashMap()
            for (propositionData in payload) {
                val optimizeProposition =
                    OptimizeProposition.fromEventData(propositionData)
                if (optimizeProposition != null
                    && !OptimizeUtils.isNullOrEmpty(optimizeProposition.offers)
                ) {
                    val scope = DecisionScope(optimizeProposition.scope)
                    propositionsMap[scope] = optimizeProposition
                }
            }

            if (OptimizeUtils.isNullOrEmpty(propositionsMap)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    ("handleEdgeResponse - Cannot process the Edge personalization:decisions"
                            + " event, no propositions with valid offers are present in the Edge"
                            + " response.")
                )
                return
            }

            PropositionManager.update(propositionsMap)

            val propositionsList: MutableList<Map<String, Any>> = ArrayList()
            for (optimizeProposition in propositionsMap.values) {
                propositionsList.add(optimizeProposition!!.toEventData())
            }
            val notificationData: MutableMap<String, Any> = HashMap()
            notificationData[OptimizeConstants.EventDataKeys.PROPOSITIONS] = propositionsList

            val edgeEvent =
                Event.Builder(
                    OptimizeConstants.EventNames.OPTIMIZE_NOTIFICATION,
                    OptimizeConstants.EventType.OPTIMIZE,
                    OptimizeConstants.EventSource.NOTIFICATION
                )
                    .setEventData(notificationData)
                    .build()

            // Dispatch notification event
            api?.dispatch(edgeEvent)
        } catch (e: Exception) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleEdgeResponse - Cannot process the Edge personalization:decisions event"
                        + " due to an exception (%s)!",
                e.localizedMessage
            )
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value
     * * OptimizeConstants.EventSource#ERROR_RESPONSE_CONTENT}.
     *
     *
     * This method logs the error information, returned in Edge response, specifying error type
     * along with a detail message.
     *
     * @param event incoming [Event] object to be processed.
     */
    private fun handleEdgeErrorResponse(event: Event) {
        try {
            val eventData = event.eventData
            val requestEventId = OptimizeUtils.getRequestEventId(event)

            if (!OptimizeUtils.isEdgeErrorResponseContent(event) || OptimizeUtils.isNullOrEmpty(
                    requestEventId
                )
            ) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    ("handleEdgeResponse - Ignoring Edge event, either handle type is not edge"
                            + " error response content, or the response isn't intended for this"
                            + " extension.")
                )
                return
            }

            if (OptimizeUtils.isNullOrEmpty(event.eventData)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleEdgeErrorResponse - Ignoring the Edge error response event, either"
                            + " event is null or event data is null/ empty."
                )
                return
            }

            val errorType =
                DataReader.optString(
                    eventData,
                    OptimizeConstants.Edge.ErrorKeys.TYPE,
                    OptimizeConstants.ERROR_UNKNOWN
                )
            val errorStatus =
                DataReader.optInt(
                    eventData,
                    OptimizeConstants.Edge.ErrorKeys.STATUS,
                    OptimizeConstants.UNKNOWN_STATUS
                )
            val errorTitle =
                DataReader.optString(
                    eventData,
                    OptimizeConstants.Edge.ErrorKeys.TITLE,
                    OptimizeConstants.ERROR_UNKNOWN
                )
            val errorDetail =
                DataReader.optString(
                    eventData,
                    OptimizeConstants.Edge.ErrorKeys.DETAIL,
                    OptimizeConstants.ERROR_UNKNOWN
                )
            val errorReport =
                DataReader.optTypedMap(
                    Any::class.java,
                    eventData,
                    OptimizeConstants.Edge.ErrorKeys.REPORT,
                    HashMap()
                )

            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                ("""
                    handleEdgeErrorResponse - Decisioning Service error! Error type: (%s),
                    title: (%s),
                    detail: (%s),
                    status: (%s),
                    report: (%s)
                    """.trimIndent()),
                errorType,
                errorTitle,
                errorDetail,
                errorStatus,
                errorReport
            )

            // Check if the errorStatus is in the list of recoverable error codes
            if (recoverableNetworkErrorCodes.contains(errorStatus)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Recoverable error encountered: Status %d",
                    errorStatus
                )
                return
            } else {
                val aepOptimizeError =
                    AEPOptimizeError(
                        errorType, errorStatus, errorTitle, errorDetail, errorReport, null
                    )
                updateRequestEventIdsErrors[requestEventId] =
                    aepOptimizeError
            }
        } catch (e: Exception) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleEdgeResponse - Cannot process the Edge Error Response event"
                        + " due to an exception (%s)!",
                e.localizedMessage
            )
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     *
     * This method caches the propositions, returned in the Edge response, in the SDK. It also
     * dispatches an optimize response event with the propositions for the requested decision
     * scopes.
     *
     * @param event incoming [Event] object to be processed.
     */
    private fun handleGetPropositions(event: Event) {

    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     *
     * This method dispatches an event to the Edge network extension to send proposition
     * interactions information to the Experience Edge network. The dispatched event may contain an
     * override `datasetId` indicating the dataset which will be used for storing the
     * Experience Events sent to the Edge network.
     *
     * @param event incoming [Event] object to be processed.
     */
    private suspend fun handleTrackPropositions(event: Event) {
        val eventData = event.eventData

        val configData = retrieveConfigurationSharedState(event)
        if (OptimizeUtils.isNullOrEmpty(configData)) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleTrackPropositions - Cannot process the track propositions request event,"
                        + " Configuration shared state is not available."
            )
            return
        }

        try {
            val propositionInteractionsXdm =
                DataReader.getTypedMap(
                    Any::class.java,
                    eventData,
                    OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS
                )
            if (OptimizeUtils.isNullOrEmpty(propositionInteractionsXdm)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleTrackPropositions - Cannot process the track propositions request"
                            + " event, provided proposition interactions map is null or empty."
                )
                return
            }

            val edgeEventData: MutableMap<String, Any> = HashMap()
            edgeEventData[OptimizeConstants.JsonKeys.XDM] = propositionInteractionsXdm

            // Add override datasetId
            if (configData!!.containsKey(
                    OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID
                )
            ) {
                val overrideDatasetId =
                    DataReader.getString(
                        configData,
                        OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID
                    )
                if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                    edgeEventData[OptimizeConstants.JsonKeys.DATASET_ID] = overrideDatasetId
                }
            }

            val edgeEvent =
                Event.Builder(
                    OptimizeConstants.EventNames
                        .EDGE_PROPOSITION_INTERACTION_REQUEST,
                    OptimizeConstants.EventType.EDGE,
                    OptimizeConstants.EventSource.REQUEST_CONTENT
                )
                    .setEventData(edgeEventData)
                    .build()

            api?.dispatch(edgeEvent)
        } catch (e: Exception) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleTrackPropositions - Failed to process track propositions request event"
                        + " due to an exception (%s)!",
                e.localizedMessage
            )
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * * OptimizeConstants.EventSource#REQUEST_RESET}.
     *
     *
     * This method clears previously cached propositions in the SDK.
     *
     * @param event incoming [Event] object to be processed.
     */
    private suspend fun handleClearPropositions(event: Event) {
        PropositionManager.clearAll()
        previewCachedPropositions.clear()
    }

    /**
     * Handles the event with type {@value EventType#SYSTEM} and source {@value
     * * OptimizeConstants.EventSource#DEBUG}.
     *
     *
     * A debug event allows the optimize extension to processes non-production workflows.
     *
     * @param event the debug [Event] to be handled.
     */
    private fun handleDebugEvent(event: Event) {
        try {
            if (OptimizeUtils.isNullOrEmpty(event.eventData)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleDebugEvent - Ignoring the Optimize Debug event, either event is null"
                            + " or event data is null/ empty."
                )
                return
            }

            if (!OptimizeUtils.isPersonalizationDebugEvent(event)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    ("handleDebugEvent - Ignoring Optimize Debug event, either handle type is"
                            + " not com.adobe.eventType.system or source is not"
                            + " com.adobe.eventSource.debug")
                )
                return
            }

            val eventData = event.eventData

            val payload =
                DataReader.getTypedListOfMap(
                    Any::class.java, eventData, OptimizeConstants.Edge.PAYLOAD
                )
            if (OptimizeUtils.isNullOrEmpty(payload)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleDebugEvent - Cannot process the Debug event, propositions list is"
                            + " either null or empty in the response."
                )
                return
            }

            val propositionsMap: MutableMap<DecisionScope?, OptimizeProposition?> = HashMap()
            for (propositionData in payload) {
                val optimizeProposition =
                    OptimizeProposition.fromEventData(propositionData)
                if (optimizeProposition != null
                    && !OptimizeUtils.isNullOrEmpty(optimizeProposition.offers)
                ) {
                    val scope = DecisionScope(optimizeProposition.scope)
                    propositionsMap[scope] = optimizeProposition
                }
            }

            if (OptimizeUtils.isNullOrEmpty(propositionsMap)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleDebugEvent - Cannot process the Debug event, no propositions with"
                            + " valid offers are present in the response."
                )
                return
            }

            previewCachedPropositions.putAll(propositionsMap)

            val propositionsList: MutableList<Map<String, Any>> = ArrayList()
            for (optimizeProposition in propositionsMap.values) {
                propositionsList.add(optimizeProposition!!.toEventData())
            }
            val notificationData: MutableMap<String, Any> = HashMap()
            notificationData[OptimizeConstants.EventDataKeys.PROPOSITIONS] = propositionsList

            val notificationEvent =
                Event.Builder(
                    OptimizeConstants.EventNames.OPTIMIZE_NOTIFICATION,
                    OptimizeConstants.EventType.OPTIMIZE,
                    OptimizeConstants.EventSource.NOTIFICATION
                )
                    .setEventData(notificationData)
                    .build()

            // Dispatch notification event
            api?.dispatch(notificationEvent)
        } catch (e: Exception) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "handleDebugEvent - Cannot process the Debug event due to an exception (%s)!",
                e.localizedMessage
            )
        }
    }

    /**
     * Retrieves the `Configuration` shared state versioned at the current `event`.
     *
     * @param event incoming [Event] instance.
     * @return `Map<String, Object>` containing configuration data.
     */
    suspend fun retrieveConfigurationSharedState(event: Event): Map<String, Any?> {
        val configurationSharedState =
            api?.getSharedState(
                OptimizeConstants.Configuration.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.ANY
            )
        return configurationSharedState?.value ?: emptyMap()
    }

    /**
     * Retrieves the `List<DecisionScope>` containing valid scopes.
     *
     *
     * This method returns null if the given `decisionScopesData` list is null, or empty,
     * or if there is no valid decision scope in the provided list.
     *
     * @param decisionScopesData input `List<Map<String, Object>>` containing scope data.
     * @return `List<DecisionScope>` containing valid scopes.
     * @see DecisionScope.isValid
     */
    private fun retrieveValidDecisionScopes(
        decisionScopesData: List<Map<String, Any>?>
    ): List<DecisionScope>? {
        if (OptimizeUtils.isNullOrEmpty(decisionScopesData)) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "retrieveValidDecisionScopes - No valid decision scopes are retrieved, provided"
                        + " decision scopes list is null or empty."
            )
            return null
        }

        val validScopes: MutableList<DecisionScope> = ArrayList()
        for (scopeData in decisionScopesData) {
            val scope = DecisionScope.fromEventData(scopeData)
            if (scope == null || !scope.isValid) {
                continue
            }
            validScopes.add(scope)
        }

        if (validScopes.size == 0) {
            Log.warning(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "retrieveValidDecisionScopes - No valid decision scopes are retrieved, provided"
                        + " list of decision scopes has no valid scope."
            )
            return null
        }

        return validScopes
    }

    /**
     * Creates {@value OptimizeConstants.EventType#OPTIMIZE}, {@value
     * * OptimizeConstants.EventSource#RESPONSE_CONTENT} event with the given `error` in event
     * data.
     *
     * @return [Event] instance.
     */
    private fun createResponseEventWithError(event: Event, error: AdobeError): Event {
        val eventData: MutableMap<String, Any> = HashMap()
        eventData[OptimizeConstants.EventDataKeys.RESPONSE_ERROR] = error.errorCode

        return Event.Builder(
            OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.RESPONSE_CONTENT
        )
            .setEventData(eventData)
            .inResponseToEvent(event)
            .build()
    }

    private fun createResponseEventWithError(event: Event, error: AEPOptimizeError): Event {
        val eventData: MutableMap<String, Any> = HashMap()
        eventData[OptimizeConstants.EventDataKeys.RESPONSE_ERROR] = error

        return Event.Builder(
            OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.RESPONSE_CONTENT
        )
            .setEventData(eventData)
            .inResponseToEvent(event)
            .build()
    }

}