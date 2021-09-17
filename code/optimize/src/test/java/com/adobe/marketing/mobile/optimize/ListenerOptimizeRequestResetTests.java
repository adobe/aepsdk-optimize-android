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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OptimizeExtension.class, ExtensionApi.class})
public class ListenerOptimizeRequestResetTests {
    @Mock
    OptimizeExtension mockOptimizeExtension;

    @Mock
    ExtensionApi mockExtensionApi;

    private ListenerOptimizeRequestReset listener;

    @Before
    public void setup() {
        listener = spy(new ListenerOptimizeRequestReset(mockExtensionApi,
                "com.adobe.eventType.optimize", "com.adobe.eventSource.requestReset"));
    }

    @Test
    public void testHear() throws Exception {
        // setup
        when(listener.getOptimizeExtension()).thenReturn(mockOptimizeExtension);
        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request",
                "com.adobe.eventType.optimize",
                "com.adobe.eventSource.requestReset")
                .build();

        // test
        listener.hear(testEvent);

        // verify
        verify(mockOptimizeExtension, Mockito.times(1)).handleClearPropositions(testEvent);
    }

    @Test
    public void testHear_nullEvent() throws Exception {
        // setup
        when(listener.getOptimizeExtension()).thenReturn(mockOptimizeExtension);

        // test
        listener.hear(null);

        // verify
        verify(mockOptimizeExtension, Mockito.never()).handleClearPropositions(Mockito.any(Event.class));
    }

    @Test
    public void testHear_nullEventData() throws Exception {
        // setup
        when(listener.getOptimizeExtension()).thenReturn(mockOptimizeExtension);
        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request",
                "com.adobe.eventType.optimize",
                "com.adobe.eventSource.requestReset")
                .setEventData(null)
                .build();

        // test
        listener.hear(testEvent);

        // verify
        verify(mockOptimizeExtension, Mockito.times(1)).handleClearPropositions(testEvent);
    }

    @Test
    public void testHear_emptyEventData() throws Exception {
        // setup
        when(listener.getOptimizeExtension()).thenReturn(mockOptimizeExtension);
        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request",
                "com.adobe.eventType.optimize",
                "com.adobe.eventSource.requestReset")
                .setEventData(new HashMap<String, Object>())
                .build();

        // test
        listener.hear(testEvent);

        // verify
        verify(mockOptimizeExtension, Mockito.times(1)).handleClearPropositions(testEvent);
    }

    @Test
    public void testHear_nullParentExtension() throws Exception {
        // setup
        when(listener.getOptimizeExtension()).thenReturn(null);
        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request",
                "com.adobe.eventType.optimize",
                "com.adobe.eventSource.requestReset")
                .build();

        // test
        listener.hear(testEvent);

        // verify
        verify(mockOptimizeExtension, Mockito.never()).handleClearPropositions(testEvent);
    }
}