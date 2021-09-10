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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.Collection;
import java.util.Map;

import static com.adobe.marketing.mobile.optimize.OptimizeConstants.LOG_TAG;

class OptimizeUtils {
    /**
     * Checks if the given {@code collection} is null or empty.
     *
     * @param collection input {@code Collection<?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code collection} is null or empty.
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
     * <p>
     * This method returns the provided {@code str} value if it is null or empty.
     *
     * @param str input {@link String} to be encoded.
     * @return {@code String} containing the Base64 encoded value.
     */
    static String base64Encode(final String str) {
        if (isNullOrEmpty(str)) {
            return str;
        }
        return Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
    }

    /**
     * Base64 decode the given {@code String}.
     * <p>
     * This method returns the provided {@code str} value if it is null or empty. It returns null if the base64 decode fails.
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
            output = new String(Base64.decode(str.getBytes(), Base64.DEFAULT));
        } catch(final IllegalArgumentException ex) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Base64 decode failed for the given string (%s) with exception: %s", str, ex.getLocalizedMessage()));
        }
        return output;
    }
}
