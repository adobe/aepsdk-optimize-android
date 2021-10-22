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

import android.util.Base64;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, Base64.class})
@SuppressWarnings("unchecked")
public class OptimizeTests {
    private Map<DecisionScope, Proposition> responseMap;
    private AdobeError responseError;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Base64.class);
        Mockito.when(Base64.encodeToString((byte[]) any(), anyInt())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                return java.util.Base64.getEncoder().encodeToString((byte[]) invocation.getArguments()[0]);
            }
        });
        Mockito.when(Base64.decode((byte[]) any(), anyInt())).thenAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                return java.util.Base64.getDecoder().decode((byte[]) invocation.getArguments()[0]);
            }
        });
    }

    @After
    public void teardown() {
        responseMap = null;
        responseError = null;
    }

    @Test
    public void test_extensionVersion() {
        // test
        final String extensionVersion = Optimize.extensionVersion();
        assertEquals("extensionVersion API should return the correct version string.", "1.0.0",
                extensionVersion);
    }

    @Test
    public void test_registerExtension() {
        // test
        Optimize.registerExtension();
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // The Optimize extension should successfully register with Mobile Core
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(eq(OptimizeExtension.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension error callback should not be null.", extensionErrorCallback);

        // should be able to invoke the callback
        // extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    @Test
    public void testUpdatePropositions_validDecisionScope() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

        Optimize.updatePropositions(scopes, null, null);

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event event = eventCaptor.getValue();

        assertNotNull(event);
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), event.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), event.getSource());

        final Map<String, Object> eventData = event.getEventData();
        assertEquals("updatepropositions", eventData.get("requesttype"));
        assertNull(eventData.get("xdm"));
        assertNull(eventData.get("data"));

        final List<Map<String, Object>> scopesList = (List<Map<String, Object>>)eventData.get("decisionscopes");
        assertEquals(1, scopesList.size());

        final Map<String, Object> scopeData = scopesList.get(0);
        assertNotNull(scopeData);
        assertEquals(1, scopeData.size());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", scopeData.get("name"));
    }

    @Test
    public void testUpdatePropositions_validDecisionScopeWithXDMAndData() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

        Optimize.updatePropositions(scopes,
                new HashMap<String, Object>() {
                    {
                        put("myXdmKey", "myXdmValue");
                    }
                },
                new HashMap<String, Object>() {
                    {
                        put("myKey", "myValue");
                    }
                });


        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event event = eventCaptor.getValue();

        assertNotNull(event);
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), event.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), event.getSource());

        final Map<String, Object> eventData = event.getEventData();
        assertEquals("updatepropositions", eventData.get("requesttype"));

        Map<String, Object> xdm = (Map<String, Object>) eventData.get("xdm");
        assertNotNull(xdm);
        assertEquals(1, xdm.size());
        assertEquals("myXdmValue", xdm.get("myXdmKey"));

        Map<String, Object> data = (Map<String, Object>) eventData.get("data");
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("myValue", data.get("myKey"));

        final List<Map<String, Object>> scopesList = (List<Map<String, Object>>)eventData.get("decisionscopes");
        assertEquals(1, scopesList.size());

        final Map<String, Object> scopeData = scopesList.get(0);
        assertNotNull(scopeData);
        assertEquals(1, scopeData.size());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", scopeData.get("name"));
    }

    @Test
    public void testUpdatePropositions_multipleValidDecisionScopes() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
        scopes.add(new DecisionScope("myMbox"));

        Optimize.updatePropositions(scopes, null, null);

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event event = eventCaptor.getValue();

        assertNotNull(event);
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), event.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), event.getSource());

        final Map<String, Object> eventData = event.getEventData();
        assertEquals("updatepropositions", eventData.get("requesttype"));
        assertNull(eventData.get("xdm"));
        assertNull(eventData.get("data"));

        final List<Map<String, Object>> scopesList = (List<Map<String, Object>>)eventData.get("decisionscopes");
        assertEquals(2, scopesList.size());

        final Map<String, Object> scopeData = scopesList.get(0);
        assertNotNull(scopeData);
        assertEquals(1, scopeData.size());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", scopeData.get("name"));

        final Map<String, Object> scopeData2 = scopesList.get(1);
        assertNotNull(scopeData2);
        assertEquals(1, scopeData2.size());
        assertEquals("myMbox", scopeData2.get("name"));
    }

    @Test
    public void testUpdatePropositions_emptyDecisionScopesList() throws Exception {
        // test
        Optimize.updatePropositions(new ArrayList<DecisionScope>(), null, null);

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testUpdatePropositions_nullDecisionScopesList() throws Exception {
        // test
        Optimize.updatePropositions(null, null, null);

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testUpdatePropositions_invalidDecisionScopeInList() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9"));

        Optimize.updatePropositions(scopes, null, null);

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(2));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testGetPropositions_validDecisionScope() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

        Optimize.getPropositions(scopes, new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallbackWithError> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), callbackCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event event = eventCaptor.getValue();
        final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

        // verify dispatched event
        assertNotNull(event);
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), event.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), event.getSource());
        final Map<String, Object> eventData = event.getEventData();
        assertEquals("getpropositions", eventData.get("requesttype"));

        final List<Map<String, Object>> scopesList = (List<Map<String, Object>>)eventData.get("decisionscopes");
        assertEquals(1, scopesList.size());

        final Map<String, Object> scopeData1 = scopesList.get(0);
        assertNotNull(scopeData1);
        assertEquals(1, scopeData1.size());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", scopeData1.get("name"));

        // verify callback response
        final Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        final DecisionScope scope = new DecisionScope(proposition.getScope());

        final List<Map<String, Object>> propositionsList = new ArrayList<>();
        propositionsList.add(proposition.toEventData());

        final Map<String, Object> responseEventData = new HashMap<>();
        responseEventData.put("propositions", propositionsList);
        final Event responseEvent = new Event.Builder("Optimize Response", "com.adobe.eventType.optimize", "com.adobe.eventSource.responseContent")
                .setEventData(responseEventData).build();
        callbackWithError.call(responseEvent);

        assertNull(responseError);
        assertNotNull(responseMap);
        assertEquals(1, responseMap.size());
        Proposition actualProposition = responseMap.get(new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
        assertEquals(proposition, actualProposition);
    }

    @Test
    public void testGetPropositions_multipleValidDecisionScopes() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
        scopes.add(new DecisionScope("myMbox"));

        Optimize.getPropositions(scopes, new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), any(AdobeCallbackWithError.class), any(ExtensionErrorCallback.class));
        final Event event = eventCaptor.getValue();
        assertNotNull(event);

        assertEquals("com.adobe.eventType.optimize".toLowerCase(), event.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), event.getSource());
        final Map<String, Object> eventData = event.getEventData();
        assertEquals("getpropositions", eventData.get("requesttype"));

        final List<Map<String, Object>> scopesList = (List<Map<String, Object>>)eventData.get("decisionscopes");
        assertEquals(2, scopesList.size());

        final Map<String, Object> scopeData1 = scopesList.get(0);
        assertNotNull(scopeData1);
        assertEquals(1, scopeData1.size());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", scopeData1.get("name"));

        final Map<String, Object> scopeData2 = scopesList.get(1);
        assertNotNull(scopeData2);
        assertEquals(1, scopeData2.size());
        assertEquals("myMbox", scopeData2.get("name"));
    }

    @Test
    public void testGetPropositions_invalidDecisionScopeInList() throws Exception {
        // test
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(new DecisionScope("eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9"));

        Optimize.getPropositions(null, new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
    }

    @Test
    public void testGetPropositions_emptyDecisionScopesList() throws Exception {
        // test
        Optimize.getPropositions(new ArrayList<DecisionScope>(), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
    }

    @Test
    public void testGetPropositions_nullDecisionScopesList() throws Exception {
        // test
        Optimize.getPropositions(null, new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
    }

    @Test
    public void testOnPropositionsUpdate_validProposition() throws Exception {
        // test
        Optimize.onPropositionsUpdate(new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(final AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(final Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<AdobeCallbackWithError> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        MobileCore.registerEventListener(eq("com.adobe.eventType.optimize"), eq("com.adobe.eventSource.notification"),
                callbackCaptor.capture());
        final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

        final Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        final DecisionScope scope = new DecisionScope(proposition.getScope());

        final List<Map<String, Object>> propositionsList = new ArrayList<>();
        propositionsList.add(proposition.toEventData());

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("propositions", propositionsList);
        final Event event = new Event.Builder("Optimize Notification", "com.adobe.eventType.optimize", "com.adobe.eventSource.notification")
                .setEventData(eventData).build();
        callbackWithError.call(event);

        assertNull(responseError);
        assertNotNull(responseMap);
        assertEquals(1, responseMap.size());
        Proposition actualProposition = responseMap.get(new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
        assertEquals(proposition, actualProposition);
    }

    @Test
    public void testOnPropositionsUpdate_emptyPropositionData() throws Exception {
        // test
        Optimize.onPropositionsUpdate(new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(final AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(final Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<AdobeCallbackWithError> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        MobileCore.registerEventListener(eq("com.adobe.eventType.optimize"), eq("com.adobe.eventSource.notification"),
                callbackCaptor.capture());
        final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

        final List<Map<String, Object>> propositionsList = new ArrayList<>();
        propositionsList.add(new HashMap<String, Object>());

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("propositions", propositionsList);
        final Event event = new Event.Builder("Optimize Notification", "com.adobe.eventType.optimize", "com.adobe.eventSource.notification")
                .setEventData(eventData).build();
        callbackWithError.call(event);

        assertNull(responseError);
        assertNull(responseMap);
    }

    @Test
    public void testOnPropositionsUpdate_nullPropositionData() throws Exception {
        // test
        Optimize.onPropositionsUpdate(new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(final AdobeError adobeError) {
                responseError = adobeError;
            }

            @Override
            public void call(final Map<DecisionScope, Proposition> propositionsMap) {
                responseMap = propositionsMap;
            }
        });

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<AdobeCallbackWithError> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
        MobileCore.registerEventListener(eq("com.adobe.eventType.optimize"), eq("com.adobe.eventSource.notification"),
                callbackCaptor.capture());
        final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

        final List<Map<String, Object>> propositionsList = new ArrayList<>();
        propositionsList.add(null);

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("propositions", propositionsList);
        final Event event = new Event.Builder("Optimize Notification", "com.adobe.eventType.optimize", "com.adobe.eventSource.notification")
                .setEventData(eventData).build();
        callbackWithError.call(event);

        assertNull(responseError);
        assertNull(responseMap);
    }

    @Test
    public void test_clearCachedPropositions() {
        // test
        Optimize.clearCachedPropositions();

        //verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event event = eventCaptor.getValue();

        assertNotNull(event);
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), event.getType());
        assertEquals("com.adobe.eventSource.requestReset".toLowerCase(), event.getSource());
        assertTrue(event.getEventData().isEmpty());
    }
}
