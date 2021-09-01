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

import java.util.Collection;
import java.util.Map;

class CollectionUtils {
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
}
