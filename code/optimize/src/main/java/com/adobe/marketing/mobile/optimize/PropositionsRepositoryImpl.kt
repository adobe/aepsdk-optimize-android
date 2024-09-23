/*
  Copyright 2024 Adobe. All rights reserved.
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

object PropositionsRepositoryImpl : PropositionsRepository {
    private val SELF_TAG: String = "PropositionsDisplayRepositoryImpl"

    override fun trackTapInteraction(xdm: Map<String, Any>) {
        dispatchTrackEvent(xdm)
    }

    override fun trackDisplayInteraction(xdm: Map<String, Any>) {
        dispatchTrackEvent(xdm)
    }

    private fun dispatchTrackEvent(xdm: Map<String, Any>?) {
        if (xdm.isNullOrEmpty()) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Failed to dispatch track propositions request event: xdm is null or empty."
            )
            return
        }

        val eventData = hashMapOf(
            OptimizeConstants.EventDataKeys.REQUEST_TYPE to OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK,
            OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS to xdm
        )

        // check: event type for tap and display are same or different
        val edgeEvent = Event.Builder(
            OptimizeConstants.EventNames.TRACK_PROPOSITIONS_REQUEST,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONTENT
        ).apply {
            setEventData(eventData)
        }.build()

        MobileCore.dispatchEvent(edgeEvent)
    }
}
