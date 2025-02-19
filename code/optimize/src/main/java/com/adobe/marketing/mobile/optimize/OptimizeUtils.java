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

import android.util.Base64;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.Collection;
import java.util.Map;

class OptimizeUtils {

    private static final String SELF_TAG = "OptimizeUtils";

    /**
     * Checks if the given {@code collection} is null or empty.
     *
     * @param collection input {@code Collection<?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code collection} is null or
     *     empty.
     */
    static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if the given {@code map} is null or empty.
     *
     * @param map input {@code Map<?, ?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code map} is null or empty.
     */
    static boolean isNullOrEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if the given {@code String} is null or empty.
     *
     * @param str input {@link String} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code str} is null or empty.
     */
    static boolean isNullOrEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Base64 encode the given {@code String}.
     *
     * <p>This method returns the provided {@code str} value if it is null or empty.
     *
     * @param str input {@link String} to be encoded.
     * @return {@code String} containing the Base64 encoded value.
     */
    static String base64Encode(final String str) {
        if (isNullOrEmpty(str)) {
            return str;
        }
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Base64 decode the given {@code String}.
     *
     * <p>This method returns the provided {@code str} value if it is null or empty. It returns null
     * if the base64 decode fails.
     *
     * @param str input {@link String} to be decoded.
     * @return {@code String} containing the Base64 decoded value.
     */
    static String base64Decode(final String str) {
        if (isNullOrEmpty(str)) {
            return str;
        }

        String output = null;
        try {
            output = new String(Base64.decode(str, Base64.DEFAULT));
        } catch (final IllegalArgumentException ex) {
            Log.trace(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    String.format(
                            "Base64 decode failed for the given string (%s) with exception: %s",
                            str, ex.getLocalizedMessage()));
        }
        return output;
    }

    /**
     * Determines the {@code AdobeError} provided the error code.
     *
     * @return {@link AdobeError} corresponding to the given error code, or {@link
     *     AdobeError#UNEXPECTED_ERROR} otherwise.
     */
    @SuppressWarnings("magicnumber")
    static AdobeError convertToAdobeError(final int errorCode) {
        final AdobeError error;
        switch (errorCode) {
            case 0:
                error = AdobeError.UNEXPECTED_ERROR;
                break;
            case 1:
                error = AdobeError.CALLBACK_TIMEOUT;
                break;
            case 2:
                error = AdobeError.CALLBACK_NULL;
                break;
            case 11:
                error = AdobeError.EXTENSION_NOT_INITIALIZED;
                break;
            default:
                error = AdobeError.UNEXPECTED_ERROR;
        }
        return error;
    }

    /**
     * Checks whether the given event is a personalization: decisions response returned from the
     * Edge network.
     *
     * @param event instance of {@link Event}
     * @return {@code boolean} containing true if event is a personalization: decisions event, false
     *     otherwise.
     */
    static boolean isPersonalizationDecisionsResponse(final Event event) {
        return OptimizeConstants.EventType.EDGE.equalsIgnoreCase(event.getType())
                && OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS.equalsIgnoreCase(
                        event.getSource());
    }

    /**
     * Checks whether the given event is a edge error response content response returned from the
     * Edge network.
     *
     * @param event instance of {@link Event}
     * @return {@code boolean} containing true if event is a edge error response content event,
     *     false otherwise.
     */
    static boolean isEdgeErrorResponseContent(final Event event) {
        return OptimizeConstants.EventType.EDGE.equalsIgnoreCase(event.getType())
                && OptimizeConstants.EventSource.ERROR_RESPONSE_CONTENT.equalsIgnoreCase(
                        event.getSource());
    }

    /**
     * Checks whether the given event is a Personaliztion Debug Event returned from the Edge
     * network.
     *
     * @param event instance of {@link Event}
     * @return {@code boolean} return true if event is a debug event, false otherwise.
     */
    static boolean isPersonalizationDebugEvent(final Event event) {
        try {
            final Map<String, String> debugData =
                    DataReader.getStringMap(event.getEventData(), OptimizeConstants.Edge.DEBUG);
            return OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS.equalsIgnoreCase(
                            debugData.get(OptimizeConstants.Edge.EVENT_SOURCE))
                    && EventType.EDGE.equalsIgnoreCase(
                            debugData.get(OptimizeConstants.Edge.EVENT_TYPE));
        } catch (Exception e) {
            Log.trace(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    String.format(
                            "Exception occurred while checking if event is a personalization debug"
                                    + " event: %s",
                            e.getLocalizedMessage()));
            return false;
        }
    }

    /**
     * Checks whether the given event is an Optimize request content event for retrieving cached
     * propositions.
     *
     * <p>The get request should have {@code requesttype} set to {@literal getpropositions} in the
     * event's data.
     *
     * @param event instance of {@link Event}
     * @return true if event is a Personalization Decision Response event, false otherwise
     */
    static boolean isGetEvent(final Event event) {
        final String requestType =
                DataReader.optString(
                        event.getEventData(), OptimizeConstants.EventDataKeys.REQUEST_TYPE, "");
        return OptimizeConstants.EventType.OPTIMIZE.equalsIgnoreCase(event.getType())
                && OptimizeConstants.EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && requestType.equalsIgnoreCase(OptimizeConstants.EventDataValues.REQUEST_TYPE_GET);
    }

    /**
     * Returns event's parentID or {@code requestEventId} present in the event's data.
     *
     * @param event instance of {@link Event}
     * @return {@link String} containing request event ID.
     */
    static String getRequestEventId(final Event event) {
        String requestEventId = event.getParentID();
        if (OptimizeUtils.isNullOrEmpty(requestEventId)) {
            requestEventId =
                    DataReader.optString(
                            event.getEventData(),
                            OptimizeConstants.EventDataKeys.REQUEST_EVENT_ID,
                            null);
        }
        return requestEventId;
    }
}
