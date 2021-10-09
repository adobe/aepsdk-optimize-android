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

package com.adobe.marketing.mobile.optimizeapp;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionListener;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

/**
 * Listens for {@code EventType.GENERIC_IDENTITY}, {@code EventSource.REQUEST_RESET} events and invokes method on the
 * parent {@code OptimizeExtension} for handling the requests.
 */
class ListenerGenericIdentityRequestReset extends ExtensionListener {
    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type {@link String} containing event type this listener is registered to handle.
     * @param source {@code String} event source this listener is registered to handle.
     */
    ListenerGenericIdentityRequestReset(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * This listener method listens to {@value OptimizeConstants.EventType#GENERIC_IDENTITY} and {@value OptimizeConstants.EventSource#REQUEST_RESET} events.
     * <p>
     * It invokes method on the parent {@link OptimizeExtension} to handle requests for clearing previously cached propositions.
     *
     * @param event {@link Event} to be processed.
     */
    @Override
    public void hear(final Event event) {
        if (event == null) {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    "Cannot process generic identity reset request, event is null.");
            return;
        }

        final OptimizeExtension parentExtension = getOptimizeExtension();
        if (parentExtension == null) {
            MobileCore.log(LoggingMode.DEBUG, OptimizeConstants.LOG_TAG,
                    "Ignoring the generic identity request reset event, parent extension for this listener is null.");
            return;
        }

        parentExtension.handleClearPropositions(event);
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
