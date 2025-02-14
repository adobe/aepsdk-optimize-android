/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.optimize;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class OptimizeExtension extends Extension {

    private static final String SELF_TAG = "OptimizeExtension";

    // Concurrent Map containing the cached propositions returned in various
    // personalization:decisions events
    // for the same Edge personalization request.
    // This is accessed from multiple threads.
    private Map<DecisionScope, OptimizeProposition> cachedPropositions = new ConcurrentHashMap<>();

    // Concurrent Map containing propositions simulated for preview and cached in-memory in the SDK
    private Map<DecisionScope, OptimizeProposition> previewCachedPropositions =
            new ConcurrentHashMap<>();

    // Events dispatcher used to maintain the processing order of update and get propositions
    // events.
    // It ensures any update propositions requests issued before a get propositions call are
    // completed
    // and the get propositions request is fulfilled from the latest cached content.
    private SerialWorkDispatcher eventsDispatcher =
            new SerialWorkDispatcher(
                    "OptimizeEventsDispatcher",
                    new SerialWorkDispatcher.WorkHandler<Event>() {
                        @Override
                        public boolean doWork(final Event event) {
                            if (OptimizeUtils.isGetEvent(event)) {
                                handleGetPropositions(event);
                            } else if (event.getType()
                                    .equalsIgnoreCase(OptimizeConstants.EventType.EDGE)) {
                                return !updateRequestEventIdsInProgress.containsKey(
                                        event.getUniqueIdentifier());
                            }
                            return true;
                        }
                    });

    // Concurrent Map containing the update event IDs (and corresponding requested scopes) for Edge
    // events that haven't yet received an Edge completion response.
    // This is accessed from multiple threads.
    private final Map<String, List<DecisionScope>> updateRequestEventIdsInProgress =
            new ConcurrentHashMap<>();

    // Concurrent Map to accumulate propositions returned in various personalization:decisions
    // events
    // for the same Edge personalization request.
    // This is accessed from multiple threads.
    private final Map<DecisionScope, OptimizeProposition> propositionsInProgress =
            new ConcurrentHashMap<>();

    // List containing the schema strings for the proposition items supported by the SDK, sent in
    // the personalization query request.
    static final List<String> supportedSchemas =
            Arrays.asList(
                    // Target schemas
                    OptimizeConstants.JsonValues.SCHEMA_TARGET_HTML,
                    OptimizeConstants.JsonValues.SCHEMA_TARGET_JSON,
                    OptimizeConstants.JsonValues.SCHEMA_TARGET_DEFAULT,

                    // Offer Decisioning schemas
                    OptimizeConstants.JsonValues.SCHEMA_OFFER_HTML,
                    OptimizeConstants.JsonValues.SCHEMA_OFFER_JSON,
                    OptimizeConstants.JsonValues.SCHEMA_OFFER_IMAGE,
                    OptimizeConstants.JsonValues.SCHEMA_OFFER_TEXT);

    // List containing recoverable network error codes being retried by Edge Network Service
    private static final List<Integer> recoverableNetworkErrorCodes =
            Arrays.asList(
                    OptimizeConstants.HTTPResponseCodes.clientTimeout,
                    OptimizeConstants.HTTPResponseCodes.tooManyRequests,
                    OptimizeConstants.HTTPResponseCodes.badGateway,
                    OptimizeConstants.HTTPResponseCodes.serviceUnavailable,
                    OptimizeConstants.HTTPResponseCodes.gatewayTimeout);

    // Map containing the update event IDs and corresponding errors as received from Edge SDK
    private static final Map<String, AEPOptimizeError> updateRequestEventIdsErrors =
            new ConcurrentHashMap<>();

    /**
     * Constructor for {@code OptimizeExtension}.
     *
     * <p>It is invoked during the extension registration to retrieve the extension's details such
     * as name and version. The following {@link Event} listeners are registered during the process.
     *
     * <ul>
     *   <li>Listener for {@code Event} type {@value OptimizeConstants.EventType#OPTIMIZE} and
     *       source {@value OptimizeConstants.EventSource#REQUEST_CONTENT} Listener for {@code
     *       Event} type {@value OptimizeConstants.EventType#EDGE} and source {@value
     *       OptimizeConstants.EventSource#EDGE_PERSONALIZATION_DECISIONS} Listener for {@code
     *       Event} type {@value OptimizeConstants.EventType#EDGE} and source {@value
     *       OptimizeConstants.EventSource#ERROR_RESPONSE_CONTENT} Listener for {@code Event} type
     *       {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     *       OptimizeConstants.EventSource#REQUEST_RESET} Listener for {@code Event} type {@value
     *       OptimizeConstants.EventType#GENERIC_IDENTITY} and source {@value
     *       OptimizeConstants.EventSource#REQUEST_RESET} Listener for {@code Event} type {@value
     *       OptimizeConstants.EventType#OPTIMIZE} and source {@value
     *       OptimizeConstants.EventSource#CONTENT_COMPLETE} Listener for {@code Event} type {@value
     *       EventType#SYSTEM} and source {@value OptimizeConstants.EventSource#DEBUG}
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance.
     */
    protected OptimizeExtension(final ExtensionApi extensionApi) {
        super(extensionApi);
    }

    @Override
    protected void onRegistered() {
        getApi().registerEventListener(
                        OptimizeConstants.EventType.OPTIMIZE,
                        OptimizeConstants.EventSource.REQUEST_CONTENT,
                        this::handleOptimizeRequestContent);

        getApi().registerEventListener(
                        OptimizeConstants.EventType.EDGE,
                        OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS,
                        this::handleEdgeResponse);

        getApi().registerEventListener(
                        OptimizeConstants.EventType.EDGE,
                        OptimizeConstants.EventSource.ERROR_RESPONSE_CONTENT,
                        this::handleEdgeErrorResponse);

        getApi().registerEventListener(
                        OptimizeConstants.EventType.OPTIMIZE,
                        OptimizeConstants.EventSource.REQUEST_RESET,
                        this::handleClearPropositions);

        // Register listener - Mobile Core `resetIdentities()` API dispatches generic identity
        // request reset event.
        getApi().registerEventListener(
                        OptimizeConstants.EventType.GENERIC_IDENTITY,
                        OptimizeConstants.EventSource.REQUEST_RESET,
                        this::handleClearPropositions);

        getApi().registerEventListener(
                        OptimizeConstants.EventType.OPTIMIZE,
                        OptimizeConstants.EventSource.CONTENT_COMPLETE,
                        this::handleUpdatePropositionsCompleted);

        getApi().registerEventListener(
                        EventType.SYSTEM,
                        OptimizeConstants.EventSource.DEBUG,
                        this::handleDebugEvent);

        eventsDispatcher.start();
    }

    @Override
    public boolean readyForEvent(@NonNull final Event event) {
        if (OptimizeConstants.EventType.OPTIMIZE.equalsIgnoreCase(event.getType())
                && OptimizeConstants.EventSource.REQUEST_CONTENT.equalsIgnoreCase(
                        event.getSource())) {
            SharedStateResult configurationSharedState =
                    getApi().getSharedState(
                                    OptimizeConstants.Configuration.EXTENSION_NAME,
                                    event,
                                    false,
                                    SharedStateResolution.ANY);
            return configurationSharedState != null
                    && configurationSharedState.getStatus() == SharedStateStatus.SET;
        }
        return true;
    }

    /**
     * Retrieve the extension name.
     *
     * @return {@link String} containing the unique name for this extension.
     */
    @NonNull @Override
    protected String getName() {
        return OptimizeConstants.EXTENSION_NAME;
    }

    /**
     * Retrieve the extension version.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @NonNull @Override
    protected String getVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * Retrieve the friendly name.
     *
     * @return {@link String} containing the friendly name for this extension.
     */
    @NonNull @Override
    protected String getFriendlyName() {
        return OptimizeConstants.FRIENDLY_NAME;
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     * <p>This method handles the event based on the value of {@value
     * OptimizeConstants.EventDataKeys#REQUEST_TYPE} in the event data of current {@code event}
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleOptimizeRequestContent(@NonNull final Event event) {
        if (OptimizeUtils.isNullOrEmpty(event.getEventData())) {
            Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleOptimizeRequestContent - Ignoring the Optimize request event, either"
                            + " event is null or event data is null/ empty.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        final String requestType =
                DataReader.optString(eventData, OptimizeConstants.EventDataKeys.REQUEST_TYPE, "");

        switch (requestType) {
            case OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE:
                handleUpdatePropositions(event);
                break;
            case OptimizeConstants.EventDataValues.REQUEST_TYPE_GET:
                try {
                    // Fetch decision scopes from the event
                    List<Map<String, Object>> decisionScopesData =
                            DataReader.getTypedListOfMap(
                                    Object.class,
                                    eventData,
                                    OptimizeConstants.EventDataKeys.DECISION_SCOPES);
                    List<DecisionScope> eventDecisionScopes =
                            retrieveValidDecisionScopes(decisionScopesData);

                    if (OptimizeUtils.isNullOrEmpty(eventDecisionScopes)) {
                        Log.debug(
                                OptimizeConstants.LOG_TAG,
                                SELF_TAG,
                                "handleOptimizeRequestContent - Cannot process the get propositions"
                                        + " request event, provided list of decision scopes has no"
                                        + " valid scope.");
                        getApi().dispatch(
                                        createResponseEventWithError(
                                                event, AdobeError.UNEXPECTED_ERROR));
                        return;
                    }

                    // Fetch propositions for the decision scopes from the cache
                    Map<DecisionScope, OptimizeProposition> fetchedPropositions = new HashMap<>();
                    for (DecisionScope scope : eventDecisionScopes) {
                        if (cachedPropositions.containsKey(scope)) {
                            fetchedPropositions.put(scope, cachedPropositions.get(scope));
                        }
                    }

                    // Check if all scopes are cached and none are in progress
                    boolean anyScopeInProgress = false;
                    HashSet<DecisionScope> scopesInProgress = new HashSet<>();
                    for (List<DecisionScope> updatingScope :
                            updateRequestEventIdsInProgress.values()) {
                        scopesInProgress.addAll(updatingScope);
                    }
                    for (DecisionScope scope : eventDecisionScopes) {
                        if (scopesInProgress.contains(scope)) {
                            anyScopeInProgress = true;
                            break;
                        }
                    }

                    if ((fetchedPropositions.size() == eventDecisionScopes.size())
                            && !anyScopeInProgress) {
                        Log.trace(
                                OptimizeConstants.LOG_TAG,
                                SELF_TAG,
                                "handleOptimizeRequestContent - All scopes are cached and none are"
                                        + " in progress, dispatching event directly.");

                        // Dispatch the event directly
                        handleGetPropositions(event);
                    } else {
                        Log.trace(
                                OptimizeConstants.LOG_TAG,
                                SELF_TAG,
                                "handleOptimizeRequestContent - Scopes are not fully cached or are"
                                        + " in progress, adding event to dispatcher.");
                        eventsDispatcher.offer(event);
                    }
                    break;
                } catch (final Exception e) {
                    Log.warning(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "handleOptimizeRequestContent - Failed to process get propositions"
                                    + " request event due to an exception (%s)!",
                            e.getLocalizedMessage());
                }
                break;
            case OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK:
                handleTrackPropositions(event);
                break;
            default:
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleOptimizeRequestContent - Ignoring the Optimize request event,"
                                + " provided request type (%s) is not handled by this extension.",
                        requestType);
                break;
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     * <p>This method dispatches an event to the Edge network extension to send personalization
     * query request to the Experience Edge network. The dispatched event contains additional XDM
     * and/ or free-form data, read from the incoming event, to be attached to the Edge request.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleUpdatePropositions(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();

        final Map<String, Object> configData = retrieveConfigurationSharedState(event);
        if (OptimizeUtils.isNullOrEmpty(configData)) {
            Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleUpdatePropositions - Cannot process the update propositions request"
                            + " event, Configuration shared state is not available.");
            return;
        }

        try {
            final List<Map<String, Object>> decisionScopesData =
                    DataReader.getTypedListOfMap(
                            Object.class,
                            eventData,
                            OptimizeConstants.EventDataKeys.DECISION_SCOPES);
            final List<DecisionScope> validScopes = retrieveValidDecisionScopes(decisionScopesData);
            if (OptimizeUtils.isNullOrEmpty(validScopes)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleUpdatePropositions - Cannot process the update propositions request"
                                + " event, provided list of decision scopes has no valid scope.");
                return;
            }

            final Map<String, Object> edgeEventData = new HashMap<>();

            // Add query
            final Map<String, Object> queryPersonalization = new HashMap<>();
            queryPersonalization.put(OptimizeConstants.JsonKeys.SCHEMAS, supportedSchemas);

            final List<String> validScopeNames = new ArrayList<>();
            for (final DecisionScope scope : validScopes) {
                validScopeNames.add(scope.getName());
            }
            queryPersonalization.put(OptimizeConstants.JsonKeys.DECISION_SCOPES, validScopeNames);

            final Map<String, Object> query = new HashMap<>();
            query.put(OptimizeConstants.JsonKeys.QUERY_PERSONALIZATION, queryPersonalization);
            edgeEventData.put(OptimizeConstants.JsonKeys.QUERY, query);

            // Add xdm
            final Map<String, Object> xdm = new HashMap<>();
            if (eventData.containsKey(OptimizeConstants.EventDataKeys.XDM)) {
                final Map<String, Object> inputXdm =
                        DataReader.getTypedMap(
                                Object.class, eventData, OptimizeConstants.EventDataKeys.XDM);
                if (!OptimizeUtils.isNullOrEmpty(inputXdm)) {
                    xdm.putAll(inputXdm);
                }
            }
            xdm.put(
                    OptimizeConstants.JsonKeys.EXPERIENCE_EVENT_TYPE,
                    OptimizeConstants.JsonValues.EE_EVENT_TYPE_PERSONALIZATION);
            edgeEventData.put(OptimizeConstants.JsonKeys.XDM, xdm);

            // Add data
            final Map<String, Object> data = new HashMap<>();
            if (eventData.containsKey(OptimizeConstants.EventDataKeys.DATA)) {
                final Map<String, Object> inputData =
                        DataReader.getTypedMap(
                                Object.class, eventData, OptimizeConstants.EventDataKeys.DATA);
                if (!OptimizeUtils.isNullOrEmpty(inputData)) {
                    data.putAll(inputData);
                    edgeEventData.put(OptimizeConstants.JsonKeys.DATA, data);
                }
            }

            // Add the flag to request sendCompletion
            final Map<String, Object> request = new HashMap<>();
            request.put(OptimizeConstants.JsonKeys.REQUEST_SEND_COMPLETION, true);
            edgeEventData.put(OptimizeConstants.JsonKeys.REQUEST, request);

            // Add override datasetId
            if (configData.containsKey(
                    OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID)) {
                final String overrideDatasetId =
                        DataReader.getString(
                                configData,
                                OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID);
                if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                    edgeEventData.put(OptimizeConstants.JsonKeys.DATASET_ID, overrideDatasetId);
                }
            }

            final Event edgeEvent =
                    new Event.Builder(
                                    OptimizeConstants.EventNames.EDGE_PERSONALIZATION_REQUEST,
                                    OptimizeConstants.EventType.EDGE,
                                    OptimizeConstants.EventSource.REQUEST_CONTENT)
                            .setEventData(edgeEventData)
                            .chainToParentEvent(event)
                            .build();

            // In AEP Response Event handle, `requestEventId` corresponds to the unique identifier
            // for the Edge request.
            // Storing the request event unique identifier to compare and process only the
            // anticipated response in the extension.
            updateRequestEventIdsInProgress.put(edgeEvent.getUniqueIdentifier(), validScopes);

            // add the Edge event to update propositions in the events queue.
            eventsDispatcher.offer(edgeEvent);
            long timeoutMillis =
                    DataReader.getLong(eventData, OptimizeConstants.EventDataKeys.TIMEOUT);
            MobileCore.dispatchEventWithResponseCallback(
                    edgeEvent,
                    timeoutMillis,
                    new AdobeCallbackWithError<Event>() {
                        @Override
                        public void fail(final AdobeError error) {
                            // response event failed or timed out, remove this event's unique
                            // identifier from the requested event IDs dictionary and kick-off
                            // queue.
                            updateRequestEventIdsInProgress.remove(edgeEvent.getUniqueIdentifier());
                            propositionsInProgress.clear();

                            AEPOptimizeError aepOptimizeError;
                            if (error == AdobeError.CALLBACK_TIMEOUT) {
                                aepOptimizeError = AEPOptimizeError.Companion.getTimeoutError();
                            } else {
                                aepOptimizeError = AEPOptimizeError.Companion.getUnexpectedError();
                            }

                            getApi().dispatch(
                                            createResponseEventWithError(event, aepOptimizeError));

                            eventsDispatcher.resume();
                        }

                        @Override
                        public void call(final Event callbackEvent) {
                            final String requestEventId =
                                    OptimizeUtils.getRequestEventId(callbackEvent);
                            if (OptimizeUtils.isNullOrEmpty(requestEventId)) {
                                fail(AdobeError.UNEXPECTED_ERROR);
                                return;
                            }

                            final Map<String, Object> responseEventData = new HashMap<>();
                            AEPOptimizeError aepOptimizeError =
                                    updateRequestEventIdsErrors.get(requestEventId);
                            if (aepOptimizeError != null) {
                                responseEventData.put(
                                        OptimizeConstants.EventDataKeys.RESPONSE_ERROR,
                                        aepOptimizeError.toEventData());
                            }

                            final List<Map<String, Object>> propositionsList = new ArrayList<>();

                            for (Map.Entry<DecisionScope, OptimizeProposition> entry :
                                    propositionsInProgress.entrySet()) {
                                OptimizeProposition optimizeProposition = entry.getValue();
                                propositionsList.add(optimizeProposition.toEventData());
                            }

                            responseEventData.put(
                                    OptimizeConstants.EventDataKeys.PROPOSITIONS, propositionsList);

                            final Event responseEvent =
                                    new Event.Builder(
                                                    OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                                                    OptimizeConstants.EventType.OPTIMIZE,
                                                    OptimizeConstants.EventSource.RESPONSE_CONTENT)
                                            .setEventData(responseEventData)
                                            .inResponseToEvent(event)
                                            .build();

                            getApi().dispatch(responseEvent);

                            final Event updateCompleteEvent =
                                    new Event.Builder(
                                                    OptimizeConstants.EventNames
                                                            .OPTIMIZE_UPDATE_COMPLETE,
                                                    OptimizeConstants.EventType.OPTIMIZE,
                                                    OptimizeConstants.EventSource.CONTENT_COMPLETE)
                                            .setEventData(
                                                    new HashMap<String, Object>() {
                                                        {
                                                            put(
                                                                    OptimizeConstants.EventDataKeys
                                                                            .COMPLETED_UPDATE_EVENT_ID,
                                                                    requestEventId);
                                                        }
                                                    })
                                            .chainToParentEvent(event)
                                            .build();

                            getApi().dispatch(updateCompleteEvent);
                        }
                    });
        } catch (final Exception e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleUpdatePropositions - Failed to process update propositions request event"
                            + " due to an exception (%s)!",
                    e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * OptimizeConstants.EventSource#CONTENT_COMPLETE}.
     *
     * <p>The event is dispatched internally upon receiving an Edge content complete response for an
     * update propositions request.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleUpdatePropositionsCompleted(@NonNull final Event event) {
        try {
            final String requestCompletedForEventId =
                    DataReader.getString(
                            event.getEventData(),
                            OptimizeConstants.EventDataKeys.COMPLETED_UPDATE_EVENT_ID);
            if (OptimizeUtils.isNullOrEmpty(requestCompletedForEventId)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleUpdatePropositionsCompleted - Ignoring Optimize complete event,"
                                + " event Id for the completed event is not present in event data");
                return;
            }

            final List<DecisionScope> requestedScopes =
                    updateRequestEventIdsInProgress.get(requestCompletedForEventId);
            if (OptimizeUtils.isNullOrEmpty(requestedScopes)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleUpdatePropositionsCompleted - Ignoring Optimize complete event,"
                            + " event Id is not being tracked for completion as requested scopes is"
                            + " null or empty.");
                return;
            }

            // Update propositions in cache
            updateCachedPropositions(requestedScopes);

            // remove completed event's ID from the request event IDs dictionary.
            updateRequestEventIdsInProgress.remove(requestCompletedForEventId);
        } catch (final DataReaderException e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleUpdatePropositionsCompleted - Cannot process the update propositions"
                            + " complete event due to an exception (%s)!",
                    e.getLocalizedMessage());
        } finally {
            propositionsInProgress.clear();

            // Resume events dispatcher processing after update propositions request is completed.
            eventsDispatcher.resume();
        }
    }

    /**
     * Updates the in-memory propositions cache with the returned propositions.
     *
     * <p>Any requested scopes for which no propositions are returned in personalization: decisions
     * events are removed from the cache.
     *
     * @param requestedScopes a {@code List<DecisionScope>} for which propositions are requested.
     */
    private void updateCachedPropositions(@NonNull final List<DecisionScope> requestedScopes) {
        // update cache with accumulated propositions
        cachedPropositions.putAll(propositionsInProgress);

        // remove cached propositions for requested scopes for which no propositions are returned.
        final List<DecisionScope> returnedScopes = new ArrayList<>(propositionsInProgress.keySet());
        final List<DecisionScope> scopesToRemove = new ArrayList<>(requestedScopes);
        scopesToRemove.removeAll(returnedScopes);

        for (final DecisionScope scope : scopesToRemove) {
            cachedPropositions.remove(scope);
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value
     * OptimizeConstants.EventSource#EDGE_PERSONALIZATION_DECISIONS}.
     *
     * <p>This method caches the propositions, returned in the Edge response, in the SDK. It also
     * dispatches a personalization notification event with the received propositions.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleEdgeResponse(@NonNull final Event event) {
        try {
            final Map<String, Object> eventData = event.getEventData();
            final String requestEventId = OptimizeUtils.getRequestEventId(event);

            if (!OptimizeUtils.isPersonalizationDecisionsResponse(event)
                    || OptimizeUtils.isNullOrEmpty(requestEventId)
                    || !updateRequestEventIdsInProgress.containsKey(requestEventId)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleEdgeResponse - Ignoring Edge event, either handle type is not"
                            + " personalization:decisions, or the response isn't intended for this"
                            + " extension.");
                propositionsInProgress.clear();
                return;
            }

            final List<Map<String, Object>> payload =
                    DataReader.getTypedListOfMap(
                            Object.class, eventData, OptimizeConstants.Edge.PAYLOAD);
            if (OptimizeUtils.isNullOrEmpty(payload)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleEdgeResponse - Cannot process the Edge personalization:decisions"
                                + " event, propositions list is either null or empty in the Edge"
                                + " response.");
                return;
            }

            final Map<DecisionScope, OptimizeProposition> propositionsMap = new HashMap<>();
            for (final Map<String, Object> propositionData : payload) {
                final OptimizeProposition optimizeProposition =
                        OptimizeProposition.fromEventData(propositionData);
                if (optimizeProposition != null
                        && !OptimizeUtils.isNullOrEmpty(optimizeProposition.getOffers())) {
                    final DecisionScope scope = new DecisionScope(optimizeProposition.getScope());
                    propositionsMap.put(scope, optimizeProposition);
                }
            }

            if (OptimizeUtils.isNullOrEmpty(propositionsMap)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleEdgeResponse - Cannot process the Edge personalization:decisions"
                            + " event, no propositions with valid offers are present in the Edge"
                            + " response.");
                return;
            }

            // accumulate propositions in in-progress propositions dictionary
            propositionsInProgress.putAll(propositionsMap);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            for (final OptimizeProposition optimizeProposition : propositionsMap.values()) {
                propositionsList.add(optimizeProposition.toEventData());
            }
            final Map<String, Object> notificationData = new HashMap<>();
            notificationData.put(OptimizeConstants.EventDataKeys.PROPOSITIONS, propositionsList);

            final Event edgeEvent =
                    new Event.Builder(
                                    OptimizeConstants.EventNames.OPTIMIZE_NOTIFICATION,
                                    OptimizeConstants.EventType.OPTIMIZE,
                                    OptimizeConstants.EventSource.NOTIFICATION)
                            .setEventData(notificationData)
                            .build();

            // Dispatch notification event
            getApi().dispatch(edgeEvent);
        } catch (final Exception e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleEdgeResponse - Cannot process the Edge personalization:decisions event"
                            + " due to an exception (%s)!",
                    e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value
     * OptimizeConstants.EventSource#ERROR_RESPONSE_CONTENT}.
     *
     * <p>This method logs the error information, returned in Edge response, specifying error type
     * along with a detail message.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleEdgeErrorResponse(@NonNull final Event event) {
        try {
            final Map<String, Object> eventData = event.getEventData();
            final String requestEventId = OptimizeUtils.getRequestEventId(event);

            if (!OptimizeUtils.isEdgeErrorResponseContent(event)
                    || OptimizeUtils.isNullOrEmpty(requestEventId)
                    || !updateRequestEventIdsInProgress.containsKey(requestEventId)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleEdgeResponse - Ignoring Edge event, either handle type is not edge"
                                + " error response content, or the response isn't intended for this"
                                + " extension.");
                return;
            }

            if (OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleEdgeErrorResponse - Ignoring the Edge error response event, either"
                                + " event is null or event data is null/ empty.");
                return;
            }

            final String errorType =
                    DataReader.optString(
                            eventData,
                            OptimizeConstants.Edge.ErrorKeys.TYPE,
                            OptimizeConstants.ERROR_UNKNOWN);
            final int errorStatus =
                    DataReader.optInt(
                            eventData,
                            OptimizeConstants.Edge.ErrorKeys.STATUS,
                            OptimizeConstants.UNKNOWN_STATUS);
            final String errorTitle =
                    DataReader.optString(
                            eventData,
                            OptimizeConstants.Edge.ErrorKeys.TITLE,
                            OptimizeConstants.ERROR_UNKNOWN);
            final String errorDetail =
                    DataReader.optString(
                            eventData,
                            OptimizeConstants.Edge.ErrorKeys.DETAIL,
                            OptimizeConstants.ERROR_UNKNOWN);
            final Map<String, Object> errorReport =
                    DataReader.optTypedMap(
                            Object.class,
                            eventData,
                            OptimizeConstants.Edge.ErrorKeys.REPORT,
                            new HashMap<>());

            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleEdgeErrorResponse - Decisioning Service error! Error type: (%s),\n"
                            + "title: (%s),\n"
                            + "detail: (%s),\n"
                            + "status: (%s),\n"
                            + "report: (%s)",
                    errorType,
                    errorTitle,
                    errorDetail,
                    errorStatus,
                    errorReport);

            // Check if the errorStatus is in the list of recoverable error codes
            if (recoverableNetworkErrorCodes.contains(errorStatus)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "Recoverable error encountered: Status %d",
                        errorStatus);
                return;
            } else {
                AEPOptimizeError aepOptimizeError =
                        new AEPOptimizeError(
                                errorType, errorStatus, errorTitle, errorDetail, errorReport, null);
                updateRequestEventIdsErrors.put(requestEventId, aepOptimizeError);
            }
        } catch (final Exception e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleEdgeResponse - Cannot process the Edge Error Response event"
                            + " due to an exception (%s)!",
                    e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     * <p>This method caches the propositions, returned in the Edge response, in the SDK. It also
     * dispatches an optimize response event with the propositions for the requested decision
     * scopes.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleGetPropositions(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();

        try {
            final List<Map<String, Object>> decisionScopesData =
                    DataReader.getTypedListOfMap(
                            Object.class,
                            eventData,
                            OptimizeConstants.EventDataKeys.DECISION_SCOPES);
            final List<DecisionScope> validScopes = retrieveValidDecisionScopes(decisionScopesData);
            if (OptimizeUtils.isNullOrEmpty(validScopes)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleGetPropositions - Cannot process the get propositions request event,"
                                + " provided list of decision scopes has no valid scope.");
                getApi().dispatch(createResponseEventWithError(event, AdobeError.UNEXPECTED_ERROR));
                return;
            }

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            for (final DecisionScope scope : validScopes) {
                if (cachedPropositions.containsKey(scope)) {
                    final OptimizeProposition optimizeProposition = cachedPropositions.get(scope);
                    propositionsList.add(optimizeProposition.toEventData());
                }
            }

            final List<Map<String, Object>> previewPropositionsList = new ArrayList<>();
            for (final DecisionScope scope : validScopes) {
                if (previewCachedPropositions.containsKey(scope)) {
                    final OptimizeProposition optimizeProposition =
                            previewCachedPropositions.get(scope);
                    previewPropositionsList.add(optimizeProposition.toEventData());
                }
            }

            final Map<String, Object> responseEventData = new HashMap<>();

            if (!previewPropositionsList.isEmpty()) {
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG, "Preview Mode is enabled.");
                responseEventData.put(
                        OptimizeConstants.EventDataKeys.PROPOSITIONS, previewPropositionsList);
            } else {
                responseEventData.put(
                        OptimizeConstants.EventDataKeys.PROPOSITIONS, propositionsList);
            }

            final Event responseEvent =
                    new Event.Builder(
                                    OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                                    OptimizeConstants.EventType.OPTIMIZE,
                                    OptimizeConstants.EventSource.RESPONSE_CONTENT)
                            .setEventData(responseEventData)
                            .inResponseToEvent(event)
                            .build();

            getApi().dispatch(responseEvent);

        } catch (final Exception e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleGetPropositions - Failed to process get propositions request event due"
                            + " to an exception (%s)!",
                    e.getLocalizedMessage());
            getApi().dispatch(createResponseEventWithError(event, AdobeError.UNEXPECTED_ERROR));
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     *
     * <p>This method dispatches an event to the Edge network extension to send proposition
     * interactions information to the Experience Edge network. The dispatched event may contain an
     * override {@code datasetId} indicating the dataset which will be used for storing the
     * Experience Events sent to the Edge network.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleTrackPropositions(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();

        final Map<String, Object> configData = retrieveConfigurationSharedState(event);
        if (OptimizeUtils.isNullOrEmpty(configData)) {
            Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleTrackPropositions - Cannot process the track propositions request event,"
                            + " Configuration shared state is not available.");
            return;
        }

        try {
            final Map<String, Object> propositionInteractionsXdm =
                    DataReader.getTypedMap(
                            Object.class,
                            eventData,
                            OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS);
            if (OptimizeUtils.isNullOrEmpty(propositionInteractionsXdm)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleTrackPropositions - Cannot process the track propositions request"
                            + " event, provided proposition interactions map is null or empty.");
                return;
            }

            final Map<String, Object> edgeEventData = new HashMap<>();
            edgeEventData.put(OptimizeConstants.JsonKeys.XDM, propositionInteractionsXdm);

            // Add override datasetId
            if (configData.containsKey(
                    OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID)) {
                final String overrideDatasetId =
                        DataReader.getString(
                                configData,
                                OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID);
                if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                    edgeEventData.put(OptimizeConstants.JsonKeys.DATASET_ID, overrideDatasetId);
                }
            }

            final Event edgeEvent =
                    new Event.Builder(
                                    OptimizeConstants.EventNames
                                            .EDGE_PROPOSITION_INTERACTION_REQUEST,
                                    OptimizeConstants.EventType.EDGE,
                                    OptimizeConstants.EventSource.REQUEST_CONTENT)
                            .setEventData(edgeEventData)
                            .build();

            getApi().dispatch(edgeEvent);

        } catch (final Exception e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleTrackPropositions - Failed to process track propositions request event"
                            + " due to an exception (%s)!",
                    e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * OptimizeConstants.EventSource#REQUEST_RESET}.
     *
     * <p>This method clears previously cached propositions in the SDK.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleClearPropositions(@NonNull final Event event) {
        cachedPropositions.clear();
        previewCachedPropositions.clear();
    }

    /**
     * Handles the event with type {@value EventType#SYSTEM} and source {@value
     * OptimizeConstants.EventSource#DEBUG}.
     *
     * <p>A debug event allows the optimize extension to processes non-production workflows.
     *
     * @param event the debug {@link Event} to be handled.
     */
    void handleDebugEvent(@NonNull final Event event) {
        try {
            if (OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleDebugEvent - Ignoring the Optimize Debug event, either event is null"
                                + " or event data is null/ empty.");
                return;
            }

            if (!OptimizeUtils.isPersonalizationDebugEvent(event)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleDebugEvent - Ignoring Optimize Debug event, either handle type is"
                                + " not com.adobe.eventType.system or source is not"
                                + " com.adobe.eventSource.debug");
                return;
            }

            final Map<String, Object> eventData = event.getEventData();

            final List<Map<String, Object>> payload =
                    DataReader.getTypedListOfMap(
                            Object.class, eventData, OptimizeConstants.Edge.PAYLOAD);
            if (OptimizeUtils.isNullOrEmpty(payload)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleDebugEvent - Cannot process the Debug event, propositions list is"
                                + " either null or empty in the response.");
                return;
            }

            final Map<DecisionScope, OptimizeProposition> propositionsMap = new HashMap<>();
            for (final Map<String, Object> propositionData : payload) {
                final OptimizeProposition optimizeProposition =
                        OptimizeProposition.fromEventData(propositionData);
                if (optimizeProposition != null
                        && !OptimizeUtils.isNullOrEmpty(optimizeProposition.getOffers())) {
                    final DecisionScope scope = new DecisionScope(optimizeProposition.getScope());
                    propositionsMap.put(scope, optimizeProposition);
                }
            }

            if (OptimizeUtils.isNullOrEmpty(propositionsMap)) {
                Log.debug(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleDebugEvent - Cannot process the Debug event, no propositions with"
                                + " valid offers are present in the response.");
                return;
            }

            previewCachedPropositions.putAll(propositionsMap);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            for (final OptimizeProposition optimizeProposition : propositionsMap.values()) {
                propositionsList.add(optimizeProposition.toEventData());
            }
            final Map<String, Object> notificationData = new HashMap<>();
            notificationData.put(OptimizeConstants.EventDataKeys.PROPOSITIONS, propositionsList);

            final Event notificationEvent =
                    new Event.Builder(
                                    OptimizeConstants.EventNames.OPTIMIZE_NOTIFICATION,
                                    OptimizeConstants.EventType.OPTIMIZE,
                                    OptimizeConstants.EventSource.NOTIFICATION)
                            .setEventData(notificationData)
                            .build();

            // Dispatch notification event
            getApi().dispatch(notificationEvent);
        } catch (final Exception e) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleDebugEvent - Cannot process the Debug event due to an exception (%s)!",
                    e.getLocalizedMessage());
        }
    }

    /**
     * Retrieves the {@code Configuration} shared state versioned at the current {@code event}.
     *
     * @param event incoming {@link Event} instance.
     * @return {@code Map<String, Object>} containing configuration data.
     */
    private Map<String, Object> retrieveConfigurationSharedState(final Event event) {
        SharedStateResult configurationSharedState =
                getApi().getSharedState(
                                OptimizeConstants.Configuration.EXTENSION_NAME,
                                event,
                                false,
                                SharedStateResolution.ANY);
        return configurationSharedState != null ? configurationSharedState.getValue() : null;
    }

    /**
     * Retrieves the {@code List<DecisionScope>} containing valid scopes.
     *
     * <p>This method returns null if the given {@code decisionScopesData} list is null, or empty,
     * or if there is no valid decision scope in the provided list.
     *
     * @param decisionScopesData input {@code List<Map<String, Object>>} containing scope data.
     * @return {@code List<DecisionScope>} containing valid scopes.
     * @see DecisionScope#isValid()
     */
    private List<DecisionScope> retrieveValidDecisionScopes(
            final List<Map<String, Object>> decisionScopesData) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopesData)) {
            Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "retrieveValidDecisionScopes - No valid decision scopes are retrieved, provided"
                            + " decision scopes list is null or empty.");
            return null;
        }

        final List<DecisionScope> validScopes = new ArrayList<>();
        for (final Map<String, Object> scopeData : decisionScopesData) {
            final DecisionScope scope = DecisionScope.fromEventData(scopeData);
            if (scope == null || !scope.isValid()) {
                continue;
            }
            validScopes.add(scope);
        }

        if (validScopes.size() == 0) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "retrieveValidDecisionScopes - No valid decision scopes are retrieved, provided"
                            + " list of decision scopes has no valid scope.");
            return null;
        }

        return validScopes;
    }

    /**
     * Creates {@value OptimizeConstants.EventType#OPTIMIZE}, {@value
     * OptimizeConstants.EventSource#RESPONSE_CONTENT} event with the given {@code error} in event
     * data.
     *
     * @return {@link Event} instance.
     */
    private Event createResponseEventWithError(final Event event, final AdobeError error) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.RESPONSE_ERROR, error.getErrorCode());

        return new Event.Builder(
                        OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                        OptimizeConstants.EventType.OPTIMIZE,
                        OptimizeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(eventData)
                .inResponseToEvent(event)
                .build();
    }

    private Event createResponseEventWithError(final Event event, final AEPOptimizeError error) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.RESPONSE_ERROR, error);

        return new Event.Builder(
                        OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                        OptimizeConstants.EventType.OPTIMIZE,
                        OptimizeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(eventData)
                .inResponseToEvent(event)
                .build();
    }

    @VisibleForTesting
    Map<DecisionScope, OptimizeProposition> getCachedPropositions() {
        return cachedPropositions;
    }

    @VisibleForTesting
    void setCachedPropositions(final Map<DecisionScope, OptimizeProposition> cachedPropositions) {
        this.cachedPropositions = cachedPropositions;
    }

    @VisibleForTesting
    Map<DecisionScope, OptimizeProposition> getPreviewCachedPropositions() {
        return previewCachedPropositions;
    }

    @VisibleForTesting
    void setPreviewCachedPropositions(
            final Map<DecisionScope, OptimizeProposition> previewCachedPropositions) {
        this.previewCachedPropositions = previewCachedPropositions;
    }

    @VisibleForTesting
    Map<DecisionScope, OptimizeProposition> getPropositionsInProgress() {
        return propositionsInProgress;
    }

    @VisibleForTesting
    void setPropositionsInProgress(
            final Map<DecisionScope, OptimizeProposition> propositionsInProgress) {
        this.propositionsInProgress.clear();
        this.propositionsInProgress.putAll(propositionsInProgress);
    }

    @VisibleForTesting
    Map<String, List<DecisionScope>> getUpdateRequestEventIdsInProgress() {
        return updateRequestEventIdsInProgress;
    }

    @VisibleForTesting
    void setUpdateRequestEventIdsInProgress(
            final String eventId, final List<DecisionScope> expectedScopes) {
        updateRequestEventIdsInProgress.put(eventId, expectedScopes);
    }

    @VisibleForTesting
    void setEventsDispatcher(final SerialWorkDispatcher<Event> eventsDispatcher) {
        this.eventsDispatcher = eventsDispatcher;
    }
}
