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

import android.app.Application;
import android.content.Context;
import android.util.Base64;

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Base64.class, Event.class, ExtensionApi.class, MobileCore.class})
@SuppressWarnings("unchecked")
public class OptimizeExtensionTests {
    private OptimizeExtension extension;
    private ExecutorService testExecutor;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;

    @Mock
    Application mockApplication;

    @Mock
    Context mockContext;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);

        extension = spy(new OptimizeExtension(mockExtensionApi));
        testExecutor = Executors.newSingleThreadExecutor();
        when(extension.getExecutor()).thenReturn(testExecutor);

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
        if (!testExecutor.isShutdown()) {
            testExecutor.shutdownNow();
        }
    }

    @Test
    public void test_getName() {
        // test
        final String extensionName = extension.getName();
        assertEquals("getName should return the correct extension name.", "com.adobe.optimize", extensionName);
    }

    @Test
    public void test_getVersion() {
        // test
        final String extensionVersion = extension.getVersion();
        assertEquals("getVersion should return the correct extension version.", "1.0.0", extensionVersion);
    }

    @Test
    public void test_registration() {
        // setup
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
        clearInvocations(mockExtensionApi);

        // test
        extension = new OptimizeExtension(mockExtensionApi);

        // verify
        verify(mockExtensionApi, Mockito.times(1)).registerEventListener(eq("com.adobe.eventType.optimize"),
                eq("com.adobe.eventSource.requestContent"), eq(ListenerOptimizeRequestContent.class),
                callbackCaptor.capture());

        final ExtensionErrorCallback errorCallback = callbackCaptor.getValue();
        assertNotNull(errorCallback);
    }

    @Test
    public void testHandleUpdatePropositions_nullEvent() throws Exception {
        // test
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        extension.handleUpdatePropositions(null);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void testHandleUpdatePropositions_nullEventData() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(null)
                .build();


        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void testHandleUpdatePropositions_emptyEventData() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(new HashMap<String, Object>())
                .build();


        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void testHandleUpdatePropositions_validDecisionScope() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();


        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());

        final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
        assertNotNull(query);
        final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
        assertNotNull(queryPersonalization);
        final List<DecisionScope> scopes = (List<DecisionScope>) queryPersonalization.get("decisionScopes");
        assertNotNull(scopes);
        assertEquals(1, scopes.size());
        assertEquals(testScope, scopes.get(0));

        final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
        assertNotNull(xdm);
        assertEquals(1, xdm.size());
        assertEquals("personalization.request", xdm.get("eventType"));

        final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
        assertNull(data);

        final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
        assertNull(datasetId);
    }

    @Test
    public void testHandleUpdatePropositions_validDecisionScopeWithXdmAndDataAndDatasetId() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                put("optimize.datasetId", "111111111111111111111111");
            }
        });

        final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });
        testEventData.put("xdm", new HashMap<String, Object>(){
            {
                put("myXdmKey", "myXdmValue");
            }
        });
        testEventData.put("data", new HashMap<String, Object>(){
            {
                put("myKey", "myValue");
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());

        final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
        assertNotNull(query);
        final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
        assertNotNull(queryPersonalization);
        final List<DecisionScope> scopes = (List<DecisionScope>) queryPersonalization.get("decisionScopes");
        assertNotNull(scopes);
        assertEquals(1, scopes.size());
        assertEquals(testScope, scopes.get(0));

        final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
        assertNotNull(xdm);
        assertEquals(2, xdm.size());
        assertEquals("personalization.request", xdm.get("eventType"));
        assertEquals("myXdmValue", xdm.get("myXdmKey"));

        final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("myValue", data.get("myKey"));

        final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
        assertEquals("111111111111111111111111", datasetId);
    }

    @Test
    public void testHandleUpdatePropositions_validDecisionScopeWithXdmAndDataAndNoDatasetId() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });
        testEventData.put("xdm", new HashMap<String, Object>(){
            {
                put("myXdmKey", "myXdmValue");
            }
        });
        testEventData.put("data", new HashMap<String, Object>(){
            {
                put("myKey", "myValue");
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());

        final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
        assertNotNull(query);
        final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
        assertNotNull(queryPersonalization);
        final List<DecisionScope> scopes = (List<DecisionScope>) queryPersonalization.get("decisionScopes");
        assertNotNull(scopes);
        assertEquals(1, scopes.size());
        assertEquals(testScope, scopes.get(0));

        final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
        assertNotNull(xdm);
        assertEquals(2, xdm.size());
        assertEquals("personalization.request", xdm.get("eventType"));
        assertEquals("myXdmValue", xdm.get("myXdmKey"));

        final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("myValue", data.get("myKey"));

        final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
        assertNull(datasetId);
    }

    @Test
    public void testHandleUpdatePropositions_multipleValidDecisionScopes() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final DecisionScope testScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final DecisionScope testScope2 = new DecisionScope("myMbox");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope1);
                add(testScope2);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();


        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());

        final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
        assertNotNull(query);
        final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
        assertNotNull(queryPersonalization);
        final List<DecisionScope> scopes = (List<DecisionScope>) queryPersonalization.get("decisionScopes");
        assertNotNull(scopes);
        assertEquals(2, scopes.size());
        assertEquals(testScope1, scopes.get(0));
        assertEquals(testScope2, scopes.get(1));

        final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
        assertNotNull(xdm);
        assertEquals(1, xdm.size());
        assertEquals("personalization.request", xdm.get("eventType"));

        final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
        assertNull(data);

        final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
        assertNull(datasetId);
    }

    @Test
    public void testHandleUpdatePropositions_configurationNotAvailable() throws Exception {
        // setup
        final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testHandleUpdatePropositions_noDecisionScopes() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>());

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(2));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testHandleUpdatePropositions_invalidDecisionScope() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoiIn0=");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(3));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testHandleUpdatePropositions_validAndInvalidDecisionScopes() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final DecisionScope testScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9");
        final DecisionScope testScope2 = new DecisionScope("myMbox");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "updatepropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope1);
                add(testScope2);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();


        // test
        extension.handleUpdatePropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());

        final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
        assertNotNull(query);
        final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
        assertNotNull(queryPersonalization);
        final List<DecisionScope> scopes = (List<DecisionScope>) queryPersonalization.get("decisionScopes");
        assertNotNull(scopes);
        assertEquals(1, scopes.size());
        assertEquals(testScope2, scopes.get(0));

        final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
        assertNotNull(xdm);
        assertEquals(1, xdm.size());
        assertEquals("personalization.request", xdm.get("eventType"));

        final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
        assertNull(data);

        final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
        assertNull(datasetId);
    }

    @Test
    public void testHandleEdgeResponse_validProposition() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_VALID.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.notification".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionsData = propositionsList.get(0);
        assertNotNull(propositionsData);
        final Proposition proposition = Proposition.fromEventData(propositionsData);
        assertNotNull(proposition);

        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        assertTrue(proposition.getScopeDetails().isEmpty());
        assertEquals(1, proposition.getOffers().size());

        final Offer offer = proposition.getOffers().get(0);
        assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        assertEquals("10", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        assertEquals(OfferType.HTML, offer.getType());
        assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("testing"));
        assertNull(offer.getLanguage());

        final Map<DecisionScope, Proposition> cachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertEquals(1, cachedPropositions.size());
        final DecisionScope cachedScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        assertEquals(proposition, cachedPropositions.get(cachedScope));
    }

    @Test
    public void testHandleEdgeResponse_validPropositionFromTargetWithClickTracking() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_VALID_TARGET_WITH_CLICK_TRACKING.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.notification".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionsData = propositionsList.get(0);
        assertNotNull(propositionsData);
        final Proposition proposition = Proposition.fromEventData(propositionsData);
        assertNotNull(proposition);

        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9", proposition.getId());
        assertEquals("myMbox", proposition.getScope());
        assertNotNull(proposition.getScopeDetails());

        final Map<String, Object> scopeDetails = proposition.getScopeDetails();
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> activity = (Map<String, Object>)scopeDetails.get("activity");
        assertNotNull(activity);
        assertEquals(1, activity.size());
        assertEquals("111111", activity.get("id"));
        Map<String, Object> experience = (Map<String, Object>)scopeDetails.get("experience");
        assertNotNull(experience);
        assertEquals(1, experience.size());
        assertEquals("0", experience.get("id"));
        final List<Map<String, Object>> strategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(strategies);
        assertEquals(1, strategies.size());
        final Map<String, Object> strategy = strategies.get(0);
        assertNotNull(strategy);
        assertEquals(2, strategy.size());
        assertEquals("0", strategy.get("algorithmID"));
        assertEquals("0", strategy.get("trafficType"));

        assertEquals(1, proposition.getOffers().size());
        final Offer offer = proposition.getOffers().get(0);
        assertEquals("0", offer.getId());
        assertNull(offer.getEtag());
        assertEquals("https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        assertEquals(OfferType.JSON, offer.getType());
        assertEquals("{\"device\":\"mobile\"}", offer.getContent());
        assertNull(offer.getCharacteristics());
        assertNull(offer.getLanguage());

        final Map<DecisionScope, Proposition> cachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertEquals(1, cachedPropositions.size());
        final DecisionScope cachedScope = new DecisionScope("myMbox");
        assertEquals(proposition, cachedPropositions.get(cachedScope));
    }

    @Test
    public void testHandleEdgeResponse_emptyProposition() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_EMPTY_PAYLOAD.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        final Map<DecisionScope, Proposition> cachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertTrue(cachedPropositions.isEmpty());
    }

    @Test
    public void testHandleEdgeResponse_unsupportedItemInProposition() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_UNSUPPORTED_ITEM_IN_PAYLOAD.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(2));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        final Map<DecisionScope, Proposition> cachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertTrue(cachedPropositions.isEmpty());
    }

    @Test
    public void testHandleEdgeResponse_missingEventHandleInData() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_MISSING_EVENT_HANDLE.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        final Map<DecisionScope, Proposition> cachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertTrue(cachedPropositions.isEmpty());
    }

    @Test
    public void testHandleEdgeErrorResponse() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });
        
        final Map<String, Object> edgeErrorResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_ERROR_RESPONSE.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                .setEventData(edgeErrorResponseData)
                .build();

        // test
        extension.handleEdgeErrorResponse(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
        final Map<DecisionScope, Proposition> cachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertTrue(cachedPropositions.isEmpty());
    }

    @Test
    public void testHandleGetPropositions_decisionScopeInCache() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final DecisionScope testScope = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "getpropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> triggerEventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleGetPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(eventCaptor.capture(), triggerEventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event triggerEvent = triggerEventCaptor.getValue();
        assertEquals(testEvent, triggerEvent);

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.responseContent".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionData = propositionsList.get(0);
        assertNotNull(propositionData);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        assertNotNull(proposition);

        assertEquals("de03ac85-802a-4331-a905-a57053164d35", proposition.getId());
        assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        assertTrue(proposition.getScopeDetails().isEmpty());
        assertEquals(1, proposition.getOffers().size());

        Offer offer = proposition.getOffers().get(0);
        assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        assertEquals("10", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        assertEquals(OfferType.HTML, offer.getType());
        assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
        assertNull(offer.getLanguage());
        assertNull(offer.getCharacteristics());
    }

    @Test
    public void testHandleGetPropositions_notAllDecisionScopesInCache() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final DecisionScope testScope1 = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final DecisionScope testScope2 = new DecisionScope("myMbox");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "getpropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope1);
                add(testScope2);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> triggerEventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleGetPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(eventCaptor.capture(), triggerEventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event triggerEvent = triggerEventCaptor.getValue();
        assertEquals(testEvent, triggerEvent);

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.responseContent".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionData = propositionsList.get(0);
        assertNotNull(propositionData);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        assertNotNull(proposition);

        assertEquals("de03ac85-802a-4331-a905-a57053164d35", proposition.getId());
        assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        assertTrue(proposition.getScopeDetails().isEmpty());
        assertEquals(1, proposition.getOffers().size());

        Offer offer = proposition.getOffers().get(0);
        assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        assertEquals("10", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        assertEquals(OfferType.HTML, offer.getType());
        assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
        assertNull(offer.getLanguage());
        assertNull(offer.getCharacteristics());
    }

    @Test
    public void testHandleGetPropositions_noDecisionScopeInCache() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final DecisionScope testScope1 = new DecisionScope("myMbox1");
        final DecisionScope testScope2 = new DecisionScope("myMbox2");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "getpropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope1);
                add(testScope2);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> triggerEventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleGetPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(eventCaptor.capture(), triggerEventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event triggerEvent = triggerEventCaptor.getValue();
        assertEquals(testEvent, triggerEvent);

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.responseContent".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(0, propositionsList.size());
    }

    @Test
    public void testHandleGetPropositions_missingDecisionScopesList() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "getpropositions");

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> triggerEventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleGetPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(eventCaptor.capture(), triggerEventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event triggerEvent = triggerEventCaptor.getValue();
        assertEquals(testEvent, triggerEvent);

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.responseContent".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNull(propositionsList);

        final AdobeError error = (AdobeError) dispatchedEvent.getEventData().get("responseerror");
        assertNotNull(error);
        assertEquals(AdobeError.UNEXPECTED_ERROR, error);
    }

    @Test
    public void testHandleGetPropositions_emptyCachedPropositions() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final DecisionScope testScope = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final Map<String, Object> testEventData = new HashMap<String, Object>();
        testEventData.put("requesttype", "getpropositions");
        testEventData.put("decisionscopes", new ArrayList<DecisionScope>() {
            {
                add(testScope);
            }
        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> triggerEventCaptor = ArgumentCaptor.forClass(Event.class);

        final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(testEventData)
                .build();

        // test
        extension.handleGetPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(eventCaptor.capture(), triggerEventCaptor.capture(), any(ExtensionErrorCallback.class));

        final Event triggerEvent = triggerEventCaptor.getValue();
        assertEquals(testEvent, triggerEvent);

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.responseContent".toLowerCase(), dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(0, propositionsList.size());
    }

    @Test
    public void testHandleClearPropositions() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestReset")
                .build();

        // test
        extension.handleClearPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        final Map<DecisionScope, Proposition> actualCachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertTrue(actualCachedPropositions.isEmpty());
    }

    @Test
    public void testHandleClearPropositions_coreResetIdentities() throws Exception {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        Whitebox.setInternalState(extension, "cachedPropositions", cachedPropositions);

        final Event testEvent = new Event.Builder("Reset Identities Request", "com.adobe.eventType.generic.identity", "com.adobe.eventSource.requestReset")
                .build();

        // test
        extension.handleClearPropositions(testEvent);

        // verify
        testExecutor.awaitTermination(1, TimeUnit.SECONDS);
        final Map<DecisionScope, Proposition> actualCachedPropositions = Whitebox.getInternalState(extension, "cachedPropositions");
        assertTrue(actualCachedPropositions.isEmpty());
    }

    // Helper methods
    private void setConfigurationSharedState(final Map<String, Object> data) {
        when(mockExtensionApi.getSharedEventState(eq("com.adobe.module.configuration"), any(Event.class), any(ExtensionErrorCallback.class)))
                .thenReturn(data);
    }
}

