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

package com.adobe.marketing.mobile.optimizeapp;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.adobe.marketing.mobile.optimizeapp.OptimizeConstants.LOG_TAG;

/**
 * Public class containing APIs for the Optimize extension.
 */
public class Optimize {
    private Optimize() {}

    /**
     * Returns the version of the {@code Optimize} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    public static String extensionVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile Core.
     * <p>
     * Note: This method should be called only once in your application class.
     */
    public static void registerExtension() {
        MobileCore.registerExtension(OptimizeExtension.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, LOG_TAG,
                        "An error occurred while registering the Optimize extension: " + extensionError.getErrorName());
            }
        });
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions, for the provided decision scopes list, from the decisioning services enabled in the Experience Edge network.
     * <p>
     * The returned decision propositions are cached in-memory in the Optimize SDK extension and can be retrieved using {@link #getPropositions(List, AdobeCallbackWithError)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in the personalization query request.
     */
    public static void updatePropositions(final List<DecisionScope> decisionScopes, final Map<String, Object> xdm, final Map<String, Object> data) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
            MobileCore.log(LoggingMode.WARNING, LOG_TAG, "Cannot update propositions, provided list of decision scopes is null or empty.");
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
            MobileCore.log(LoggingMode.WARNING, LOG_TAG, "Cannot update propositions, provided list of decision scopes has no valid scope.");
            return;
        }

        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                        String.format("Failed to dispatch event (%s) due to error (%s).",
                                OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
                                extensionError.getErrorName()));
            }
        };

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<Map<String, Object>>();
        for (final DecisionScope scope: validScopes) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.REQUEST_TYPE, OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE);
        eventData.put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes);

        if (!OptimizeUtils.isNullOrEmpty(xdm)) {
            eventData.put(OptimizeConstants.EventDataKeys.XDM, xdm);
        }

        if (!OptimizeUtils.isNullOrEmpty(data)) {
            eventData.put(OptimizeConstants.EventDataKeys.DATA, data);
        }

        final Event event = new Event.Builder(OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event, errorCallback);
    }

    /**
     * This API retrieves the previously fetched propositions, for the provided decision scopes, from the in-memory extension propositions cache.
     * <p>
     * The returned decision propositions are cached in-memory in the Optimize SDK extension and can be retrieved using {@link #getPropositions(List, AdobeCallbackWithError)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to be requested.
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, Proposition>>} which will be invoked when decision propositions are retrieved from the local cache.
     */
    public static void getPropositions(final List<DecisionScope> decisionScopes, final AdobeCallbackWithError<Map<DecisionScope, Proposition>> callback) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
            MobileCore.log(LoggingMode.WARNING, LOG_TAG, "Cannot get propositions, provided list of decision scopes is null or empty.");
            if (callback != null) {
                callback.fail(AdobeError.UNEXPECTED_ERROR);
            }
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
            MobileCore.log(LoggingMode.WARNING, LOG_TAG, "Cannot update propositions, provided list of decision scopes has no valid scope.");
            if (callback != null) {
                callback.fail(AdobeError.UNEXPECTED_ERROR);
            }
            return;
        }

        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                        String.format("Failed to dispatch event (%s) due to error (%s).",
                                OptimizeConstants.EventNames.GET_PROPOSITIONS_REQUEST,
                                extensionError.getErrorName()));
            }
        };

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<Map<String, Object>>();
        for (final DecisionScope scope : validScopes) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.REQUEST_TYPE, OptimizeConstants.EventDataValues.REQUEST_TYPE_GET);
        eventData.put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes);

        final Event event = new Event.Builder(OptimizeConstants.EventNames.GET_PROPOSITIONS_REQUEST,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                callback.fail(adobeError);
            }

            @Override
            public void call(final Event event) {
                final Map<String, Object> eventData = event.getEventData();
                if (OptimizeUtils.isNullOrEmpty(eventData)) {
                    callback.fail(AdobeError.UNEXPECTED_ERROR);
                    return;
                }

                if (eventData.containsKey(OptimizeConstants.EventDataKeys.RESPONSE_ERROR)) {
                    final AdobeError error = (AdobeError) eventData.get(OptimizeConstants.EventDataKeys.RESPONSE_ERROR);
                    callback.fail(error);
                    return;
                }

                final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>)eventData.get(OptimizeConstants.EventDataKeys.PROPOSITIONS);

                final Map<DecisionScope, Proposition> propositionsMap = new HashMap<>();
                for (final Map<String, Object> propositionData: propositionsList) {
                    final Proposition proposition = Proposition.fromEventData(propositionData);
                    if (proposition != null && !OptimizeUtils.isNullOrEmpty(proposition.getScope())) {
                        final DecisionScope scope = new DecisionScope(proposition.getScope());
                        propositionsMap.put(scope, proposition);
                    }
                }
                callback.call(propositionsMap);
            }
        }, errorCallback);
    }

    /**
     * This API registers a permanent callback which is invoked whenever the Edge extension dispatches a response Event received from the Experience Edge Network upon a personalization query.
     * <p>
     * The personalization query requests can be triggered by the {@link Optimize#updatePropositions(List, Map, Map)} API, Edge extension {@code sendEvent(ExperienceEvent, EdgeCallback)} API or launch consequence rules.
     *
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, Proposition>>} which will be invoked when decision propositions are received from the Edge network.
     */
    public static void onPropositionsUpdate(final AdobeCallbackWithError<Map<DecisionScope, Proposition>> callback) {
        MobileCore.registerEventListener(OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.NOTIFICATION,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(final AdobeError adobeError) {}

                    @Override
                    public void call(final Event event) {
                        final Map<String, Object> eventData = event.getEventData();
                        if (OptimizeUtils.isNullOrEmpty(eventData)) {
                            return;
                        }

                        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>)eventData.get(OptimizeConstants.EventDataKeys.PROPOSITIONS);

                        final Map<DecisionScope, Proposition> propositionsMap = new HashMap<>();
                        for (final Map<String, Object> propositionData : propositionsList) {
                            final Proposition proposition = Proposition.fromEventData(propositionData);
                            if (proposition != null && !OptimizeUtils.isNullOrEmpty(proposition.getScope())) {
                                final DecisionScope scope = new DecisionScope(proposition.getScope());
                                propositionsMap.put(scope, proposition);
                            }
                        }

                        if (!propositionsMap.isEmpty()) {
                            callback.call(propositionsMap);
                        }
                    }
                }
        );
    }

    /**
     * Clears the client-side in-memory propositions cache.
     */
    public static void clearCachedPropositions() {
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                        String.format("Failed to dispatch event (%s) due to error (%s).",
                                OptimizeConstants.EventNames.CLEAR_PROPOSITIONS_REQUEST,
                                extensionError.getErrorName()));
            }
        };

        final Event event = new Event.Builder(OptimizeConstants.EventNames.CLEAR_PROPOSITIONS_REQUEST,
                                            OptimizeConstants.EventType.OPTIMIZE,
                                            OptimizeConstants.EventSource.REQUEST_RESET).build();
        MobileCore.dispatchEvent(event, errorCallback);
    }
}