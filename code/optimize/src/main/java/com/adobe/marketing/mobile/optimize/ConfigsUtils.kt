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
import com.adobe.marketing.mobile.util.DataReader
import com.adobe.marketing.mobile.util.DataReaderException

object ConfigsUtils {
    @JvmStatic
    fun Event.retrieveOptimizeRequestTimeout(configData: Map<String, Any?>): Long {
        val defaultTimeout =
            OptimizeConstants.UPDATE_RESPONSE_DEFAULT_TIMEOUT.times(OptimizeConstants.TIMEOUT_CONVERSION_FACTOR)
                .toLong()
        return try {
            val eventTimeout = DataReader.getLong(eventData, OptimizeConstants.EventDataKeys.TIMEOUT)
            if (eventTimeout == Long.MAX_VALUE)
                DataReader.getLong(configData, OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT).times(OptimizeConstants.TIMEOUT_CONVERSION_FACTOR)
            else eventTimeout
        } catch (e: DataReaderException) {
            defaultTimeout
        }
    }
}
