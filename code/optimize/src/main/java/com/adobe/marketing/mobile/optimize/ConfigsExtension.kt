/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.services.Log

object ConfigsExtension {
    private const val SELF_TAG: String = "ConfigsExtension"

    /**
     * Retrieves the `Configuration` shared state versioned at the current `event`.
     *
     * @param event The incoming [Event] instance (optional).
     * @return A `Map<String, Any>` containing configuration data, or an empty map if unavailable.
     */

    private fun ExtensionApi.getConfigsSharedState(
        event: Event? = null
    ): Map<String, Any> = try {
        getSharedState(
            OptimizeConstants.Configuration.EXTENSION_NAME,
            event,
            false,
            SharedStateResolution.ANY
        )?.value ?: emptyMap()
    } catch (e: Exception) {
        Log.error(
            OptimizeConstants.LOG_TAG,
            SELF_TAG,
            "Error retrieving shared state: ${e.message}"
        )
        emptyMap()
    }

    /**
     * Retrieves the timeout configuration from the shared state.
     *
     * @param event The incoming [Event] instance (optional).
     * @return The timeout value as a [Double], or `null` if unavailable or an error occurs.
     */
    @JvmStatic
    @JvmOverloads
    fun <T> ExtensionApi.getConfigValue(
        event: Event? = null,
        key: String,
        defaultValue: T,
        extractor: (Map<String, Any>, String) -> T?
    ): T {
        val sharedState = getConfigsSharedState(event)
        if (sharedState.isEmpty()) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Cannot process the request, Configuration shared state is not available."
            )
            return defaultValue
        }

        return try {
            extractor(sharedState, key) ?: defaultValue
        } catch (e: Exception) {
            Log.error(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Failed to retrieve config value for key '$key' from configuration shared state: ${e.message}"
            )
            defaultValue
        }
    }
}
