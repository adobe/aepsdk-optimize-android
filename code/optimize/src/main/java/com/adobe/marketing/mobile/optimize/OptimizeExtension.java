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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.ArrayList;
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

    private Map<DecisionScope, Proposition> cachedPropositions;

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
                    final List<DecisionScope> decisionScopes = (List<DecisionScope>) eventData.get(OptimizeConstants.EventDataKeys.DECISION_SCOPES);
                    if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot process the update propositions request event, decision scopes list is null or empty.");
                        return;
                    }

                    final List<DecisionScope> validScopes = new ArrayList<>();
                    for (final DecisionScope scope: decisionScopes) {
                        if (!scope.isValid()) {
                            continue;
                        }
                        validScopes.add(scope);
                    }

                    if (validScopes.size() == 0) {
                        MobileCore.log(LoggingMode.WARNING, LOG_TAG, "Cannot process the update propositions request, provided list of decision scopes has no valid scope.");
                        return;
                    }

                    final Map<String, Object> edgeEventData = new HashMap<>();

                    // Add query
                    final Map<String, Object> queryPersonalization = new HashMap<>();
                    queryPersonalization.put(OptimizeConstants.JsonKeys.DECISION_SCOPES, validScopes);
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
                      MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge response event, event is null or event data is null/ empty.");
                      return;
                  }
                  final Map<String, Object> eventData = event.getEventData();

                  // Verify the Edge response event handle
                  final String eventType = (String) eventData.get(OptimizeConstants.Edge.EVENT_HANDLE);
                  if (!OptimizeConstants.Edge.EVENT_HANDLE_TYPE_PERSONALIZATION.equals(eventType)) {
                      MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge response event, event handle type is not personalization:decisions.");
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
                      MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG, "Cannot process the Edge response event, no propositions with valid offers are present in the Edge response.");
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
                        String.format("Decisioning Service error! Error type: %s, detail: %s", errorType, errorDetail));
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
}