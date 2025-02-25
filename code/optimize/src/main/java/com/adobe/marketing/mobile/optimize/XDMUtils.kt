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
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log

object XDMUtils {

    private const val SELF_TAG: String = "XDMUtils"

    /**
     * Dispatches an event to track propositions with type {@value
     * * OptimizeConstants.EventType#OPTIMIZE} and source {@value
     * * OptimizeConstants.EventSource#REQUEST_CONTENT}.
     * No event is dispatched if the provided `xdm` is null or empty.
     * @param xdm `Map<String, Object>` containing the XDM data for the proposition
     * interactions.
     */
    @JvmStatic
    fun trackWithData(xdm: Map<String, Any>?) = xdm?.takeIf { it.isNotEmpty() }?.let {
        val eventData: MutableMap<String, Any> = hashMapOf(
            OptimizeConstants.EventDataKeys.REQUEST_TYPE to OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK,
            OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS to it
        )
        val edgeEvent = Event.Builder(
            OptimizeConstants.EventNames.TRACK_PROPOSITIONS_REQUEST,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONTENT
        ).setEventData(eventData).build()

        MobileCore.dispatchEvent(edgeEvent)
    } ?: Log.debug(
        OptimizeConstants.LOG_TAG,
        SELF_TAG,
        "Failed to dispatch track propositions request event, input xdm is null or empty."
    )

    /**
     * Generates a map containing XDM formatted data for `Experience Event - OptimizeProposition
     * Interactions` field group from this `OptimizeProposition` offer and given
     * `experienceEventType`.
     * The method returns null if the proposition reference within the offer is released and no
     * longer valid.
     * @param experienceEventType [String] containing the event type for the Experience Event
     * @return `Map<String></String>, Object>` containing the XDM data for the proposition interaction.
     */

    @JvmStatic
    fun generateInteractionXdm(
        experienceEventType: String,
        pendingInteraction: InteractionPropositionType
    ): Map<String, Any> {

        val finalPendingPropositions = when (pendingInteraction) {
            is InteractionPropositionType.MultiplePropositions -> pendingInteraction.propositions
            is InteractionPropositionType.SingleProposition -> listOf(pendingInteraction.proposition)
        }.map { prop ->
            mutableMapOf<String, Any>(
                OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_ID to prop.id,
                OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_SCOPE to prop.scope,
                OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_SCOPEDETAILS to prop.scopeDetails
            ).apply {
                if (prop.offers.isNotEmpty() && pendingInteraction is InteractionPropositionType.SingleProposition) {
                    put(OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_ITEMS, getOfferIds(prop))
                }
            }
        }

        return mapOf(
            OptimizeConstants.JsonKeys.EXPERIENCE to mapOf(
                OptimizeConstants.JsonKeys.EXPERIENCE_DECISIONING to mapOf(
                    OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS to finalPendingPropositions
                )
            ),
            OptimizeConstants.JsonKeys.EXPERIENCE_EVENT_TYPE to experienceEventType
        )
    }

    private fun getOfferIds(
        prop: OptimizeProposition
    ): List<Map<String, Any>> = prop.offers.mapNotNull { offer ->
        offer.id?.takeIf { it.isNotEmpty() }?.let {
            mapOf(OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_ITEMS_ID to it)
        }
    }

    sealed interface InteractionPropositionType {
        data class SingleProposition(val proposition: OptimizeProposition) : InteractionPropositionType
        data class MultiplePropositions(val propositions: List<OptimizeProposition>) : InteractionPropositionType
    }
}
