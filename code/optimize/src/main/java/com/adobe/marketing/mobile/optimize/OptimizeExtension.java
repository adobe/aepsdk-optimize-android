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
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.adobe.marketing.mobile.optimize.OptimizeConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.optimize.OptimizeConstants.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.optimize.OptimizeConstants.LOG_TAG;

class OptimizeExtension extends Extension {
    private final Object executorMutex = new Object();
    private ExecutorService executorService;

    private final Map<DecisionScope, Proposition> cachedPropositions;

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

        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, LOG_TAG,
                        String.format("Failed to register event listener for Optimize extension due to an error (%s)!",
                                extensionError.getErrorName()));
            }
        };

        extensionApi.registerEventListener(OptimizeConstants.EventType.OPTIMIZE, OptimizeConstants.EventSource.REQUEST_CONTENT,
                ListenerOptimizeRequestContent.class, errorCallback);

        extensionApi.registerEventListener(OptimizeConstants.EventType.EDGE, OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS,
                ListenerEdgeResponseContent.class, errorCallback);

        extensionApi.registerEventListener(OptimizeConstants.EventType.EDGE, OptimizeConstants.EventSource.ERROR_RESPONSE_CONTENT,
                ListenerEdgeErrorResponseContent.class, errorCallback);

        extensionApi.registerEventListener(OptimizeConstants.EventType.OPTIMIZE, OptimizeConstants.EventSource.REQUEST_RESET,
                ListenerOptimizeRequestReset.class, errorCallback);

        // Register listener - Mobile Core `resetIdentities()` API dispatches generic identity request reset event.
        extensionApi.registerEventListener(OptimizeConstants.EventType.GENERIC_IDENTITY, OptimizeConstants.EventSource.REQUEST_RESET,
                ListenerGenericIdentityRequestReset.class, errorCallback);
    }

    /**
     * Retrieve the extension name.
     *
     * @return {@link String} containing the unique name for this extension.
     */
    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Retrieve the extension version.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * <p>
     * This method dispatches an event to the Edge network extension to send personalization query request to the Experience Edge network. The dispatched event
     * contains additional XDM and/ or free-form data, read from the incoming event, to be attached to the Edge request.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleUpdatePropositions(final Event event) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (event == null || OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the update propositions request event, event is null or event data is null/ empty.");
                    return;
                }
                final Map<String, Object> eventData = event.getEventData();

                final Map<String, Object> configData = retrieveConfigurationSharedState(event);
                if (OptimizeUtils.isNullOrEmpty(configData)) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the update propositions request event, Configuration shared state is not available.");
                    return;
                }

                try {
                    final List<Map<String, Object>> decisionScopesData = (List<Map<String, Object>>) eventData.get(OptimizeConstants.EventDataKeys.DECISION_SCOPES);
                    final List<String> validScopeNames = retrieveValidDecisionScopes(decisionScopesData);
                    if (OptimizeUtils.isNullOrEmpty(validScopeNames)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the update propositions request event, provided list of decision scopes has no valid scope.");
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
                        final Map<String, Object> inputXdm = (Map<String, Object>) eventData.get(OptimizeConstants.EventDataKeys.XDM);
                        if (!OptimizeUtils.isNullOrEmpty(inputXdm)) {
                            xdm.putAll(inputXdm);
                        }
                    }
                    xdm.put(OptimizeConstants.JsonKeys.EXPERIENCE_EVENT_TYPE, OptimizeConstants.JsonValues.EE_EVENT_TYPE_PERSONALIZATION);
                    edgeEventData.put(OptimizeConstants.JsonKeys.XDM, xdm);

                    // Add data
                    final Map<String, Object> data = new HashMap<>();
                    if (eventData.containsKey(OptimizeConstants.EventDataKeys.DATA)) {
                        final Map<String, Object> inputData = (Map<String, Object>) eventData.get(OptimizeConstants.EventDataKeys.DATA);
                        if (!OptimizeUtils.isNullOrEmpty(inputData)) {
                            data.putAll(inputData);
                            edgeEventData.put(OptimizeConstants.JsonKeys.DATA, data);
                        }
                    }

                    // Add override datasetId
                    if (configData.containsKey(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID)) {
                        final String overrideDatasetId = (String) configData.get(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID);
                        if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                            edgeEventData.put(OptimizeConstants.JsonKeys.DATASET_ID, overrideDatasetId);
                        }
                    }

                    final Event edgeEvent = new Event.Builder(OptimizeConstants.EventNames.EDGE_PERSONALIZATION_REQUEST,
                                                            OptimizeConstants.EventType.EDGE,
                                                            OptimizeConstants.EventSource.REQUEST_CONTENT)
                            .setEventData(edgeEventData)
                            .build();

                    MobileCore.dispatchEvent(edgeEvent, new ExtensionErrorCallback<ExtensionError>() {
                        @Override
                        public void error(final ExtensionError extensionError) {
                            MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                                    String.format("Failed to dispatch update propositions request event to the Edge network due to an error (%s)!", extensionError.getErrorName()));
                        }
                    });

                } catch (final Exception e) {
                    MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                            String.format("Failed to process update propositions request event due to an exception (%s)!", e.getLocalizedMessage()));
                }
            }
        });
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value OptimizeConstants.EventSource#EDGE_PERSONALIZATION_DECISIONS}.
     * <p>
     * This method caches the propositions, returned in the Edge response, in the SDK. It also dispatches a personalization notification event with the
     * received propositions.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleEdgeResponse(final Event event) {
        getExecutor().execute(new Runnable() {
              @Override
              public void run() {
                  if (event == null || OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                      MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge personalization:decisions event, event is null or event data is null/ empty.");
                      return;
                  }
                  final Map<String, Object> eventData = event.getEventData();

                  // Verify the Edge response event handle
                  final String edgeEventHandleType = (String) eventData.get(OptimizeConstants.Edge.EVENT_HANDLE);
                  if (!OptimizeConstants.Edge.EVENT_HANDLE_TYPE_PERSONALIZATION.equals(edgeEventHandleType)) {
                      MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge personalization:decisions event, event handle type is not personalization:decisions.");
                      return;
                  }

                  final List<Map<String, Object>> payload = (List<Map<String, Object>>) eventData.get(OptimizeConstants.Edge.PAYLOAD);
                  final Map<DecisionScope, Proposition> propositionsMap = new HashMap<>();
                  for (final Map<String, Object> propositionData: payload) {
                     final Proposition proposition = Proposition.fromEventData(propositionData);
                     if (proposition != null && !OptimizeUtils.isNullOrEmpty(proposition.getOffers())) {
                         final DecisionScope scope = new DecisionScope(proposition.getScope());
                         propositionsMap.put(scope, proposition);
                     }
                  }

                  if (OptimizeUtils.isNullOrEmpty(propositionsMap)) {
                      MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge personalization:decisions event, no propositions with valid offers are present in the Edge response.");
                      return;
                  }

                  // Update propositions cache
                  cachedPropositions.putAll(propositionsMap);

                  final List<Map<String, Object>> propositionsList = new ArrayList<>();
                  for (final Proposition proposition: propositionsMap.values()) {
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
                  MobileCore.dispatchEvent(edgeEvent, new ExtensionErrorCallback<ExtensionError>() {
                      @Override
                      public void error(final ExtensionError extensionError) {
                          MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                                  String.format("Failed to dispatch optimize notification event due to an error (%s)!", extensionError.getErrorName()));
                      }
                  });
              }
        });
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#EDGE} and source {@value OptimizeConstants.EventSource#ERROR_RESPONSE_CONTENT}.
     * <p>
     * This method logs the error information, returned in Edge response, specifying error type along with a detail message.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleEdgeErrorResponse(final Event event) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (event == null || OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                    MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge error response event, event is null or event data is null/ empty.");
                    return;
                }
                final Map<String, Object> eventData = event.getEventData();

                final String errorType = (String) eventData.get(OptimizeConstants.Edge.ErrorKeys.TYPE);
                final String errorDetail = (String) eventData.get(OptimizeConstants.Edge.ErrorKeys.DETAIL);

                MobileCore.log(LoggingMode.WARNING, OptimizeConstants.LOG_TAG,
                        String.format("Decisioning Service error! Error type: (%s), detail: (%s)", errorType, errorDetail));
            }
        });
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * <p>
     * This method caches the propositions, returned in the Edge response, in the SDK. It also dispatches an optimize response event with the
     * propositions for the requested decision scopes.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleGetPropositions(final Event event) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final ExtensionErrorCallback<ExtensionError> callback = new ExtensionErrorCallback<ExtensionError>() {
                    @Override
                    public void error(final ExtensionError extensionError) {
                        MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                                String.format("Failed to dispatch optimize response event due to an error (%s)!", extensionError.getErrorName()));
                    }
                };

                if (event == null || OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the update propositions request event, event is null or event data is null/ empty.");
                    MobileCore.dispatchResponseEvent(createResponseEventWithError(AdobeError.UNEXPECTED_ERROR), event, callback);
                    return;
                }
                final Map<String, Object> eventData = event.getEventData();

                try {
                    final List<Map<String, Object>> decisionScopesData = (List<Map<String, Object>>) eventData.get(OptimizeConstants.EventDataKeys.DECISION_SCOPES);
                    final List<String> validScopeNames = retrieveValidDecisionScopes(decisionScopesData);
                    if (OptimizeUtils.isNullOrEmpty(validScopeNames)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the get propositions request event, provided list of decision scopes has no valid scope.");
                        MobileCore.dispatchResponseEvent(createResponseEventWithError(AdobeError.UNEXPECTED_ERROR), event, callback);
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
                            .build();

                    MobileCore.dispatchResponseEvent(responseEvent, event, callback);

                } catch (final Exception e) {
                    MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                            String.format("Failed to process get propositions request event due to an exception (%s)!", e.getLocalizedMessage()));
                }
            }
        });
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
    void handleTrackPropositions(final Event event) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (event == null || OptimizeUtils.isNullOrEmpty(event.getEventData())) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the track propositions request event, event is null or event data is null/ empty.");
                    return;
                }
                final Map<String, Object> eventData = event.getEventData();

                final Map<String, Object> configData = retrieveConfigurationSharedState(event);
                if (OptimizeUtils.isNullOrEmpty(configData)) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the track propositions request event, Configuration shared state is not available.");
                    return;
                }

                try {
                    final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>) eventData.get(OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS);
                    if (OptimizeUtils.isNullOrEmpty(propositionInteractionsXdm)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the track propositions request event, provided proposition interactions map is null or empty.");
                        return;
                    }

                    final Map<String, Object> edgeEventData = new HashMap<>();

                    // Add xdm
                    final Map<String, Object> xdm = new HashMap<>();
                    xdm.putAll(propositionInteractionsXdm);
                    edgeEventData.put(OptimizeConstants.JsonKeys.XDM, xdm);

                    // Add override datasetId
                    if (configData.containsKey(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID)) {
                        final String overrideDatasetId = (String) configData.get(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID);
                        if (!OptimizeUtils.isNullOrEmpty(overrideDatasetId)) {
                            edgeEventData.put(OptimizeConstants.JsonKeys.DATASET_ID, overrideDatasetId);
                        }
                    }

                    final Event edgeEvent = new Event.Builder(OptimizeConstants.EventNames.EDGE_PROPOSITION_INTERACTION_REQUEST,
                            OptimizeConstants.EventType.EDGE,
                            OptimizeConstants.EventSource.REQUEST_CONTENT)
                            .setEventData(edgeEventData)
                            .build();

                    MobileCore.dispatchEvent(edgeEvent, new ExtensionErrorCallback<ExtensionError>() {
                        @Override
                        public void error(final ExtensionError extensionError) {
                            MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                                    String.format("Failed to dispatch proposition interactions event to the Edge network due to an error (%s)!", extensionError.getErrorName()));
                        }
                    });

                } catch (final Exception e) {
                    MobileCore.log(LoggingMode.WARNING, LOG_TAG,
                            String.format("Failed to process track propositions request event due to an exception (%s)!", e.getLocalizedMessage()));
                }
            }
        });
    }

    /**
     * Handles the event with type {@value OptimizeConstants.EventType#OPTIMIZE} and source {@value OptimizeConstants.EventSource#REQUEST_RESET}.
     * <p>
     * This method clears previously cached propositions in the SDK.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    void handleClearPropositions(final Event event) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                cachedPropositions.clear();
            }
        });
    }

    /**
     * Retrieves the {@code Configuration} shared state versioned at the current {@code event}.
     *
     * @param event incoming {@link Event} instance.
     * @return {@code Map<String, Object>} containing configuration data.
     */
    Map<String, Object> retrieveConfigurationSharedState(final Event event) {
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, LOG_TAG,
                        String.format("Failed to read Configuration shared state due to an error (%s)!",
                                extensionError.getErrorName()));
            }
        };

        return getApi().getSharedEventState(OptimizeConstants.Configuration.EXTENSION_NAME, event, errorCallback);
    }

    /**
     * Gets the {@code ExecutorService} instance that can execute this extension's tasks on a separate thread.
     * <p>
     * This prevents blocking the {@code EventHub} thread for long running extension tasks such as processing
     * of incoming events.
     *
     * @return {@link ExecutorService} instance for this extension.
     */
    ExecutorService getExecutor() {
        synchronized (executorMutex) {
            if (executorService == null) {
                executorService = Executors.newSingleThreadExecutor();
            }

            return executorService;
        }
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
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "No valid decision scopes are retrieved, provided decision scopes list is null or empty.");
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
            MobileCore.log(LoggingMode.WARNING, LOG_TAG, "No valid decision scopes are retrieved, provided list of decision scopes has no valid scope.");
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
    private Event createResponseEventWithError(final AdobeError error) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.RESPONSE_ERROR, error);

        return new Event.Builder(OptimizeConstants.EventNames.OPTIMIZE_RESPONSE,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(eventData)
                .build();
    }
}