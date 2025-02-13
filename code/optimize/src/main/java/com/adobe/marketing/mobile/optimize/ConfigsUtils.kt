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
import com.adobe.marketing.mobile.util.DataReader
import com.adobe.marketing.mobile.util.DataReaderException

object ConfigsUtils {
    /**
     * Retrieves the `Configuration` shared state versioned at the current `event`.
     *
     * @param event incoming [Event] instance.
     * @return `Map<String, Object>` containing configuration data.
     */
    @JvmStatic
    @JvmOverloads
    fun ExtensionApi.retrieveConfigurationSharedState(
        event: Event? = null
    ): Map<String, Any>? = getSharedState(
        OptimizeConstants.Configuration.EXTENSION_NAME,
        event,
        false,
        SharedStateResolution.ANY
    )?.value

    @JvmStatic
    fun Event.retrieveOptimizeRequestTimeout(configData: Map<String, Any?>): Long {
        val defaultTimeout = OptimizeConstants.DEFAULT_CONFIGURABLE_TIMEOUT_CONFIG.toLong()

        return try {
            val timeout = eventData?.takeIf {
                it.containsKey(OptimizeConstants.EventDataKeys.TIMEOUT)
            }?.let {
                DataReader.getLong(it, OptimizeConstants.EventDataKeys.TIMEOUT)
            } ?: return defaultTimeout

            if (timeout == Long.MAX_VALUE) DataReader.getLong(
                configData,
                OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT
            ) else timeout
        } catch (e: DataReaderException) {
            defaultTimeout
        }
    }
}
