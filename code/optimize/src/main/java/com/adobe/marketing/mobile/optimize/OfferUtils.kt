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

object OfferUtils {
    /**
     * Dispatches an event for the Edge network extension to send an Experience Event to the Edge
     * network with the display interaction data for the given list of [Offer]s.
     *
     * This function extracts unique [OptimizeProposition]s from the list of offers based on their
     * proposition ID and dispatches an event with multiple propositions.
     *
     * @see XDMUtils.trackWithData
     */
    @JvmStatic
    fun List<Offer>.displayed() {
        if (isEmpty()) return
        val uniquePropositions = mapToUniquePropositions()
        if (uniquePropositions.isEmpty()) return
        XDMUtils.trackWithData(
            XDMUtils.generateInteractionXdm(
                OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY,
                uniquePropositions
            )
        )
    }

    /**
     * Generates a map containing XDM formatted data for `Experience Event - OptimizeProposition
     * Interactions` field group from the given list of [Offer]s.
     *
     * This function extracts unique [OptimizeProposition]s from the list of offers based on their
     * proposition ID and generates XDM data for the interaction.
     *
     * @return [Map] containing the XDM data for the proposition interaction, or null if the list is empty
     * or no valid propositions are found
     */
    @JvmStatic
    fun List<Offer>.generateDisplayInteractionXdm(): Map<String, Any>? {
        if (isEmpty()) return null
        val uniquePropositions = mapToUniquePropositions()
        if (uniquePropositions.isEmpty()) return null
        return XDMUtils.generateInteractionXdm(
            OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY,
            uniquePropositions
        )
    }

    /**
     * Extracts unique [OptimizeProposition]s from the list of offers based on their proposition ID.
     *
     * <p>For each distinct proposition, it filters the associated offers to include only those
     * present in the original list (matched by offer ID).
     *
     * @return [List] of unique [OptimizeProposition]s with filtered offers, or an empty list if no valid propositions are found.
     */
    private fun List<Offer>.mapToUniquePropositions(): List<OptimizeProposition> {
        val offerIds = mapTo(mutableSetOf()) { it.id }
        return map { it.proposition }
            .distinctBy { it.id }
            .mapNotNull { proposition ->
                val displayedOffers = proposition.offers.filter {
                    it.id in offerIds
                }.distinctBy { it.id }
                if (displayedOffers.isNotEmpty()) {
                    OptimizeProposition(
                        proposition.id,
                        displayedOffers,
                        proposition.scope,
                        proposition.scopeDetails,
                        proposition.activity,
                        proposition.placement
                    )
                } else null
            }
    }
}
