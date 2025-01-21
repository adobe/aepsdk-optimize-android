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

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log

object ConfigsManager {

    private const val SELF_TAG: String = "ConfigsManager"
    private var isFetchingConfigurations: Boolean = false

    private fun fetchConfigurations(onComplete: (Map<String, Any>) -> Unit) {
        synchronized(this) {
            if (isFetchingConfigurations) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Configuration fetch in progress. Adding callback to pending list."
                )
                return
            }
            isFetchingConfigurations = true
        }

        val event = Event.Builder(
            "Get Configurations Request",
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONFIGURATION,
            null
        ).build()

        MobileCore.dispatchEventWithResponseCallback(
            event,
            1000L,
            object : AdobeCallbackWithError<Event> {
                override fun call(responseEvent: Event?) {
                    val configurations = responseEvent?.eventData ?: emptyMap()
                    synchronized(this@ConfigsManager) {
                        isFetchingConfigurations = false
                        Log.debug(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "Configurations fetched: $configurations"
                        )
                        onComplete(configurations)
                    }
                }

                override fun fail(error: AdobeError?) {
                    synchronized(this@ConfigsManager) {
                        isFetchingConfigurations = false
                        Log.error(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "Failed to fetch configurations: ${error?.errorName}. Returning empty configurations."
                        )
                        onComplete(emptyMap())
                    }
                }
            }
        )
    }

    fun getConfigurableTimeoutConfig(
        onSuccess: (Double) -> Unit
    ) = fetchConfigurations { configurations ->
        val timeout = try {
            configurations[OptimizeConstants.EventDataKeys.TIMEOUT] as Double
        } catch (e: Exception) {
            Log.error(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Error retrieving timeout configuration: ${e.message}. Using default value: ${OptimizeConstants.DEFAULT_CONFIGURABLE_TIMEOUT_CONFIG}"
            )
            OptimizeConstants.DEFAULT_CONFIGURABLE_TIMEOUT_CONFIG
        }
        onSuccess(timeout)
    }
}
