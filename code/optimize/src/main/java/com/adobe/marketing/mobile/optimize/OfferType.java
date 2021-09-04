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

public enum OfferType {
    UNKNOWN, JSON, TEXT, HTML, IMAGE;

    @Override
    public String toString() {
        switch(this) {
            case JSON:
                return "application/json";

            case TEXT:
                return "text/plain";

            case HTML:
                return "text/html";

            case IMAGE:
                return "image/*";

            default:
                return "";
        }
    }

    /**
     * Returns the {@code OfferType} for the given {@code format}.
     *
     * @param format {@link String} containing the {@link Offer} format.
     * @return {@link OfferType} indicating the {@code Offer} format.
     */
    public static OfferType from(final String format) {
        if (format == null || format.isEmpty()) {
            return OfferType.UNKNOWN;
        }

        final String lowerCaseType = format.toLowerCase();
        switch (lowerCaseType) {
            case "application/json":
                return JSON;

            case "text/plain":
                return TEXT;

            case "text/html":
                return HTML;

            default:
                if (lowerCaseType.startsWith("image/")) {
                    return IMAGE;
                }
                return UNKNOWN;
        }
    }
}