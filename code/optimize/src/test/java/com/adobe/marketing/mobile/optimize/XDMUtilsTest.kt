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

import com.adobe.marketing.mobile.optimize.TestUtils.loadJsonFromFile
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import kotlin.test.Test

class XDMUtilsTest {
    private lateinit var singleProposition: XDMUtils.InteractionPropositionType.SingleProposition
    private lateinit var multiplePropositions: XDMUtils.InteractionPropositionType.MultiplePropositions

    private fun validateStructure(expectedStructure: Any, actual: Any): Boolean {
        return when {
            expectedStructure is Map<*, *> && actual is Map<*, *> ->
                expectedStructure.keys.all { key ->
                    expectedStructure[key]?.let { expectedValue ->
                        actual[key]?.let { actualValue ->
                            validateStructure(expectedValue, actualValue)
                        }
                    } ?: false
                }
            expectedStructure is List<*> && actual is List<*> ->
                expectedStructure.firstOrNull()?.let { expectedValue ->
                    actual.firstOrNull()?.let { actualValue ->
                        validateStructure(expectedValue, actualValue)
                    }
                } ?: false
            else -> expectedStructure::class == actual::class
        }
    }

    @Before
    fun setUp() {
        val multipleItemsList: List<Map<String, Any>> =
            loadJsonFromFile("json/MULTIPLE_PROPOSITION_VALID_TARGET.json") ?: emptyList()
        val singlePropositionMap: Map<String, Any> =
            loadJsonFromFile("json/PROPOSITION_VALID_TARGET.json") ?: emptyMap()
        singleProposition = XDMUtils.InteractionPropositionType.SingleProposition(
            OptimizeProposition.fromEventData(singlePropositionMap)
        )
        multiplePropositions = XDMUtils.InteractionPropositionType.MultiplePropositions(
            multipleItemsList.map { OptimizeProposition.fromEventData(it) }
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `generateInteractionXdm should return correct structure for SingleProposition`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val result = XDMUtils.generateInteractionXdm(experienceEventType, singleProposition)

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "",
                            "scope" to "",
                            "scopeDetails" to mapOf<String, Any>(),
                            "items" to listOf(
                                mapOf("id" to "")
                            )
                        )
                    )
                )
            ),
            "eventType" to ""
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should return correct structure for MultiplePropositions`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val result = XDMUtils.generateInteractionXdm(experienceEventType, multiplePropositions)

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "",
                            "scope" to "",
                            "scopeDetails" to mapOf<String, Any>()
                        )
                    )
                )
            ),
            "eventType" to ""
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should not include items array for MultiplePropositions`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val result = XDMUtils.generateInteractionXdm(experienceEventType, multiplePropositions)

        val propositions =
            (result["_experience"] as Map<*, *>)["decisioning"] as Map<*, *>
        val decisioningPropositions =
            propositions["propositions"] as List<Map<String, Any>>

        decisioningPropositions.forEach { proposition ->
            Assert.assertTrue(!proposition.containsKey("items"))
        }
    }
}
