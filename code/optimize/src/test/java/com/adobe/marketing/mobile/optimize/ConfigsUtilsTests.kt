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
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.optimize.ConfigsUtils.retrieveConfigurationSharedState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class ConfigsUtilsTests {

    private val mockExtensionApi: ExtensionApi = mockk()
    private val mockEvent: Event = mockk()
    private val mockSharedState: SharedStateResult = mockk()

    @Test
    fun `test retrieveConfigurationSharedState returns valid configuration`() {
        val expectedConfig = mapOf("key" to "value")
        every {
            mockExtensionApi.getSharedState(
                OptimizeConstants.Configuration.EXTENSION_NAME,
                null,
                false,
                SharedStateResolution.ANY
            )
        } returns mockSharedState
        every { mockSharedState.value } returns expectedConfig

        val result = mockExtensionApi.retrieveConfigurationSharedState()
        Assert.assertNotNull(result)
        Assert.assertEquals(expectedConfig, result)
    }

    @Test
    fun `test retrieveConfigurationSharedState returns null when no shared state is found`() {
        every {
            mockExtensionApi.getSharedState(
                OptimizeConstants.Configuration.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.ANY
            )
        } returns null

        val result = mockExtensionApi.retrieveConfigurationSharedState(mockEvent)
        Assert.assertNull(result)
    }

    @Test
    fun `test retrieveConfigurationSharedState returns null when shared state has no value`() {
        every {
            mockExtensionApi.getSharedState(
                OptimizeConstants.Configuration.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.ANY
            )
        } returns mockSharedState
        every { mockSharedState.value } returns null

        val result = mockExtensionApi.retrieveConfigurationSharedState(mockEvent)
        Assert.assertNull(result)
    }

    @Test
    fun `test retrieveConfigurationSharedState with default event`() {
        val expectedConfig = mapOf("key" to "value")
        every {
            mockExtensionApi.getSharedState(
                OptimizeConstants.Configuration.EXTENSION_NAME,
                null, // Default event is null
                false,
                SharedStateResolution.ANY
            )
        } returns mockSharedState
        every { mockSharedState.value } returns expectedConfig

        val result = mockExtensionApi.retrieveConfigurationSharedState()
        Assert.assertNotNull(result)
        Assert.assertEquals(expectedConfig, result)
    }
}
