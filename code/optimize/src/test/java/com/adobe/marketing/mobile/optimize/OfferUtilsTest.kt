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
import kotlin.test.assertNotNull

class OfferUtilsTest {
    private lateinit var offersWithSameProposition: List<Offer>
    private lateinit var duplicateOffersWithSameProposition: List<Offer>

    @Before
    fun setUp() {
        mockkStatic(XDMUtils::trackWithData)
        mockkStatic(XDMUtils::generateInteractionXdm)

        val multipleItemsList: List<Map<String, Any>> =
            loadJsonFromFile("json/MULTIPLE_OFFERS_WITH_COMMON_PROPOSITIONS.json") ?: emptyList()
        offersWithSameProposition = multipleItemsList.map { OptimizeProposition.fromEventData(it) }.flatMap { it.offers }

        val multipleDuplicateItemsList: List<Map<String, Any>> =
            loadJsonFromFile("json/DUPLICATE_OFFERS_WITH_COMMON_PROPOSITIONS.json") ?: emptyList()
        duplicateOffersWithSameProposition =
            multipleDuplicateItemsList.map { OptimizeProposition.fromEventData(it) }.flatMap { it.offers }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test displayed() does nothing when list is empty`() {
        every { XDMUtils.trackWithData(any()) } just Runs

        emptyList<Offer>().displayed()

        verify(exactly = 0) { XDMUtils.trackWithData(any()) }
    }

    @Test
    fun `test displayed() handles single offer correctly`() {
        val singleOffer = listOf(offersWithSameProposition.first())

        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        singleOffer.displayed()

        val capturedXdm = xdmSlot.captured
        verify { XDMUtils.trackWithData(any()) }

        val rawPropositions = (
            (capturedXdm["_experience"] as? Map<String, Any>)
                ?.get("decisioning") as? Map<String, Any>
            )?.get("propositions") as? List<Map<String, Any>>

        val offers = rawPropositions?.flatMap { proposition ->
            (proposition["items"] as? List<Map<String, Any>>).orEmpty()
        }

        assertNotNull(rawPropositions)
        assertEquals(1, rawPropositions.size)
        assertEquals(
            "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ8",
            rawPropositions.first()["id"] as? String
        )
        assertNotNull(offers)
        assertEquals(1, offers.size)
        assertEquals("246314", offers.first()["id"] as? String)
    }

    @Test
    fun `test displayed() handles multiple offers with shared propositions`() {
        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        offersWithSameProposition.displayed()

        val capturedXdm = xdmSlot.captured
        verify { XDMUtils.trackWithData(any()) }

        val rawPropositions = (
            (capturedXdm["_experience"] as? Map<String, Any>)
                ?.get("decisioning") as? Map<String, Any>
            )?.get("propositions") as? List<Map<String, Any>>

        val offers = rawPropositions?.flatMap { proposition ->
            (proposition["items"] as? List<Map<String, Any>>).orEmpty()
        }

        assertNotNull(rawPropositions)
        assertEquals(2, rawPropositions.size)
        assertNotNull(offers)
        assertEquals(3, offers.size)
        assertEquals("246314", offers.first()["id"])
        assertEquals("246316", offers.last()["id"])
    }

    @Test
    fun `test displayed() removes propositions with no matching offers`() {
        val unrelatedOffers = listOf(
            offersWithSameProposition[0],
            offersWithSameProposition[2]
        )

        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        unrelatedOffers.displayed()

        val capturedXdm = xdmSlot.captured
        verify { XDMUtils.trackWithData(any()) }

        val rawPropositions = (
            (capturedXdm["_experience"] as? Map<String, Any>)
                ?.get("decisioning") as? Map<String, Any>
            )?.get("propositions") as? List<Map<String, Any>>

        val offers = rawPropositions?.flatMap { proposition ->
            (proposition["items"] as? List<Map<String, Any>>).orEmpty()
        }

        assertNotNull(rawPropositions)
        assertEquals(2, rawPropositions.size)
        assertEquals(offers?.size, 2)
        assertEquals(offers?.first()?.get("id"), "246314")
        assertEquals(offers?.last()?.get("id"), "246316")
    }

    @Test
    fun `test displayed() handles duplicate offers correctly`() {
        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        duplicateOffersWithSameProposition.displayed()

        val capturedXdm = xdmSlot.captured
        verify { XDMUtils.trackWithData(any()) }

        val rawPropositions = (
            (capturedXdm["_experience"] as? Map<String, Any>)
                ?.get("decisioning") as? Map<String, Any>
            )?.get("propositions") as? List<Map<String, Any>>

        assertNotNull(rawPropositions)
        assertEquals(1, rawPropositions.size)

        val proposition = rawPropositions.first()
        assertEquals("prop-789", proposition["id"])

        val offers = (proposition["items"] as? List<Map<String, Any>>).orEmpty()
        assertNotNull(offers)
        assertEquals(1, offers.size)

        val offer = offers.first()
        assertEquals("offer1", offer["id"])
    }

    @Test
    fun `test displayed() handles all offers belonging to the same proposition`() {
        val allSamePropositionOffers = offersWithSameProposition.filter {
            it.proposition.id == offersWithSameProposition.first().proposition.id
        }

        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        allSamePropositionOffers.displayed()

        val capturedXdm = xdmSlot.captured
        verify { XDMUtils.trackWithData(any()) }

        val rawPropositions = (
            (capturedXdm["_experience"] as? Map<String, Any>)
                ?.get("decisioning") as? Map<String, Any>
            )?.get("propositions") as? List<Map<String, Any>>

        assertNotNull(rawPropositions)
        assertEquals(1, rawPropositions.size)

        val proposition = rawPropositions.first()
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ8", proposition["id"])

        val offers = (proposition["items"] as? List<Map<String, Any>>).orEmpty()
        assertNotNull(offers)
        assertEquals(1, offers.size)

        val offer = offers.first()
        assertEquals("246314", offer["id"])
    }

    @Test
    fun `test displayed() handles offers with mixed proposition references`() {
        val mixedOffers = listOf(
            offersWithSameProposition.first(),
            duplicateOffersWithSameProposition.first()
        )

        val xdmSlot = slot<Map<String, Any>>()
        every { XDMUtils.trackWithData(capture(xdmSlot)) } just Runs

        mixedOffers.displayed()

        val capturedXdm = xdmSlot.captured
        verify { XDMUtils.trackWithData(any()) }

        val rawPropositions = (
            (capturedXdm["_experience"] as? Map<String, Any>)
                ?.get("decisioning") as? Map<String, Any>
            )?.get("propositions") as? List<Map<String, Any>>

        assertNotNull(rawPropositions)
        assertEquals(2, rawPropositions.size)

        val propositionIds = rawPropositions.map { it["id"] as? String }
        assertEquals(
            setOf(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ8",
                "prop-789"
            ),
            propositionIds.toSet()
        )

        val offers = rawPropositions.flatMap { proposition ->
            (proposition["items"] as? List<Map<String, Any>>).orEmpty()
        }

        assertNotNull(offers)
        assertEquals(2, offers.size)

        val offerIds = offers.map { it["id"] as? String }
        assertEquals(setOf("246314", "offer1"), offerIds.toSet())
    }

    @Test
    fun `test displayed() does nothing if uniquePropositions is empty`() {
        val unrelatedOffers = listOf<Offer>()

        every { XDMUtils.trackWithData(any()) } just Runs

        unrelatedOffers.displayed()

        verify(exactly = 0) { XDMUtils.trackWithData(any()) }
    }
}
