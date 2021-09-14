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
 * Listens for {@code EventType.Edge}, {@code EventSource.RESPONSE_CONTENT} events and invokes method on the
 * parent {@code OptimizeExtension} for handling the requests.
 */
public class ListenerEdgeResponseContent extends ExtensionListener {
    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type {@link String} containing event type this listener is registered to handle.
     * @param source {@code String} event source this listener is registered to handle.
     */
    ListenerEdgeResponseContent(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * This listener method listens to {@value OptimizeConstants.EventType#EDGE} and {@value OptimizeConstants.EventSource#RESPONSE_CONTENT} events.
     * <p>
     * It invokes method on the parent {@link OptimizeExtension} to handle Edge response containing error information when fetching propositions
     * for requested decision scopes.
     *
     * @param event {@link Event} to be processed.
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null || event.getEventData().isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    "Ignoring the Edge response event, either event is null or event data is null/ empty.");
            return;
        }

        final OptimizeExtension parentExtension = getOptimizeExtension();
        if (parentExtension == null) {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    "Ignoring the Edge response event, parent extension for this listener is null.");
            return;
        }

        parentExtension.handleEdgeResponse(event);
    }

    /**
     * Returns the parent extension for this listener.
     *
     * @return a {@link OptimizeExtension} instance registered with the {@code EventHub}.
     */
    OptimizeExtension getOptimizeExtension() {
        return (OptimizeExtension) getParentExtension();
    }
}
