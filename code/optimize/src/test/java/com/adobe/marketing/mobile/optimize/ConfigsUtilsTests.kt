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
import com.adobe.marketing.mobile.optimize.ConfigsUtils.retrieveOptimizeRequestTimeout
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Test

class ConfigsUtilsTests {

    private val mockExtensionApi: ExtensionApi = mockk()
    private val mockEvent: Event = mockk<Event>(relaxed = true)
    private val mockSharedState: SharedStateResult = mockk()

    @After
    fun tearDown() {
        unmockkAll()
    }

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

    @Test
    fun `returns default timeout when eventData is null`() {
        every { mockEvent.eventData } returns null
        val configData = mapOf<String, Any?>()

        Assert.assertEquals(10, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns timeout from eventData when present`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to 3000L)
        every { mockEvent.eventData } returns eventData
        val configData = mapOf<String, Any?>()

        Assert.assertEquals(3000L, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns timeout from configData when eventData contains Long_MAX_VALUE`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to Long.MAX_VALUE)
        val configData =
            mapOf<String, Any?>(OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT to 7000L)
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(7000L, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns default timeout when configData does not contain timeout and eventData contains Long_MAX_VALUE`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to Long.MAX_VALUE)
        val configData = mapOf<String, Any?>()
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(10, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns default timeout when DataReaderException is thrown`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to "invalid_value")
        val configData =
            mapOf<String, Any?>(OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT to 7000L)
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(10, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns default timeout when eventData contains Long_MAX_VALUE and configData timeout is invalid`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to Long.MAX_VALUE)
        val configData =
            mapOf<String, Any?>(OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT to "invalid_value")
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(10, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }
}
