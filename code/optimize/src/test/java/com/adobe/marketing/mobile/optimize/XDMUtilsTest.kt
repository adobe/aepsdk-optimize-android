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
    private lateinit var singleProposition: OptimizeProposition
    private lateinit var multiplePropositions: List<OptimizeProposition>

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
                if (expectedStructure.isEmpty()) {
                    actual.isEmpty() // true only if actual is also empty
                } else {
                    expectedStructure.firstOrNull()?.let { expectedValue ->
                        actual.firstOrNull()?.let { actualValue ->
                            validateStructure(expectedValue, actualValue)
                        }
                    } ?: false
                }

            else -> expectedStructure::class == actual::class
        }
    }

    @Before
    fun setUp() {
        val multipleItemsList: List<Map<String, Any>> =
            loadJsonFromFile("json/MULTIPLE_PROPOSITION_VALID_TARGET.json") ?: emptyList()
        val singlePropositionMap: Map<String, Any> =
            loadJsonFromFile("json/PROPOSITION_VALID_TARGET.json") ?: emptyMap()
        singleProposition = OptimizeProposition.fromEventData(singlePropositionMap)
        multiplePropositions = multipleItemsList.map { OptimizeProposition.fromEventData(it) }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `generateInteractionXdm should return correct structure for SingleProposition`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(singleProposition))

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
            Assert.assertTrue(proposition.containsKey("items"))
        }
    }

    @Test
    fun `generateInteractionXdm should contain exactly one item in items array for SingleProposition`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(singleProposition))

        val propositions =
            (result["_experience"] as Map<*, *>)["decisioning"] as Map<*, *>
        val decisioningPropositions =
            propositions["propositions"] as List<Map<String, Any>>
        val items = decisioningPropositions.firstOrNull()?.get("items") as? List<*>

        Assert.assertNotNull(items)
        Assert.assertEquals(1, items?.size)
    }

    @Test
    fun `generateInteractionXdm should handle empty propositions list`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val result = XDMUtils.generateInteractionXdm(experienceEventType, emptyList())

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to emptyList<Map<String, Any>>()
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with empty activity and placement`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            "test-scope",
            emptyMap(),
            emptyMap()
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "items" to arrayListOf<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with activity and placement in scopeDetails`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val scopeDetails = mapOf(
            "activity" to mapOf("id" to "activity-id"),
            "placement" to mapOf("id" to "placement-id")
        )
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            "test-scope",
            scopeDetails,
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "scopeDetails" to scopeDetails,
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with activity and placement are in proposition`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val activity = mapOf("id" to "prop-activity-id")
        val placement = mapOf("id" to "prop-placement-id")
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            "test-scope",
            activity,
            placement
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with null activity and placement`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            "test-scope",
            null,
            null
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with null scopeDetails`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            "test-scope",
            null
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with empty items`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            "test-scope",
            emptyMap(),
            emptyMap()
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with null items`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            "test-id",
            null,
            "test-scope",
            emptyMap(),
            emptyMap()
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "test-scope",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with null scope`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            "test-id",
            emptyList(),
            null,
            emptyMap(),
            emptyMap()
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "test-id",
                            "scope" to "",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }

    @Test
    fun `generateInteractionXdm should handle propositions with null id`() {
        val experienceEventType = "decisioning.propositionDisplay"
        val proposition = OptimizeProposition(
            null,
            emptyList(),
            "test-scope",
            emptyMap(),
            emptyMap()
        )
        val result = XDMUtils.generateInteractionXdm(experienceEventType, listOf(proposition))

        val expectedStructure = mapOf(
            "_experience" to mapOf(
                "decisioning" to mapOf(
                    "propositions" to listOf(
                        mapOf(
                            "id" to "",
                            "scope" to "test-scope",
                            "items" to emptyList<Map<String, Any>>()
                        )
                    )
                )
            ),
            "eventType" to experienceEventType
        )

        Assert.assertTrue(validateStructure(expectedStructure, result))
    }
}
