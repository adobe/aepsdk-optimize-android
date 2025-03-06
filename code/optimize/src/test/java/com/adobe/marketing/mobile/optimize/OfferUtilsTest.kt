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

import com.adobe.marketing.mobile.optimize.OfferUtils.displayed
import com.adobe.marketing.mobile.optimize.TestUtils.loadJsonFromFile
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class OfferUtilsTest {
    private lateinit var multiplePropositions: XDMUtils.InteractionPropositionType.MultiplePropositions

    @Before
    fun setUp() {
        mockkStatic(XDMUtils::trackWithData)
        mockkStatic(XDMUtils::generateInteractionXdm)
        val multipleItemsList: List<Map<String, Any>> =
            loadJsonFromFile("json/MULTIPLE_OFFERS_WITH_COMMON_PROPOSITIONS.json") ?: emptyList()
        multiplePropositions = XDMUtils.InteractionPropositionType.MultiplePropositions(
            multipleItemsList.map { OptimizeProposition.fromEventData(it) }
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test displayed() calls XDMUtils with unique propositions`() {
        val offers = multiplePropositions.propositions.flatMap { it.offers }
        assertEquals(3, offers.size, "Offers size should be 3")
        assertEquals(2, multiplePropositions.propositions.distinctBy { it.id }.size, "Proposition size should be 2")
        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        offers.displayed()

        val capturedXdm = xdmSlot.captured

        verify { XDMUtils.trackWithData(any()) }

        val capturedPropositions = (
            (
                (capturedXdm["_experience"] as? Map<*, *>)
                    ?.get("decisioning") as? Map<*, *>
                )
                ?.get("propositions") as? List<Map<String, Any>>
            )
            ?.mapNotNull { OptimizeProposition.fromEventData(it) }
            ?.let { XDMUtils.InteractionPropositionType.MultiplePropositions(it) }
            ?.propositions

        assertEquals(2, capturedPropositions?.size, "Proposition size should be 2")
    }
}
