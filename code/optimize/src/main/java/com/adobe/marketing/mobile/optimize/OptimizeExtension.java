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

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

class OptimizeExtension extends Extension {

    private static final String SELF_TAG = "OptimizeExtension";
    private Map<DecisionScope, Proposition> cachedPropositions;

    // List containing the schema strings for the proposition items supported by the SDK, sent in the personalization query request.
    final static List<String> supportedSchemas = Arrays.asList(
            // Target schemas
            OptimizeConstants.JsonValues.SCHEMA_TARGET_HTML,
            OptimizeConstants.JsonValues.SCHEMA_TARGET_JSON,
            OptimizeConstants.JsonValues.SCHEMA_TARGET_DEFAULT,

            // Offer Decisioning schemas
            OptimizeConstants.JsonValues.SCHEMA_OFFER_HTML,
            OptimizeConstants.JsonValues.SCHEMA_OFFER_JSON,
            OptimizeConstants.JsonValues.SCHEMA_OFFER_IMAGE,
            OptimizeConstants.JsonValues.SCHEMA_OFFER_TEXT
    );

    /**
     * Constructor for {@code OptimizeExtension}.
     * <p>
     * It is invoked during the extension registration to retrieve the extension's details such as name and version. The following {@link Event}
     * listeners are registered during the process.
     * <ul>
     *     <li>
     *         Listener for {@code Event} type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}
     *         Listener for {@code Event} type {@value OptimizeConstants.EventType#EDGE} and source {@value OptimizeConstants.EventSource#EDGE_PERSONALIZATION_DECISIONS}
     *         Listener for {@code Event} type {@value OptimizeConstants.EventType#EDGE} and source {@value OptimizeConstants.EventSource#ERROR_RESPONSE_CONTENT}
     *         Listener for {@code Event} type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_RESET}
     *         Listener for {@code Event} type {@value OptimizeConstants.EventType#GENERIC_IDENTITY} and source {@value OptimizeConstants.EventSource#REQUEST_RESET}
     *     </li>
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance.
     */
    protected OptimizeExtension(final ExtensionApi extensionApi) {
        super(extensionApi);

        cachedPropositions = new HashMap<>();
    }

    @Override
    protected void onRegistered() {
        getApi().registerEventListener(OptimizeConstants.EventType.OPTIMIZE, OptimizeConstants.EventSource.REQUEST_CONTENT, this::handleOptimizeRequestContent);

        getApi().registerEventListener(OptimizeConstants.EventType.EDGE, OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS, this::handleEdgeResponse);

        getApi().registerEventListener(OptimizeConstants.EventType.EDGE, OptimizeConstants.EventSource.ERROR_RESPONSE_CONTENT, this::handleEdgeErrorResponse);

        getApi().registerEventListener(OptimizeConstants.EventType.OPTIMIZE, OptimizeConstants.EventSource.REQUEST_RESET, this::handleClearPropositions);

        // Register listener - Mobile Core `resetIdentities()` API dispatches generic identity request reset event.
        getApi().registerEventListener(OptimizeConstants.EventType.GENERIC_IDENTITY, OptimizeConstants.EventSource.REQUEST_RESET, this::handleClearPropositions);

    }

