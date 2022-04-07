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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.adobe.marketing.mobile.optimize.OptimizeConstants.LOG_TAG;

/**
 * {@code DecisionScope} class represents a scope used to fetch personalized offers from the Experience Edge network.
 */
public class DecisionScope {
    private static final String SCOPE_JSON = "{\"activityId\":\"%s\",\"placementId\":\"%s\"}";
    private static final String SCOPE_WITH_ITEMCOUNT_JSON = "{\"activityId\":\"%s\",\"placementId\":\"%s\",\"itemCount\":%s}";
    private static final int DEFAULT_ITEM_COUNT = 1;

    final private String name;

    /**
     * Constructor creates a {@code DecisionScope} using the provided {@code name}.
     *
     * @param name {@link String} containing scope name.
     */
    public DecisionScope(final String name) {
        this.name = name != null ? name : "";
    }

    /**
     * Constructor creates a {@code DecisionScope} using the provided {@code activityId} and {@code placementId}.
     * <p>
     * This constructor assumes the item count for the given scope to be {@value #DEFAULT_ITEM_COUNT}.
     *
     * @param activityId {@link String} containing activity identifier for the given scope.
     * @param placementId {@code String} containing placement identifier for the given scope.
     */
    public DecisionScope(final String activityId, final String placementId) {
        this(activityId, placementId, DEFAULT_ITEM_COUNT);
    }

    /**
     * Constructor creates a {@code DecisionScope} using the provided {@code activityId}, {@code placementId} and {@code itemCount}.
     *
     * @param activityId {@link String} containing activity identifier for the given scope.
     * @param placementId {@code String} containing placement identifier for the given scope.
     * @param itemCount {@code String} containing number of items to be returned for the given scope.
     */
    public DecisionScope(final String activityId, final String placementId, final int itemCount) {
        final String encodedScope = generateEncodedScope(activityId, placementId, itemCount);
        this.name = encodedScope != null ? encodedScope: "";
    }

    /**
     * Gets the name for this scope.
     *
     * @return {@link String} containing the scope name.
     */
    public String getName() {
        return name;
    }

    /**
     * Determines whether this scope is valid.
     *
     * @return {@code boolean} indicating whether the scope is valid.
     */
    boolean isValid() {
        if (OptimizeUtils.isNullOrEmpty(name)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Invalid scope! Scope name is null or empty.");
            return false;
        }

        final String jsonString = OptimizeUtils.base64Decode(name);
        if (jsonString != null) {
            try {
                final JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.has(OptimizeConstants.XDM_NAME)) {
                    final String scopeName = jsonObject.getString(OptimizeConstants.XDM_NAME);
                    if (OptimizeUtils.isNullOrEmpty(scopeName)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Scope name is null or empty.", name));
                        return false;
                    }
                } else if (jsonObject.has(OptimizeConstants.XDM_ACTIVITY_ID)) {
                    final String activityId = jsonObject.getString(OptimizeConstants.XDM_ACTIVITY_ID);
                    if (OptimizeUtils.isNullOrEmpty(activityId)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Activity Id is null or empty.", name));
                        return false;
                    }

                    final String placementId = jsonObject.getString(OptimizeConstants.XDM_PLACEMENT_ID);
                    if (OptimizeUtils.isNullOrEmpty(placementId)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Placement Id is null or empty.", name));
                        return false;
                    }

                    final int itemCount = jsonObject.optInt(OptimizeConstants.XDM_ITEM_COUNT, DEFAULT_ITEM_COUNT);
                    if (itemCount < DEFAULT_ITEM_COUNT) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Item count (%d) is invalid.", name, itemCount));
                        return false;
                    }
                } else {
                    final String activityId = jsonObject.getString(OptimizeConstants.ACTIVITY_ID);
                    if (OptimizeUtils.isNullOrEmpty(activityId)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Activity Id is null or empty.", name));
                        return false;
                    }

                    final String placementId = jsonObject.getString(OptimizeConstants.PLACEMENT_ID);
                    if (OptimizeUtils.isNullOrEmpty(placementId)) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Placement Id is null or empty.", name));
                        return false;
                    }

                    final int itemCount = jsonObject.optInt(OptimizeConstants.ITEM_COUNT, DEFAULT_ITEM_COUNT);
                    if (itemCount < DEFAULT_ITEM_COUNT) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Invalid scope (%s)! Item count (%d) is invalid.", name, itemCount));
                        return false;
                    }
                }
            } catch (JSONException e) {
                MobileCore.log(LoggingMode.WARNING, LOG_TAG, String.format("Scope name (%s), when decoded, does not contain a JSON string.", name));
            }
        }

        MobileCore.log(LoggingMode.VERBOSE, LOG_TAG, String.format("Decision scope (%s) is valid.", name));
        return true;
    }

    /**
     * Generates the scope name using the given {@code activityId}, {@code placementId} and {@code itemCount}.
     * <p>
     * This method creates the scope name by Base64 encoding the JSON string created using the provided data.
     * If {@code itemCount} > 1, then JSON string is
     *  {@literal {"activityId":#activityId,"placementId":#placementId,"itemCount":#itemCount}}
     * otherwise it is,
     *  {@literal {"activityId":#activityId,"placementId":#placementId}}
     *
     * @param activityId {@link String} containing activity identifier for the given scope.
     * @param placementId {@code String} containing placement identifier for the given scope.
     * @param itemCount {@code int} containing number of items to be returned for the given scope.
     *
     * @return {@code String} containing the Base64 encoded scope name.
     */
    static String generateEncodedScope(final String activityId, final String placementId, final int itemCount) {
        if (OptimizeUtils.isNullOrEmpty(activityId) || OptimizeUtils.isNullOrEmpty(placementId) || itemCount <= 0) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot generate the Base64 encoded decision scope as the provided activityId or placementId or itemCount is invalid.");
            return null;
        }

        final String json;
        if (itemCount > DEFAULT_ITEM_COUNT) {
            json = String.format(SCOPE_WITH_ITEMCOUNT_JSON, activityId, placementId, itemCount);
        } else {
            json = String.format(SCOPE_JSON, activityId, placementId);
        }
        return OptimizeUtils.base64Encode(json);
    }

    /**
     * Creates a {@code DecisionScope} object using information provided in {@code data} map.
     * <p>
     * This method returns null if the provided {@code data} is empty or null or if it does not contain required info for creating a {@link DecisionScope} object.
     *
     * @param data {@code Map<String, Object>} containing this {@code DecisionScope} object's attributes.
     * @return {@code DecisionScope} object or null.
     */
    static DecisionScope fromEventData(final Map<String, Object> data) {
        if (OptimizeUtils.isNullOrEmpty(data)
                || !data.containsKey(OptimizeConstants.EventDataKeys.DECISION_SCOPE_NAME)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot create DecisionScope object, provided data Map is empty or null.");
            return null;
        }

        final String name = (String) data.get(OptimizeConstants.EventDataKeys.DECISION_SCOPE_NAME);
        if (OptimizeUtils.isNullOrEmpty(name)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot create DecisionScope object, provided data does not contain valid scope name.");
            return null;
        }

        return new DecisionScope(name);
    }

    /**
     * Returns a {@code Map<String, Object>} containing this {@code DecisionScope} object's attributes.
     *
     * @return {@code Map<String, Object>} containing {@link DecisionScope} data.
     */
    Map<String, Object> toEventData() {
        Map<String, Object> map = new HashMap<>();
        map.put(OptimizeConstants.EventDataKeys.DECISION_SCOPE_NAME, this.name);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecisionScope that = (DecisionScope) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
