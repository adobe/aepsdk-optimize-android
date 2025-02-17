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
import com.adobe.marketing.mobile.optimize.ConfigsUtils.retrieveOptimizeRequestTimeout
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Test

class ConfigsUtilsTests {

    private val mockEvent: Event = mockk<Event>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `returns default timeout when eventData is null`() {
        every { mockEvent.eventData } returns null
        val configData = mapOf<String, Any?>()

        Assert.assertEquals(10000, mockEvent.retrieveOptimizeRequestTimeout(configData))
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
            mapOf<String, Any?>(OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT to 7)
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(7000L, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns default timeout when configData does not contain timeout and eventData contains Long_MAX_VALUE`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to Long.MAX_VALUE)
        val configData = mapOf<String, Any?>()
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(10000, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns default timeout when DataReaderException is thrown`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to "invalid_value")
        val configData =
            mapOf<String, Any?>(OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT to 7)
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(10000, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }

    @Test
    fun `returns default timeout when eventData contains Long_MAX_VALUE and configData timeout is invalid`() {
        val eventData = mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to Long.MAX_VALUE)
        val configData =
            mapOf<String, Any?>(OptimizeConstants.EventDataKeys.CONFIGS_TIMEOUT to "invalid_value")
        every { mockEvent.eventData } returns eventData

        Assert.assertEquals(10000, mockEvent.retrieveOptimizeRequestTimeout(configData))
    }
}
