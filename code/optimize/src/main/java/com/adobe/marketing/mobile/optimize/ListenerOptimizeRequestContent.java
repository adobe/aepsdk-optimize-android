/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.optimize;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionListener;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.Map;

/**
 * Listens for {@code EventType.Optimize}, {@code EventSource.REQUEST_CONTENT} events and invokes method on the
 * parent {@code OptimizeExtension} for handling the requests.
 */
class ListenerOptimizeRequestContent extends ExtensionListener {
    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type {@link String} containing event type this listener is registered to handle.
     * @param source {@code String} event source this listener is registered to handle.
     */
    ListenerOptimizeRequestContent(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * This listener method listens to {@value OptimizeConstants.EventType#OPTIMIZE} and {@value OptimizeConstants.EventSource#REQUEST_CONTENT} events.
     * <p>
     * It invokes method on the parent {@link OptimizeExtension} to handle requests for,
     * <ul>
     *     <li>Updating propositions in the extension by sending personalization query requests to the Experience Edge.</li>
     *     <li>Retrieving previously fetched and cached propositions in the extension.</li>
     *     <li>Sending proposition interactions information to the Edge network.</li>
     * </ul>
     *
     * @param event {@link Event} to be processed.
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null || event.getEventData().isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    "Ignoring the Optimize request event, either event is null or event data is null/ empty.");
            return;
        }

        final OptimizeExtension parentExtension = getOptimizeExtension();
        if (parentExtension == null) {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    "Ignoring the Optimize request event, parent extension for this listener is null.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        final String requestType = (String) eventData.get(OptimizeConstants.EventDataKeys.REQUEST_TYPE);

        if (requestType.equals(OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE)) {
            parentExtension.handleUpdatePropositions(event);
        } else if (requestType.equals(OptimizeConstants.EventDataValues.REQUEST_TYPE_GET)) {
            parentExtension.handleGetPropositions(event);
        } else if (requestType.equals(OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK)) {
            parentExtension.handleTrackPropositions(event);
        } else {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    String.format("Ignoring the Optimize request event, provided request type (%s) is not handled by this extension.", requestType));
        }
    }

    /**
     * Returns the parent extension for this listener.
     *
     * @return an {@link OptimizeExtension} instance registered with the {@code EventHub}.
     */
    OptimizeExtension getOptimizeExtension() {
        return (OptimizeExtension) getParentExtension();
    }
}
