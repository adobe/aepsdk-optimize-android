package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ConfigsManagerTests {

    @Before
    fun setup() {
        mockkStatic(MobileCore::class)
        mockkStatic(Log::class)
        ConfigsManager::class.java.getDeclaredField("isFetchingConfigurations").apply {
            isAccessible = true
            setBoolean(ConfigsManager, false)
        }
    }

    @Test
    fun `getConfigurableTimeoutConfig fetches and retrieves valid timeout configuration`() {
        val mockResponseEvent = mockk<Event> {
            every { eventData } returns mapOf(OptimizeConstants.EventDataKeys.TIMEOUT to 5.0)
        }

        every {
            MobileCore.dispatchEventWithResponseCallback(
                any(),
                any(),
                any<AdobeCallbackWithError<Event>>()
            )
        } answers {
            val callback = thirdArg<AdobeCallbackWithError<Event>>()
            callback.call(mockResponseEvent)
        }

        ConfigsManager.getConfigurableTimeoutConfig { timeout ->
            Assert.assertEquals(5.0, timeout, 0.0)
        }
    }


    @Test
    fun `getConfigurableTimeoutConfig returns default timeout when configuration is missing`() {
        val mockResponseEvent = mockk<Event> {
            every { eventData } returns emptyMap()
        }

        every {
            MobileCore.dispatchEventWithResponseCallback(
                any(),
                any(),
                any<AdobeCallbackWithError<Event>>()
            )
        } answers {
            val callback = thirdArg<AdobeCallbackWithError<Event>>()
            callback.call(mockResponseEvent)
        }

        ConfigsManager.getConfigurableTimeoutConfig { timeout ->
            Assert.assertEquals(10.0, timeout, 0.0)
        }
    }

    @Test
    fun `getConfigurableTimeoutConfig handles fetch failure and returns default timeout`() {
        every {
            MobileCore.dispatchEventWithResponseCallback(
                any(),
                any(),
                any<AdobeCallbackWithError<Event>>()
            )
        } answers {
            val callback = thirdArg<AdobeCallbackWithError<Event>>()
            callback.fail(mockk {
                every { errorName } returns "FetchError"
            })
        }

        ConfigsManager.getConfigurableTimeoutConfig { timeout ->
            Assert.assertEquals(10.0, timeout, 0.0)
        }
    }

    @Test
    fun `getConfigurableTimeoutConfig handles exception and returns default timeout`() {
        val mockResponseEvent = mockk<Event> {
            every { eventData } returns emptyMap()
        }
        every {
            MobileCore.dispatchEventWithResponseCallback(
                any(),
                any(),
                any<AdobeCallbackWithError<Event>>()
            )
        } answers {
            val callback = thirdArg<AdobeCallbackWithError<Event>>()
            callback.call(mockResponseEvent)
        }
        ConfigsManager.getConfigurableTimeoutConfig { timeout ->
            Assert.assertEquals(10.0, timeout, 0.0)
        }
    }
}
