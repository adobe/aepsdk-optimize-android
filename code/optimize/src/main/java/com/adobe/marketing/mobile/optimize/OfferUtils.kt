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
    fun List<Offer>.displayed() {
        if (isEmpty()) return
        val offerIds = mapTo(mutableSetOf()) { it.id }

        val uniquePropositions = map { it.proposition }
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
                        proposition.scopeDetails
                    )
                } else null
            }
        if (uniquePropositions.isEmpty()) return
        XDMUtils.trackWithData(
            XDMUtils.generateInteractionXdm(
                OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY, uniquePropositions
            )
        )
    }
}