    @Override
    public boolean readyForEvent(@NonNull final Event event) {
        if (OptimizeConstants.EventType.OPTIMIZE.equalsIgnoreCase(event.getType())
                && OptimizeConstants.EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())) {
            SharedStateResult configurationSharedState = getApi().getSharedState(OptimizeConstants.Configuration.EXTENSION_NAME, event, false, SharedStateResolution.ANY);
            return configurationSharedState != null && configurationSharedState.getStatus() == SharedStateStatus.SET;
        }
        return true;
    }

    /**
     * Retrieve the extension name.
     *
     * @return {@link String} containing the unique name for this extension.
     */
    @NonNull
    @Override
    protected String getName() {
        return OptimizeConstants.EXTENSION_NAME;
    }

    /**
     * Retrieve the extension version.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @NonNull
    @Override
    protected String getVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * Retrieve the friendly name.
     *
     * @return {@link String} containing the friendly name for this extension.
     */
    @NonNull
    @Override
    protected String getFriendlyName() {
        return OptimizeConstants.FRIENDLY_NAME;
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * <p>
     * This method handles the event based on the value of {@value OptimizeConstants.EventDataKeys#REQUEST_TYPE} in the event data of current {@code event}
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleOptimizeRequestContent(@NonNull final Event event) {
        if (OptimizeUtils.isNullOrEmpty(event.getEventData())) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleOptimizeRequestContent - Ignoring the Optimize request event, either event is null or event data is null/ empty.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        final String requestType = DataReader.optString(eventData, OptimizeConstants.EventDataKeys.REQUEST_TYPE, "");

        switch (requestType) {
            case OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE:
                handleUpdatePropositions(event);
                break;
            case OptimizeConstants.EventDataValues.REQUEST_TYPE_GET:
                handleGetPropositions(event);
                break;
            case OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK:
                handleTrackPropositions(event);
                break;
            default:
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                        "handleOptimizeRequestContent - Ignoring the Optimize request event, provided request type (%s) is not handled by this extension.", requestType);
                break;
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * <p>
     * This method dispatches an event to the Edge network extension to send personalization query request to the Experience Edge network. The dispatched event
     * contains additional XDM and/ or free-form data, read from the incoming event, to be attached to the Edge request.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleUpdatePropositions(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();

        final Map<String, Object> configData = retrieveConfigurationSharedState(event);
        if (OptimizeUtils.isNullOrEmpty(configData)) {
            Log.debug(OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "handleUpdatePropositions - Cannot process the update propositions request event, Configuration shared state is not available.");
            return;
        }

        try {
            final List<Map<String, Object>> decisionScopesData = DataReader.getTypedListOfMap(Object.class, eventData, OptimizeConstants.EventDataKeys.DECISION_SCOPES);
            final List<String> validScopeNames = retrieveValidDecisionScopes(decisionScopesData);
            if (OptimizeUtils.isNullOrEmpty(validScopeNames)) {
                Log.debug(OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "handleUpdatePropositions - Cannot process the update propositions request event, provided list of decision scopes has no valid scope.");
                return;
            }

            final Map<String, Object> edgeEventData = new HashMap<>();

            // Add query
            final Map<String, Object> queryPersonalization = new HashMap<>();
            queryPersonalization.put(OptimizeConstants.JsonKeys.SCHEMAS, supportedSchemas);
            queryPersonalization.put(OptimizeConstants.JsonKeys.DECISION_SCOPES, validScopeNames);
            final Map<String, Object> query = new HashMap<>();
            query.put(OptimizeConstants.JsonKeys.QUERY_PERSONALIZATION, queryPersonalization);
            edgeEventData.put(OptimizeConstants.JsonKeys.QUERY, query);

            // Add xdm
            final Map<String, Object> xdm = new HashMap<>();
            if (eventData.containsKey(OptimizeConstants.EventDataKeys.XDM)) {
                final Map<String, Object> inputXdm = DataReader.getTypedMap(Object.class, eventData, OptimizeConstants.EventDataKeys.XDM);
                if (!OptimizeUtils.isNullOrEmpty(inputXdm)) {
                    xdm.putAll(inputXdm);
                }
            }
            xdm.put(OptimizeConstants.JsonKeys.EXPERIENCE_EVENT_TYPE, OptimizeConstants.JsonValues.EE_EVENT_TYPE_PERSONALIZATION);
            edgeEventData.put(OptimizeConstants.JsonKeys.XDM, xdm);

            // Add data
            final Map<String, Object> data = new HashMap<>();
            if (eventData.containsKey(OptimizeConstants.EventDataKeys.DATA)) {
                final Map<String, Object> inputData = DataReader.getTypedMap(Object.class, eventData, OptimizeConstants.EventDataKeys.DATA);
                if (!OptimizeUtils.isNullOrEmpty(inputData)) {
                    data.putAll(inputData);
                    edgeEventData.put(OptimizeConstants.JsonKeys.DATA, data);
                }
            }

            // Add override datasetId
            if (configData.containsKey(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID)) {
                final String overrideDatasetId = DataReader.getString(configData, OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID);
                if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                    edgeEventData.put(OptimizeConstants.JsonKeys.DATASET_ID, overrideDatasetId);
                }
            }

            final Event edgeEvent = new Event.Builder(OptimizeConstants.EventNames.EDGE_PERSONALIZATION_REQUEST,
                    OptimizeConstants.EventType.EDGE,
                    OptimizeConstants.EventSource.REQUEST_CONTENT)
                    .setEventData(edgeEventData)
                    .build();

            getApi().dispatch(edgeEvent);
        } catch (final Exception e) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleUpdatePropositions - Failed to process update propositions request event due to an exception (%s)!", e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value OptimizeConstants.EventSource#EDGE_PERSONALIZATION_DECISIONS}.
     * <p>
     * This method caches the propositions, returned in the Edge response, in the SDK. It also dispatches a personalization notification event with the
     * received propositions.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleEdgeResponse(@NonNull final Event event) {
        if (OptimizeUtils.isNullOrEmpty(event.getEventData())) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleEdgeResponse - Ignoring the Edge personalization:decisions event, either event is null or event data is null/ empty.");
            return;
        }

        try {
            final Map<String, Object> eventData = event.getEventData();

            // Verify the Edge response event handle
            final String edgeEventHandleType = DataReader.getString(eventData, OptimizeConstants.Edge.EVENT_HANDLE);
            if (!OptimizeConstants.Edge.EVENT_HANDLE_TYPE_PERSONALIZATION.equals(edgeEventHandleType)) {
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                        "handleEdgeResponse - Cannot process the Edge personalization:decisions event, event handle type is not personalization:decisions.");
                return;
            }

            final List<Map<String, Object>> payload = DataReader.getTypedListOfMap(Object.class, eventData, OptimizeConstants.Edge.PAYLOAD);
            if (OptimizeUtils.isNullOrEmpty(payload)) {
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG, "handleEdgeResponse - Cannot process the Edge personalization:decisions event, propositions list is either null or empty in the Edge response.");
                return;
            }

            final Map<DecisionScope, Proposition> propositionsMap = new HashMap<>();
            for (final Map<String, Object> propositionData : payload) {
                final Proposition proposition = Proposition.fromEventData(propositionData);
                if (proposition != null && !OptimizeUtils.isNullOrEmpty(proposition.getOffers())) {
                    final DecisionScope scope = new DecisionScope(proposition.getScope());
                    propositionsMap.put(scope, proposition);
                }
            }

            if (OptimizeUtils.isNullOrEmpty(propositionsMap)) {
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG, "handleEdgeResponse - Cannot process the Edge personalization:decisions event, no propositions with valid offers are present in the Edge response.");
                return;
            }

            // Update propositions cache
            cachedPropositions.putAll(propositionsMap);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            for (final Proposition proposition : propositionsMap.values()) {
                propositionsList.add(proposition.toEventData());
            }
            final Map<String, Object> notificationData = new HashMap<>();
            notificationData.put(OptimizeConstants.EventDataKeys.PROPOSITIONS, propositionsList);

            final Event edgeEvent = new Event.Builder(OptimizeConstants.EventNames.OPTIMIZE_NOTIFICATION,
                    OptimizeConstants.EventType.OPTIMIZE,
                    OptimizeConstants.EventSource.NOTIFICATION)
                    .setEventData(notificationData)
                    .build();

            // Dispatch notification event
            getApi().dispatch(edgeEvent);
        }  catch (final Exception e) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleEdgeResponse - Cannot process the Edge personalization:decisions event due to an exception (%s)!", e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value OptimizeConstants.EventSource#ERROR_RESPONSE_CONTENT}.
     * <p>
     * This method logs the error information, returned in Edge response, specifying error type along with a detail message.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleEdgeErrorResponse(@NonNull final Event event) {
        if (OptimizeUtils.isNullOrEmpty(event.getEventData())) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleEdgeErrorResponse - Ignoring the Edge error response event, either event is null or event data is null/ empty.");
            return;
        }
        final Map<String, Object> eventData = event.getEventData();

        final String errorType = DataReader.optString(eventData, OptimizeConstants.Edge.ErrorKeys.TYPE, OptimizeConstants.ERROR_UNKNOWN);
        final String errorDetail = DataReader.optString(eventData, OptimizeConstants.Edge.ErrorKeys.DETAIL, OptimizeConstants.ERROR_UNKNOWN);

        Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG,
                "handleEdgeErrorResponse - Decisioning Service error! Error type: (%s), detail: (%s)", errorType, errorDetail);
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * <p>
     * This method caches the propositions, returned in the Edge response, in the SDK. It also dispatches an optimize response event with the
     * propositions for the requested decision scopes.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleGetPropositions(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();
        
        try {
            final List<Map<String, Object>> decisionScopesData = DataReader.getTypedListOfMap(Object.class, eventData, OptimizeConstants.EventDataKeys.DECISION_SCOPES);
            final List<String> validScopeNames = retrieveValidDecisionScopes(decisionScopesData);
            if (OptimizeUtils.isNullOrEmpty(validScopeNames)) {
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                        "handleGetPropositions - Cannot process the get propositions request event, provided list of decision scopes has no valid scope.");
                getApi().dispatch(createResponseEventWithError(event, AdobeError.UNEXPECTED_ERROR));
                return;
            }

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            for (final String scopeName : validScopeNames) {
                final DecisionScope scope = new DecisionScope(scopeName);
                if (cachedPropositions.containsKey(scope)) {
                    final Proposition proposition = cachedPropositions.get(scope);
                    propositionsList.add(proposition.toEventData());
                }
            }

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put(OptimizeConstants.EventDataKeys.PROPOSITIONS, propositionsList);

            final Event responseEvent = new Event.Builder(OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                    OptimizeConstants.EventType.OPTIMIZE,
                    OptimizeConstants.EventSource.RESPONSE_CONTENT)
                    .setEventData(responseEventData)
                    .inResponseToEvent(event)
                    .build();

            getApi().dispatch(responseEvent);

        } catch (final Exception e) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleGetPropositions - Failed to process get propositions request event due to an exception (%s)!", e.getLocalizedMessage());
            getApi().dispatch(createResponseEventWithError(event, AdobeError.UNEXPECTED_ERROR));
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * <p>
     * This method dispatches an event to the Edge network extension to send proposition interactions information to the Experience Edge network.
     * The dispatched event may contain an override {@code datasetId} indicating the dataset which will be used for storing the Experience Events
     * sent to the Edge network.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleTrackPropositions(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();

        final Map<String, Object> configData = retrieveConfigurationSharedState(event);
        if (OptimizeUtils.isNullOrEmpty(configData)) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleTrackPropositions - Cannot process the track propositions request event, Configuration shared state is not available.");
            return;
        }

        try {
            final Map<String, Object> propositionInteractionsXdm = DataReader.getTypedMap(Object.class, eventData, OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS);
            if (OptimizeUtils.isNullOrEmpty(propositionInteractionsXdm)) {
                Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG,
                        "handleTrackPropositions - Cannot process the track propositions request event, provided proposition interactions map is null or empty.");
                return;
            }

            final Map<String, Object> edgeEventData = new HashMap<>();
            edgeEventData.put(OptimizeConstants.JsonKeys.XDM, propositionInteractionsXdm);

            // Add override datasetId
            if (configData.containsKey(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID)) {
                final String overrideDatasetId = DataReader.getString(configData, OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID);
                if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                    edgeEventData.put(OptimizeConstants.JsonKeys.DATASET_ID, overrideDatasetId);
                }
            }

            final Event edgeEvent = new Event.Builder(OptimizeConstants.EventNames.EDGE_PROPOSITION_INTERACTION_REQUEST,
                    OptimizeConstants.EventType.EDGE,
                    OptimizeConstants.EventSource.REQUEST_CONTENT)
                    .setEventData(edgeEventData)
                    .build();

            getApi().dispatch(edgeEvent);

        } catch (final Exception e) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "handleTrackPropositions - Failed to process track propositions request event due to an exception (%s)!", e.getLocalizedMessage());
        }
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_RESET}.
     * <p>
     * This method clears previously cached propositions in the SDK.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleClearPropositions(@NonNull final Event event) {
        cachedPropositions.clear();
    }

    /**
     * Retrieves the {@code Configuration} shared state versioned at the current {@code event}.
     *
     * @param event incoming {@link Event} instance.
     * @return {@code Map<String, Object>} containing configuration data.
     */
    private Map<String, Object> retrieveConfigurationSharedState(final Event event) {
        SharedStateResult configurationSharedState = getApi().getSharedState(OptimizeConstants.Configuration.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.ANY);
        return configurationSharedState != null ? configurationSharedState.getValue() : null;
    }

    /**
     * Retrieves the {@code List<String>} containing valid scope names.
     * <p>
     * This method returns null if the given {@code decisionScopesData} list is null, or empty, or if there is no valid decision scope in the
     * provided list.
     *
     * @param decisionScopesData input {@code List<Map<String, Object>>} containing scope data.
     * @return {@code List<String>} containing valid scope names.
     * @see DecisionScope#isValid()
     */
    private List<String> retrieveValidDecisionScopes(final List<Map<String, Object>> decisionScopesData) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopesData)) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG, "retrieveValidDecisionScopes - No valid decision scopes are retrieved, provided decision scopes list is null or empty.");
            return null;
        }

        final List<String> validScopeNames = new ArrayList<>();
        for (final Map<String, Object> scopeData: decisionScopesData) {
            final DecisionScope scope = DecisionScope.fromEventData(scopeData);
            if (scope == null || !scope.isValid()) {
                continue;
            }
            validScopeNames.add(scope.getName());
        }

        if (validScopeNames.size() == 0) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "retrieveValidDecisionScopes - No valid decision scopes are retrieved, provided list of decision scopes has no valid scope.");
            return null;
        }

        return validScopeNames;
    }

    /**
     * Creates {@value OptimizeConstants.EventType#OPTIMIZE}, {@value OptimizeConstants.EventSource#RESPONSE_CONTENT} event with
     * the given {@code error} in event data.
     *
     * @return {@link Event} instance.
     */
    private Event createResponseEventWithError(final Event event, final AdobeError error) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.RESPONSE_ERROR, error.getErrorCode());

        return new Event.Builder(OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(eventData)
                .inResponseToEvent(event)
                .build();
    }

    @VisibleForTesting
    Map<DecisionScope, Proposition> getCachedPropositions() {
        return cachedPropositions;
    }

    @VisibleForTesting
    void setCachedPropositions(final Map<DecisionScope, Proposition> cachedPropositions) {
        this.cachedPropositions = cachedPropositions;
    }
}