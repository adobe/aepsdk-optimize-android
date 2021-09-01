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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.adobe.marketing.mobile.optimize.OptimizeConstants.LOG_TAG;

public class Proposition {
    final private String id;
    final private List<Offer> offers;
    final private String scope;
    final private Map<String, Object> scopeDetails;

    /**
     * Constructor creates a {@code Proposition} using the provided propostion {@code id}, {@code offers}, {@code scope} and {@code scopeDetails}.
     *
     * @param id {@link String} containing proposition identifier.
     * @param offers {@link String} containing proposition items.
     * @param scope {@code String} containing encoded scope.
     * @param scopeDetails {@code Map<String, Object>} containing scope details.
     */
    Proposition(final String id, final List<Offer> offers, final String scope, final Map<String, Object> scopeDetails) {
        this.id = id != null ? id : "";
        this.offers = offers != null ? offers : new ArrayList<Offer>();
        this.scope = scope != null ? scope : "";
        this.scopeDetails = scopeDetails != null ? scopeDetails : new HashMap<String, Object>();
    }

    /**
     * Gets the {@code Proposition} identifier.
     *
     * @return {@link String} containing the {@link Proposition} identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the {@code Proposition} items.
     *
     * @return {@code List<Offer>} containing the {@link Proposition} items.
     */
    public List<Offer> getOffers() {
        return offers;
    }

    /**
     * Gets the {@code Proposition} scope.
     *
     * @return {@link String} containing the encoded {@link Proposition} scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the {@code Proposition} scope details.
     *
     * @return {@code Map<String, Object>} containing the {@link Proposition} scope details.
     */
    public Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }

    /**
     * Creates a {@code Proposition} object using information provided in {@code data} Map.
     * <p>
     * This method returns null if the provided {@code data} is empty or null or if it does not contain required info for creating a {@link Proposition} object.
     *
     * @param data {@code Map<String, Object>} containing proposition data.
     * @return {@code Proposition} object or null.
     */
    static Proposition fromEventData(final Map<String, Object> data) {
        if(CollectionUtils.isNullOrEmpty(data)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot create Proposition object, provided data Map is empty or null.");
            return null;
        }

        try {
            final String id = (String) data.get(OptimizeConstants.JsonKeys.PAYLOAD_ID);
            if(StringUtils.isNullOrEmpty(id)) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot create Proposition object, provided data does not contain proposition identifier.");
                return null;
            }

            final String scope = (String) data.get(OptimizeConstants.JsonKeys.PAYLOAD_SCOPE);
            if(StringUtils.isNullOrEmpty(scope)) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot create Proposition object, provided data does not contain proposition scope.");
                return null;
            }

            final Map<String, Object> scopeDetails = (Map<String, Object>) data.get(OptimizeConstants.JsonKeys.PAYLOAD_SCOPEDETAILS);

            final List<Map<String, Object>> items = (List<Map<String, Object>>) data.get(OptimizeConstants.JsonKeys.PAYLOAD_ITEMS);
            List<Offer> offers = new ArrayList<Offer>();
            for (Object item : items) {
                final Offer offer = Offer.fromEventData((Map<String, Object>)item);
                if(offer != null) {
                    offers.add(offer);
                }
            }
            return new Proposition(id, offers, scope, scopeDetails);

        } catch (Exception e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot create Proposition object, provided data contains invalid fields.");
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proposition that = (Proposition) o;
        return id.equals(that.id) &&
                offers.equals(that.offers) &&
                scope.equals(that.scope) &&
                scopeDetails.equals(that.scopeDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, offers, scope, scopeDetails);
    }
}

