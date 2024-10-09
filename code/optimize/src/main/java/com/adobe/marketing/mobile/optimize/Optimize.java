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
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Public class containing APIs for the Optimize extension. */
public class Optimize {
    public static final Class<? extends Extension> EXTENSION = OptimizeExtension.class;
    private static final String SELF_TAG = "Optimize";

    private Optimize() {}

    /**
     * Returns the version of the {@code Optimize} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @NonNull public static String extensionVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions,
     * for the provided decision scopes list, from the decisioning services enabled in the
     * Experience Edge network.
     *
     * <p>The returned decision propositions are cached in-memory in the Optimize SDK extension and
     * can be retrieved using {@link #getPropositions(List, AdobeCallback)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in
     *     the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in
     *     the personalization query request.
     */
    public static void updatePropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @Nullable final Map<String, Object> xdm,
            @Nullable final Map<String, Object> data) {

        updatePropositions(decisionScopes, xdm, data, null);
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions,
     * for the provided decision scopes list, from the decisioning services enabled in the
     * Experience Edge network.
     *
     * <p>The returned decision propositions are cached in-memory in the Optimize SDK extension and
     * can be retrieved using {@link #getPropositions(List, AdobeCallback)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in
     *     the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in
     *     the personalization query request.
     * @param callback {@code AdobeCallback<Map<DecisionScope, OptimizeProposition>>} which will be
     *     invoked when decision propositions are received from the Edge network.
     */
    public static void updatePropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @Nullable final Map<String, Object> xdm,
            @Nullable final Map<String, Object> data,
            @Nullable final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {

        if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot update propositions, provided list of decision scopes is null or"
                            + " empty.");

            AEPOptimizeError aepOptimizeError = AEPOptimizeError.Companion.getUnexpectedError();
            failWithOptimizeError(callback, aepOptimizeError);

            return;
        }

        final List<DecisionScope> validScopes = new ArrayList<>();
        for (final DecisionScope scope : decisionScopes) {
            if (!scope.isValid()) {
                continue;
            }
            validScopes.add(scope);
        }

        if (validScopes.size() == 0) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot update propositions, provided list of decision scopes has no valid"
                            + " scope.");
            return;
        }

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<>();
        for (final DecisionScope scope : validScopes) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                OptimizeConstants.EventDataKeys.REQUEST_TYPE,
                OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE);
        eventData.put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes);

        if (!OptimizeUtils.isNullOrEmpty(xdm)) {
            eventData.put(OptimizeConstants.EventDataKeys.XDM, xdm);
        }

        if (!OptimizeUtils.isNullOrEmpty(data)) {
            eventData.put(OptimizeConstants.EventDataKeys.DATA, data);
        }

        final Event event =
                new Event.Builder(
                                OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
                                OptimizeConstants.EventType.OPTIMIZE,
                                OptimizeConstants.EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        MobileCore.dispatchEventWithResponseCallback(
                event,
                OptimizeConstants.EDGE_CONTENT_COMPLETE_RESPONSE_TIMEOUT,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        AEPOptimizeError aepOptimizeError;
                        if (adobeError == AdobeError.CALLBACK_TIMEOUT) {
                            aepOptimizeError = AEPOptimizeError.Companion.getTimeoutError();
                        } else {
                            aepOptimizeError = AEPOptimizeError.Companion.getUnexpectedError();
                        }
                        failWithOptimizeError(callback, aepOptimizeError);
                    }

                    @Override
                    public void call(final Event event) {
                        try {
                            final Map<String, Object> eventData = event.getEventData();
                            if (OptimizeUtils.isNullOrEmpty(eventData)) {

                                AEPOptimizeError aepOptimizeError =
                                        AEPOptimizeError.Companion.getUnexpectedError();
                                failWithOptimizeError(callback, aepOptimizeError);
                                return;
                            }

                            if (eventData.containsKey(
                                    OptimizeConstants.EventDataKeys.RESPONSE_ERROR)) {
                                Object error =
                                        eventData.get(
                                                OptimizeConstants.EventDataKeys.RESPONSE_ERROR);
                                if (error instanceof Map) {
                                    failWithOptimizeError(
                                            callback,
                                            AEPOptimizeError.toAEPOptimizeError(
                                                    (Map<String, ? extends Object>) error));
                                }
                            }

                            if (!eventData.containsKey(
                                    OptimizeConstants.EventDataKeys.PROPOSITIONS)) {
                                return;
                            }

                            final List<Map<String, Object>> propositionsList;
                            propositionsList =
                                    DataReader.getTypedListOfMap(
                                            Object.class,
                                            eventData,
                                            OptimizeConstants.EventDataKeys.PROPOSITIONS);
                            final Map<DecisionScope, OptimizeProposition> propositionsMap =
                                    new HashMap<>();
                            if (propositionsList != null) {
                                for (final Map<String, Object> propositionData : propositionsList) {
                                    final OptimizeProposition optimizeProposition =
                                            OptimizeProposition.fromEventData(propositionData);
                                    if (optimizeProposition != null
                                            && !OptimizeUtils.isNullOrEmpty(
                                                    optimizeProposition.getScope())) {
                                        final DecisionScope scope =
                                                new DecisionScope(optimizeProposition.getScope());
                                        propositionsMap.put(scope, optimizeProposition);
                                    }
                                }
                            }

                            if (callback != null) {
                                callback.call(propositionsMap);
                            }
                        } catch (DataReaderException e) {
                            failWithOptimizeError(
                                    callback, AEPOptimizeError.Companion.getUnexpectedError());
                        }
                    }
                });
    }

    /**
     * This API retrieves the previously fetched propositions, for the provided decision scopes,
     * from the in-memory extension propositions cache.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be requested.
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>} which
     *     will be invoked when decision propositions are retrieved from the local cache.
     */
    public static void getPropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @NonNull final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot get propositions, provided list of decision scopes is null or empty.");
            failWithError(callback, AdobeError.UNEXPECTED_ERROR);
            return;
        }

        final List<DecisionScope> validScopes = new ArrayList<>();
        for (final DecisionScope scope : decisionScopes) {
            if (!scope.isValid()) {
                continue;
            }
            validScopes.add(scope);
        }

        if (validScopes.size() == 0) {
            Log.warning(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot update propositions, provided list of decision scopes has no valid"
                            + " scope.");
            failWithError(callback, AdobeError.UNEXPECTED_ERROR);
            return;
        }

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<>();
        for (final DecisionScope scope : validScopes) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                OptimizeConstants.EventDataKeys.REQUEST_TYPE,
                OptimizeConstants.EventDataValues.REQUEST_TYPE_GET);
        eventData.put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes);

        final Event event =
                new Event.Builder(
                                OptimizeConstants.EventNames.GET_PROPOSITIONS_REQUEST,
                                OptimizeConstants.EventType.OPTIMIZE,
                                OptimizeConstants.EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // Increased default response callback timeout to 10s to ensure prior update propositions
        // requests have enough time to complete.
        MobileCore.dispatchEventWithResponseCallback(
                event,
                OptimizeConstants.GET_RESPONSE_CALLBACK_TIMEOUT,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        failWithError(callback, adobeError);
                    }

                    @Override
                    public void call(final Event event) {
                        try {
                            final Map<String, Object> eventData = event.getEventData();
                            if (OptimizeUtils.isNullOrEmpty(eventData)) {
                                failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                                return;
                            }

                            if (eventData.containsKey(
                                    OptimizeConstants.EventDataKeys.RESPONSE_ERROR)) {
                                final int errorCode =
                                        DataReader.getInt(
                                                eventData,
                                                OptimizeConstants.EventDataKeys.RESPONSE_ERROR);
                                failWithError(
                                        callback, OptimizeUtils.convertToAdobeError(errorCode));
                                return;
                            }

                            final List<Map<String, Object>> propositionsList;
                            propositionsList =
                                    DataReader.getTypedListOfMap(
                                            Object.class,
                                            eventData,
                                            OptimizeConstants.EventDataKeys.PROPOSITIONS);
                            final Map<DecisionScope, OptimizeProposition> propositionsMap =
                                    new HashMap<>();
                            if (propositionsList != null) {
                                for (final Map<String, Object> propositionData : propositionsList) {
                                    final OptimizeProposition optimizeProposition =
                                            OptimizeProposition.fromEventData(propositionData);
                                    if (optimizeProposition != null
                                            && !OptimizeUtils.isNullOrEmpty(
                                                    optimizeProposition.getScope())) {
                                        final DecisionScope scope =
                                                new DecisionScope(optimizeProposition.getScope());
                                        propositionsMap.put(scope, optimizeProposition);
                                    }
                                }
                            }
                            callback.call(propositionsMap);
                        } catch (DataReaderException e) {
                            failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                        }
                    }
                });
    }

    /**
     * This API registers a permanent callback which is invoked whenever the Edge extension
     * dispatches a response Event received from the Experience Edge Network upon a personalization
     * query.
     *
     * <p>The personalization query requests can be triggered by the {@link
     * Optimize#updatePropositions(List, Map, Map)} API, Edge extension {@code
     * sendEvent(ExperienceEvent, EdgeCallback)} API or launch consequence rules.
     *
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>} which
     *     will be invoked when decision propositions are received from the Edge network.
     */
    public static void onPropositionsUpdate(
            @NonNull final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        MobileCore.registerEventListener(
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.NOTIFICATION,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(final AdobeError error) {}

                    @Override
                    public void call(final Event event) {
                        final Map<String, Object> eventData = event.getEventData();
                        if (OptimizeUtils.isNullOrEmpty(eventData)) {
                            return;
                        }

                        final List<Map<String, Object>> propositionsList;
                        try {
                            propositionsList =
                                    DataReader.getTypedListOfMap(
                                            Object.class,
                                            eventData,
                                            OptimizeConstants.EventDataKeys.PROPOSITIONS);

                            final Map<DecisionScope, OptimizeProposition> propositionsMap =
                                    new HashMap<>();
                            if (propositionsList != null) {
                                for (final Map<String, Object> propositionData : propositionsList) {
                                    final OptimizeProposition optimizeProposition =
                                            OptimizeProposition.fromEventData(propositionData);
                                    if (optimizeProposition != null
                                            && !OptimizeUtils.isNullOrEmpty(
                                                    optimizeProposition.getScope())) {
                                        final DecisionScope scope =
                                                new DecisionScope(optimizeProposition.getScope());
                                        propositionsMap.put(scope, optimizeProposition);
                                    }
                                }
                            }

                            if (!propositionsMap.isEmpty()) {
                                callback.call(propositionsMap);
                            }
                        } catch (DataReaderException ignored) {
                        }
                    }
                });
    }

    /** Clears the client-side in-memory propositions cache. */
    public static void clearCachedPropositions() {
        final Event event =
                new Event.Builder(
                                OptimizeConstants.EventNames.CLEAR_PROPOSITIONS_REQUEST,
                                OptimizeConstants.EventType.OPTIMIZE,
                                OptimizeConstants.EventSource.REQUEST_RESET)
                        .build();
        MobileCore.dispatchEvent(event);
    }

    /**
     * Invokes fail method with the provided {@code error}, if the callback is an instance of {@code
     * AdobeCallbackWithError}.
     *
     * @param callback can be an instance of {@link AdobeCallback} or {@link
     *     AdobeCallbackWithError}.
     * @param error {@link AdobeError} indicating the error name and code.
     */
    private static void failWithError(final AdobeCallback<?> callback, final AdobeError error) {

        final AdobeCallbackWithError<?> callbackWithError =
                callback instanceof AdobeCallbackWithError
                        ? (AdobeCallbackWithError<?>) callback
                        : null;

        if (callbackWithError != null) {
            callbackWithError.fail(error);
        }
    }

    protected static void failWithOptimizeError(
            final AdobeCallback<?> callback, final AEPOptimizeError error) {

        final AdobeCallbackWithOptimizeError<?> callbackWithError =
                callback instanceof AdobeCallbackWithOptimizeError
                        ? (AdobeCallbackWithOptimizeError<?>) callback
                        : null;

        if (callbackWithError != null) {
            callbackWithError.fail(error);
        }
    }

    /**
     * Dispatches an event for the Edge network extension to send an Experience Event to the Edge
     * network with the display interaction data for the given {@code List<OptimizeProposition>}
     * offer.
     *
     * @param propositions {@code List<OptimizeProposition>} containing the list of propositions
     * @see OptimizeUtils#trackWithData(Map)
     */
    public static void trackDisplayedPropositions(final List<OptimizeProposition> propositions) {
        Map<String, Object> xdm =
                OptimizeUtils.generateInteractionXdm(
                        OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY,
                        propositions,
                        false);
        OptimizeUtils.trackWithData(xdm);
    }
}
