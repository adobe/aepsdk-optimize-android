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

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class OptimizeExtensionTests {
    private OptimizeExtension extension;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;

    @Before
    public void setup() {
        extension = new OptimizeExtension(mockExtensionApi);
    }

    @Test
    public void test_getName() {
        // test
        final String extensionName = extension.getName();
        Assert.assertEquals("getName should return the correct extension name.", "com.adobe.optimize", extensionName);
    }

    @Test
    public void test_getVersion() {
        // test
        final String extensionVersion = extension.getVersion();
        Assert.assertEquals("getVersion should return the correct extension version.", "2.0.1", extensionVersion);
    }

    @Test
    public void test_registration() {
        // setup
        Mockito.clearInvocations(mockExtensionApi);

        // test
        extension = new OptimizeExtension(mockExtensionApi);
        extension.onRegistered();

        // verify
        Mockito.verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                ArgumentMatchers.eq("com.adobe.eventType.optimize"),
                ArgumentMatchers.eq("com.adobe.eventSource.requestContent"),
                ArgumentMatchers.any(ExtensionEventListener.class));
        Mockito.verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                ArgumentMatchers.eq("com.adobe.eventType.edge"),
                ArgumentMatchers.eq("personalization:decisions"),
                ArgumentMatchers.any(ExtensionEventListener.class));
        Mockito.verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                ArgumentMatchers.eq("com.adobe.eventType.edge"),
                ArgumentMatchers.eq("com.adobe.eventSource.errorResponseContent"),
                ArgumentMatchers.any(ExtensionEventListener.class));
        Mockito.verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                ArgumentMatchers.eq("com.adobe.eventType.optimize"),
                ArgumentMatchers.eq("com.adobe.eventSource.requestReset"),
                ArgumentMatchers.any(ExtensionEventListener.class));
        Mockito.verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                ArgumentMatchers.eq("com.adobe.eventType.generic.identity"),
                ArgumentMatchers.eq("com.adobe.eventSource.requestReset"),
                ArgumentMatchers.any(ExtensionEventListener.class));
    }

    @Test
    public void testReadyForEvent_configurationSet() {
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .build();

        Assert.assertTrue(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_configurationPending() {
        // setup
        setConfigurationSharedState(SharedStateStatus.PENDING, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .build();

        Assert.assertFalse(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_configurationPendingNoData() {
        // setup
        setConfigurationSharedState(SharedStateStatus.PENDING, null);

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .build();

        Assert.assertFalse(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_configurationNotSet() {
        // setup
        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .build();

        Assert.assertFalse(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_OptimizeResetContentEvent() {
        // setup
        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestReset")
                .build();

        Assert.assertTrue(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_EdgeEvent() {
        // setup
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .build();

        Assert.assertTrue(extension.readyForEvent(testEvent));
    }

    @Test
    public void testHandleOptimizeRequestContent_nullEventData() {
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(null)
                .build();


        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        // verify
        Mockito.verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_emptyEventData() {
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(new HashMap<>())
                .build();


        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        Mockito.verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_invalidRequestType() {
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        Map<String, Object> eventData = new HashMap<String, Object>() {
            {
                put("requesttype", "unknown");
            }
        };
        Event testEvent = new Event.Builder("Optimize Get Propositions Request",
                "com.adobe.eventType.optimize",
                "com.adobe.eventSource.requestContent")
                .setEventData(eventData)
                .build();

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        Mockito.verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_handleUpdatePropositions_validDecisionScope() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();


            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            Assert.assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            Assert.assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            Assert.assertNotNull(schemas);
            Assert.assertEquals(7, schemas.size());
            Assert.assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            Assert.assertNotNull(scopes);
            Assert.assertEquals(1, scopes.size());
            Assert.assertEquals(testScope.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            Assert.assertNotNull(xdm);
            Assert.assertEquals(1, xdm.size());
            Assert.assertEquals("personalization.request", xdm.get("eventType"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            Assert.assertNull(data);

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            Assert.assertNull(datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_validDecisionScopeWithXdmAndDataAndDatasetId() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                    put("optimize.datasetId", "111111111111111111111111");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            testEventData.put("xdm", new HashMap<String, Object>() {
                {
                    put("myXdmKey", "myXdmValue");
                }
            });
            testEventData.put("data", new HashMap<String, Object>() {
                {
                    put("myKey", "myValue");
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            Assert.assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            Assert.assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            Assert.assertNotNull(schemas);
            Assert.assertEquals(7, schemas.size());
            Assert.assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            Assert.assertNotNull(scopes);
            Assert.assertEquals(1, scopes.size());
            Assert.assertEquals(testScope.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            Assert.assertNotNull(xdm);
            Assert.assertEquals(2, xdm.size());
            Assert.assertEquals("personalization.request", xdm.get("eventType"));
            Assert.assertEquals("myXdmValue", xdm.get("myXdmKey"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            Assert.assertNotNull(data);
            Assert.assertEquals(1, data.size());
            Assert.assertEquals("myValue", data.get("myKey"));

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            Assert.assertEquals("111111111111111111111111", datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_validDecisionScopeWithXdmAndDataAndNoDatasetId() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            testEventData.put("xdm", new HashMap<String, Object>() {
                {
                    put("myXdmKey", "myXdmValue");
                }
            });
            testEventData.put("data", new HashMap<String, Object>() {
                {
                    put("myKey", "myValue");
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            Assert.assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            Assert.assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            Assert.assertNotNull(schemas);
            Assert.assertEquals(7, schemas.size());
            Assert.assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            Assert.assertNotNull(scopes);
            Assert.assertEquals(1, scopes.size());
            Assert.assertEquals(testScope.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            Assert.assertNotNull(xdm);
            Assert.assertEquals(2, xdm.size());
            Assert.assertEquals("personalization.request", xdm.get("eventType"));
            Assert.assertEquals("myXdmValue", xdm.get("myXdmKey"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            Assert.assertNotNull(data);
            Assert.assertEquals(1, data.size());
            Assert.assertEquals("myValue", data.get("myKey"));

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            Assert.assertNull(datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_multipleValidDecisionScopes() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final DecisionScope testScope2 = new DecisionScope("myMbox");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();


            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            Assert.assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            Assert.assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            Assert.assertNotNull(schemas);
            Assert.assertEquals(7, schemas.size());
            Assert.assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            Assert.assertNotNull(scopes);
            Assert.assertEquals(2, scopes.size());
            Assert.assertEquals(testScope1.getName(), scopes.get(0));
            Assert.assertEquals(testScope2.getName(), scopes.get(1));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            Assert.assertNotNull(xdm);
            Assert.assertEquals(1, xdm.size());
            Assert.assertEquals("personalization.request", xdm.get("eventType"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            Assert.assertNull(data);

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            Assert.assertNull(datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_UpdatePropositions_configurationNotAvailable() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {

            // setup
            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(0)).dispatch(ArgumentMatchers.any());
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_noDecisionScopes() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>());

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(0)).dispatch(ArgumentMatchers.any());
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()), Mockito.times(2));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_invalidDecisionScope() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoiIn0=");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(0)).dispatch(ArgumentMatchers.any());
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()), Mockito.times(2));
            logMockedStatic.verify(() -> Log.warning(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_validAndInvalidDecisionScopes() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9");
            final DecisionScope testScope2 = new DecisionScope("myMbox");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();


            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            Assert.assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            Assert.assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            Assert.assertNotNull(schemas);
            Assert.assertEquals(7, schemas.size());
            Assert.assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            Assert.assertNotNull(scopes);
            Assert.assertEquals(1, scopes.size());
            Assert.assertEquals(testScope2.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            Assert.assertNotNull(xdm);
            Assert.assertEquals(1, xdm.size());
            Assert.assertEquals("personalization.request", xdm.get("eventType"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            Assert.assertNull(data);

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            Assert.assertNull(datasetId);
        }
    }

    @Test
    public void testHandleEdgeResponse_validProposition() throws Exception{
        // setup
        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_VALID.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
        Assert.assertEquals("com.adobe.eventSource.notification", dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        Assert.assertNotNull(propositionsList);
        Assert.assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionsData = propositionsList.get(0);
        Assert.assertNotNull(propositionsData);
        final Proposition proposition = Proposition.fromEventData(propositionsData);
        Assert.assertNotNull(proposition);

        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        Assert.assertTrue(proposition.getScopeDetails().isEmpty());
        Assert.assertEquals(1, proposition.getOffers().size());

        final Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("10", offer.getEtag());
        Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("testing"));
        Assert.assertNull(offer.getLanguage());

        final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
        Assert.assertNotNull(cachedPropositions);
        Assert.assertEquals(1, cachedPropositions.size());
        final DecisionScope cachedScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        Assert.assertEquals(proposition, cachedPropositions.get(cachedScope));
    }

    @Test
    public void testHandleEdgeResponse_validPropositionFromTargetWithClickTracking() throws Exception {
        // setup
        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_VALID_TARGET_WITH_CLICK_TRACKING.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
        Assert.assertEquals("com.adobe.eventSource.notification", dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        Assert.assertNotNull(propositionsList);
        Assert.assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionsData = propositionsList.get(0);
        Assert.assertNotNull(propositionsData);
        final Proposition proposition = Proposition.fromEventData(propositionsData);
        Assert.assertNotNull(proposition);

        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9", proposition.getId());
        Assert.assertEquals("myMbox", proposition.getScope());
        Assert.assertNotNull(proposition.getScopeDetails());

        final Map<String, Object> scopeDetails = proposition.getScopeDetails();
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(5, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> activity = (Map<String, Object>)scopeDetails.get("activity");
        Assert.assertNotNull(activity);
        Assert.assertEquals(1, activity.size());
        Assert.assertEquals("111111", activity.get("id"));
        Map<String, Object> experience = (Map<String, Object>)scopeDetails.get("experience");
        Assert.assertNotNull(experience);
        Assert.assertEquals(1, experience.size());
        Assert.assertEquals("0", experience.get("id"));
        final List<Map<String, Object>> strategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        Assert.assertNotNull(strategies);
        Assert.assertEquals(2, strategies.size());
        final Map<String, Object> strategy0 = strategies.get(0);
        Assert.assertNotNull(strategy0);
        Assert.assertEquals(3, strategy0.size());
        Assert.assertEquals("entry", strategy0.get("step"));
        Assert.assertEquals("0", strategy0.get("algorithmID"));
        Assert.assertEquals("0", strategy0.get("trafficType"));

        final Map<String, Object> strategy1 = strategies.get(1);
        Assert.assertNotNull(strategy1);
        Assert.assertEquals(3, strategy1.size());
        Assert.assertEquals("display", strategy1.get("step"));
        Assert.assertEquals("0", strategy1.get("algorithmID"));
        Assert.assertEquals("0", strategy1.get("trafficType"));

        final Map<String, Object> characteristics = (Map<String, Object>)scopeDetails.get("characteristics");
        Assert.assertNotNull(characteristics);
        Assert.assertEquals(2, characteristics.size());
        Assert.assertEquals("SGFZpwAqaqFTayhAT2xsgzG3+2fw4m+O9FK8c0QoOHfxVkH1ttT1PGBX3/jV8a5uFF0fAox6CXpjJ1PGRVQBjHl9Zc6mRxY9NQeM7rs/3Es1RHPkzBzyhpVS6eg9q+kw", characteristics.get("stateToken"));
        final Map<String, Object> eventTokens = (Map<String, Object>)characteristics.get("eventTokens");
        Assert.assertNotNull(eventTokens);
        Assert.assertEquals(2, eventTokens.size());
        Assert.assertEquals("MmvRrL5aB4Jz36JappRYg2qipfsIHvVzTQxHolz2IpSCnQ9Y9OaLL2gsdrWQTvE54PwSz67rmXWmSnkXpSSS2Q==", eventTokens.get("display"));
        Assert.assertEquals("EZDMbI2wmAyGcUYLr3VpmA==", eventTokens.get("click"));

        Assert.assertEquals(1, proposition.getOffers().size());
        final Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("0", offer.getId());
        Assert.assertNull(offer.getEtag());
        Assert.assertEquals("https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.JSON, offer.getType());
        Assert.assertEquals("{\"device\":\"mobile\"}", offer.getContent());
        Assert.assertNull(offer.getCharacteristics());
        Assert.assertNull(offer.getLanguage());

        final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
        Assert.assertNotNull(cachedPropositions);
        Assert.assertEquals(1, cachedPropositions.size());
        final DecisionScope cachedScope = new DecisionScope("myMbox");
        Assert.assertEquals(proposition, cachedPropositions.get(cachedScope));
    }

    @Test
    public void testHandleEdgeResponse_emptyProposition() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_EMPTY_PAYLOAD.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(edgeResponseData)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_unsupportedItemInProposition() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_UNSUPPORTED_ITEM_IN_PAYLOAD.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(edgeResponseData)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()), Mockito.times(2));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_missingEventHandleInData() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_MISSING_EVENT_HANDLE.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(edgeResponseData)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_nullEventData(){
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(null)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_emptyEventData() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(new HashMap<>())
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeErrorResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_ERROR_RESPONSE.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                    .setEventData(edgeErrorResponseData)
                    .build();

            // test
            extension.handleEdgeErrorResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.warning(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse_nullEventData() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                    .setEventData(null)
                    .build();

            // test
            extension.handleEdgeErrorResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse_emptyEventData(){
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                    .setEventData(new HashMap<>())
                    .build();

            // test
            extension.handleEdgeErrorResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            Assert.assertNotNull(cachedPropositions);
            Assert.assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_decisionScopeInCache() throws Exception{
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));
            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            Assert.assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            Assert.assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            Assert.assertNotNull(propositionsList);
            Assert.assertEquals(1, propositionsList.size());

            final Map<String, Object> propositionData = propositionsList.get(0);
            Assert.assertNotNull(propositionData);
            final Proposition proposition = Proposition.fromEventData(propositionData);
            Assert.assertNotNull(proposition);

            Assert.assertEquals("de03ac85-802a-4331-a905-a57053164d35", proposition.getId());
            Assert.assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
            Assert.assertTrue(proposition.getScopeDetails().isEmpty());
            Assert.assertEquals(1, proposition.getOffers().size());

            Offer offer = proposition.getOffers().get(0);
            Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
            Assert.assertEquals("10", offer.getEtag());
            Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
            Assert.assertEquals(OfferType.HTML, offer.getType());
            Assert.assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
            Assert.assertNull(offer.getLanguage());
            Assert.assertNull(offer.getCharacteristics());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_notAllDecisionScopesInCache() throws Exception{
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));
            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            Assert.assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope1 = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final DecisionScope testScope2 = new DecisionScope("myMbox");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            Assert.assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            Assert.assertNotNull(propositionsList);
            Assert.assertEquals(1, propositionsList.size());

            final Map<String, Object> propositionData = propositionsList.get(0);
            Assert.assertNotNull(propositionData);
            final Proposition proposition = Proposition.fromEventData(propositionData);
            Assert.assertNotNull(proposition);

            Assert.assertEquals("de03ac85-802a-4331-a905-a57053164d35", proposition.getId());
            Assert.assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
            Assert.assertTrue(proposition.getScopeDetails().isEmpty());
            Assert.assertEquals(1, proposition.getOffers().size());

            Offer offer = proposition.getOffers().get(0);
            Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
            Assert.assertEquals("10", offer.getEtag());
            Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
            Assert.assertEquals(OfferType.HTML, offer.getType());
            Assert.assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
            Assert.assertNull(offer.getLanguage());
            Assert.assertNull(offer.getCharacteristics());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_noDecisionScopeInCache() throws Exception {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            Assert.assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope1 = new DecisionScope("myMbox1");
            final DecisionScope testScope2 = new DecisionScope("myMbox2");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            Assert.assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            Assert.assertNotNull(propositionsList);
            Assert.assertEquals(0, propositionsList.size());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_missingDecisionScopesList() throws Exception {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            Assert.assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            Assert.assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            Assert.assertNull(propositionsList);

            Assert.assertEquals(0, dispatchedEvent.getEventData().get("responseerror"));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_emptyCachedPropositions(){
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            Assert.assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            Assert.assertNotNull(propositionsList);
            Assert.assertEquals(0, propositionsList.size());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_validPropositionInteractionsForDisplay() throws Exception{
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_DISPLAY.json"), HashMap.class);
        final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(optimizeTrackRequestData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
        Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        Assert.assertNotNull(eventData);
        final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>)eventData.get("xdm");
        Assert.assertNotNull(propositionInteractionsXdm);
        Assert.assertEquals("decisioning.propositionDisplay", propositionInteractionsXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionsXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_validPropositionInteractionsForTap() throws Exception{
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_TAP.json"), HashMap.class);
        final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(optimizeTrackRequestData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
        Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        Assert.assertNotNull(eventData);
        final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>)eventData.get("xdm");
        Assert.assertNotNull(propositionInteractionsXdm);
        Assert.assertEquals("decisioning.propositionInteract", propositionInteractionsXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionsXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(4, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        Assert.assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        Assert.assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        Assert.assertNotNull(sdStrategies);
        Assert.assertEquals(1, sdStrategies.size());
        Assert.assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        Assert.assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_validPropositionInteractionsWithDatasetIdInConfig() throws Exception{
        // setup
        setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                put("optimize.datasetId", "111111111111111111111111");
            }
        });

        final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_TAP.json"), HashMap.class);
        final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(optimizeTrackRequestData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        Mockito.verify(mockExtensionApi, Mockito.times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        Assert.assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
        Assert.assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        Assert.assertNotNull(eventData);
        final String datasetId = (String)eventData.get("datasetId");
        Assert.assertEquals("111111111111111111111111", datasetId);
        final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>)eventData.get("xdm");
        Assert.assertNotNull(propositionInteractionsXdm);
        Assert.assertEquals("decisioning.propositionInteract", propositionInteractionsXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionsXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(4, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        Assert.assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        Assert.assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        Assert.assertNotNull(sdStrategies);
        Assert.assertEquals(1, sdStrategies.size());
        Assert.assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        Assert.assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_configurationNotAvailable() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_DISPLAY.json"), HashMap.class);
            final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(optimizeTrackRequestData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(0)).dispatch(ArgumentMatchers.any());

            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_missingPropositionInteractions() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_MISSING_PROPOSITION_INTERACTIONS.json"), HashMap.class);
            final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(optimizeTrackRequestData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(0)).dispatch(ArgumentMatchers.any());

            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_emptyPropositionInteractions() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            setConfigurationSharedState(SharedStateStatus.SET, new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_EMPTY_PROPOSITION_INTERACTIONS.json"), HashMap.class);
            final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(optimizeTrackRequestData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            Mockito.verify(mockExtensionApi, Mockito.times(0)).dispatch(ArgumentMatchers.any());

            logMockedStatic.verify(() -> Log.debug(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()));
        }
    }

    @Test
    public void testHandleClearPropositions() throws Exception{
        // setup
        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        Assert.assertNotNull(testProposition);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        extension.setCachedPropositions(cachedPropositions);

        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestReset")
                .build();

        // test
        extension.handleClearPropositions(testEvent);

        // verify
        final Map<DecisionScope, Proposition> actualCachedPropositions = extension.getCachedPropositions();
        Assert.assertTrue(actualCachedPropositions.isEmpty());
    }

    @Test
    public void testHandleClearPropositions_coreResetIdentities() throws Exception{
        // setup
        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        Assert.assertNotNull(testProposition);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        extension.setCachedPropositions(cachedPropositions);

        final Event testEvent = new Event.Builder("Reset Identities Request", "com.adobe.eventType.generic.identity", "com.adobe.eventSource.requestReset")
                .build();

        // test
        extension.handleClearPropositions(testEvent);

        // verify
        final Map<DecisionScope, Proposition> actualCachedPropositions = extension.getCachedPropositions();
        Assert.assertTrue(actualCachedPropositions.isEmpty());
    }


    // Helper methods
    private void setConfigurationSharedState(final SharedStateStatus status, final Map<String, Object> data) {
        Mockito.when(mockExtensionApi.getSharedState(
                ArgumentMatchers.eq(OptimizeConstants.Configuration.EXTENSION_NAME),
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(false),
                ArgumentMatchers.eq(SharedStateResolution.ANY)
        )).thenReturn(new SharedStateResult(status, data));
    }
}

