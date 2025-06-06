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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.util.Base64;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class OptimizeTests {
    private Map<DecisionScope, OptimizeProposition> responseMap;
    private AdobeError responseError;

    @After
    public void teardown() {
        responseMap = null;
        responseError = null;
    }

    @Test
    public void test_extensionVersion() {
        // test
        final String extensionVersion = Optimize.extensionVersion();
        Assert.assertEquals(
                "extensionVersion API should return the correct version string.",
                OptimizeConstants.EXTENSION_VERSION,
                extensionVersion);
    }

    @Test
    public void testUpdatePropositions_validDecisionScope() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

            Optimize.updatePropositions(scopes, null, null);

            // verify

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));

            final Event event = eventCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
            Assert.assertNull(eventData.get("xdm"));
            Assert.assertNull(eventData.get("data"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(1, scopesList.size());

            final Map<String, Object> scopeData = scopesList.get(0);
            Assert.assertNotNull(scopeData);
            Assert.assertEquals(1, scopeData.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData.get("name"));
        }
    }

    @Test
    public void testUpdatePropositionsWithCallback_validDecisionScope() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

            Optimize.updatePropositions(
                    scopes,
                    null,
                    null,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);

            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));

            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("updatepropositions", eventData.get("requesttype"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(1, scopesList.size());

            final Map<String, Object> scopeData = scopesList.get(0);
            Assert.assertNotNull(scopeData);
            Assert.assertEquals(1, scopeData.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData.get("name"));

            // verify callback response
            final Map<String, Object> propositionData =
                    new ObjectMapper()
                            .readValue(
                                    getClass()
                                            .getClassLoader()
                                            .getResource("json/PROPOSITION_VALID_ODE.json"),
                                    HashMap.class);
            final OptimizeProposition optimizeProposition =
                    OptimizeProposition.fromEventData(propositionData);
            Assert.assertNotNull(optimizeProposition);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            propositionsList.add(optimizeProposition.toEventData());

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("propositions", propositionsList);
            final Event responseEvent =
                    new Event.Builder(
                                    "Optimize Response",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.responseContent")
                            .setEventData(responseEventData)
                            .build();
            callbackWithError.call(responseEvent);

            Assert.assertNull(responseError);
        }
    }

    @Test
    public void testUpdatePropositions_validDecisionScopeWithXDMAndData() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

            Optimize.updatePropositions(
                    scopes,
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

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));

            final Event event = eventCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("updatepropositions", eventData.get("requesttype"));

            Map<String, Object> xdm = (Map<String, Object>) eventData.get("xdm");
            Assert.assertNotNull(xdm);
            Assert.assertEquals(1, xdm.size());
            Assert.assertEquals("myXdmValue", xdm.get("myXdmKey"));

            Map<String, Object> data = (Map<String, Object>) eventData.get("data");
            Assert.assertNotNull(data);
            Assert.assertEquals(1, data.size());
            Assert.assertEquals("myValue", data.get("myKey"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(1, scopesList.size());

            final Map<String, Object> scopeData = scopesList.get(0);
            Assert.assertNotNull(scopeData);
            Assert.assertEquals(1, scopeData.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData.get("name"));
        }
    }

    @Test
    public void testUpdatePropositions_multipleValidDecisionScopes() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
            scopes.add(new DecisionScope("myMbox"));

            Optimize.updatePropositions(scopes, null, null);

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));

            final Event event = eventCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
            Assert.assertNull(eventData.get("xdm"));
            Assert.assertNull(eventData.get("data"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(2, scopesList.size());

            final Map<String, Object> scopeData = scopesList.get(0);
            Assert.assertNotNull(scopeData);
            Assert.assertEquals(1, scopeData.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData.get("name"));

            final Map<String, Object> scopeData2 = scopesList.get(1);
            Assert.assertNotNull(scopeData2);
            Assert.assertEquals(1, scopeData2.size());
            Assert.assertEquals("myMbox", scopeData2.get("name"));
        }
    }

    @Test
    public void testUpdatePropositions_emptyDecisionScopesList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Optimize.updatePropositions(new ArrayList<DecisionScope>(), null, null);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testUpdatePropositions_nullDecisionScopesList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Optimize.updatePropositions(null, null, null);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testUpdatePropositions_invalidDecisionScopeInList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9"));

            Optimize.updatePropositions(scopes, null, null);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testGetPropositions_validDecisionScope() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

            Optimize.getPropositions(
                    scopes,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            // verify dispatched event
            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());
            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("getpropositions", eventData.get("requesttype"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(1, scopesList.size());

            final Map<String, Object> scopeData1 = scopesList.get(0);
            Assert.assertNotNull(scopeData1);
            Assert.assertEquals(1, scopeData1.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData1.get("name"));

            // verify callback response
            final Map<String, Object> propositionData =
                    new ObjectMapper()
                            .readValue(
                                    getClass()
                                            .getClassLoader()
                                            .getResource("json/PROPOSITION_VALID_ODE.json"),
                                    HashMap.class);
            final OptimizeProposition optimizeProposition =
                    OptimizeProposition.fromEventData(propositionData);
            Assert.assertNotNull(optimizeProposition);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            propositionsList.add(optimizeProposition.toEventData());

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("propositions", propositionsList);
            final Event responseEvent =
                    new Event.Builder(
                                    "Optimize Response",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.responseContent")
                            .setEventData(responseEventData)
                            .build();
            callbackWithError.call(responseEvent);

            Assert.assertNull(responseError);
            Assert.assertNotNull(responseMap);
            Assert.assertEquals(1, responseMap.size());
            OptimizeProposition actualOptimizeProposition =
                    responseMap.get(
                            new DecisionScope(
                                    "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
            Assert.assertEquals(optimizeProposition, actualOptimizeProposition);
        }
    }

    @Test
    public void testGetPropositions_multipleValidDecisionScopes() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
            scopes.add(new DecisionScope("myMbox"));

            Optimize.getPropositions(
                    scopes,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    ArgumentMatchers.any(AdobeCallbackWithError.class)));
            final Event event = eventCaptor.getValue();
            Assert.assertNotNull(event);

            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());
            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("getpropositions", eventData.get("requesttype"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(2, scopesList.size());

            final Map<String, Object> scopeData1 = scopesList.get(0);
            Assert.assertNotNull(scopeData1);
            Assert.assertEquals(1, scopeData1.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData1.get("name"));

            final Map<String, Object> scopeData2 = scopesList.get(1);
            Assert.assertNotNull(scopeData2);
            Assert.assertEquals(1, scopeData2.size());
            Assert.assertEquals("myMbox", scopeData2.get("name"));
        }
    }

    @Test
    public void testGetPropositions_invalidDecisionScopeInList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9"));

            Optimize.getPropositions(
                    scopes,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
        }
    }

    @Test
    public void testGetPropositions_emptyDecisionScopesList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Optimize.getPropositions(
                    new ArrayList<DecisionScope>(),
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
        }
    }

    @Test
    public void testGetPropositions_nullDecisionScopesList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            Optimize.getPropositions(
                    null,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.warning(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
        }
    }

    @Test
    public void testOnPropositionsUpdate_validProposition() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // test
            Optimize.onPropositionsUpdate(
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(final AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(
                                final Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.registerEventListener(
                                    ArgumentMatchers.eq("com.adobe.eventType.optimize"),
                                    ArgumentMatchers.eq("com.adobe.eventSource.notification"),
                                    callbackCaptor.capture()));
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            final Map<String, Object> propositionData =
                    new ObjectMapper()
                            .readValue(
                                    getClass()
                                            .getClassLoader()
                                            .getResource("json/PROPOSITION_VALID_ODE.json"),
                                    HashMap.class);
            final OptimizeProposition optimizeProposition =
                    OptimizeProposition.fromEventData(propositionData);
            Assert.assertNotNull(optimizeProposition);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            propositionsList.add(optimizeProposition.toEventData());

            final Map<String, Object> eventData = new HashMap<>();
            eventData.put("propositions", propositionsList);
            final Event event =
                    new Event.Builder(
                                    "Optimize Notification",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.notification")
                            .setEventData(eventData)
                            .build();
            callbackWithError.call(event);

            Assert.assertNull(responseError);
            Assert.assertNotNull(responseMap);
            Assert.assertEquals(1, responseMap.size());
            OptimizeProposition actualOptimizeProposition =
                    responseMap.get(
                            new DecisionScope(
                                    "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
            Assert.assertEquals(optimizeProposition, actualOptimizeProposition);
        }
    }

    @Test
    public void testOnPropositionsUpdate_emptyPropositionData() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // test
            Optimize.onPropositionsUpdate(
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(final AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(
                                final Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.registerEventListener(
                                    ArgumentMatchers.eq("com.adobe.eventType.optimize"),
                                    ArgumentMatchers.eq("com.adobe.eventSource.notification"),
                                    callbackCaptor.capture()));
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            propositionsList.add(new HashMap<String, Object>());

            final Map<String, Object> eventData = new HashMap<>();
            eventData.put("propositions", propositionsList);
            final Event event =
                    new Event.Builder(
                                    "Optimize Notification",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.notification")
                            .setEventData(eventData)
                            .build();
            callbackWithError.call(event);

            Assert.assertNull(responseError);
            Assert.assertNull(responseMap);
        }
    }

    @Test
    public void testOnPropositionsUpdate_nullPropositionData() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // test
            Optimize.onPropositionsUpdate(
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(final AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(
                                final Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            // verify
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.registerEventListener(
                                    ArgumentMatchers.eq("com.adobe.eventType.optimize"),
                                    ArgumentMatchers.eq("com.adobe.eventSource.notification"),
                                    callbackCaptor.capture()));
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            propositionsList.add(null);

            final Map<String, Object> eventData = new HashMap<>();
            eventData.put("propositions", propositionsList);
            final Event event =
                    new Event.Builder(
                                    "Optimize Notification",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.notification")
                            .setEventData(eventData)
                            .build();
            callbackWithError.call(event);

            Assert.assertNull(responseError);
            Assert.assertNull(responseMap);
        }
    }

    @Test
    public void test_clearCachedPropositions() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // test
            Optimize.clearCachedPropositions();

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestReset", event.getSource());
            Assert.assertNull(event.getEventData());
        }
    }

    @Test
    public void testUpdatePropositions_timeoutError() {

        double timeoutSeconds = 0.1;
        Map<String, Object> xdm = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(
                new DecisionScope(
                        "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

        // Mock the callback
        AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> callbackMock =
                Mockito.mock(AdobeCallbackWithError.class);

        AdobeCallbackWithOptimizeError<Event> callbackMockEvent =
                Mockito.mock(AdobeCallbackWithOptimizeError.class);

        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {

            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            mobileCoreMockedStatic
                    .when(
                            () ->
                                    MobileCore.dispatchEventWithResponseCallback(
                                            ArgumentMatchers.any(Event.class),
                                            ArgumentMatchers.anyLong(),
                                            ArgumentMatchers.any(AdobeCallbackWithError.class)))
                    .thenAnswer(
                            (Answer<Void>)
                                    invocation -> {
                                        Optimize.failWithOptimizeError(
                                                callbackMockEvent,
                                                AEPOptimizeError.Companion.getTimeoutError());
                                        return null;
                                    });

            Optimize.updatePropositions(scopes, xdm, data, timeoutSeconds, callbackMock);
            ArgumentCaptor<AEPOptimizeError> errorCaptor =
                    ArgumentCaptor.forClass(AEPOptimizeError.class);
            verify(callbackMockEvent, times(1)).fail(errorCaptor.capture());
            Assert.assertEquals(
                    AEPOptimizeError.Companion.getTimeoutError(), errorCaptor.getValue());
        }
    }

    @Test
    public void testGetPropositions_timeoutError() {

        double timeoutSeconds = 0.1;
        final List<DecisionScope> scopes = new ArrayList<>();
        scopes.add(
                new DecisionScope(
                        "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

        // Mock the callback
        AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> callbackMock =
                Mockito.mock(AdobeCallbackWithError.class);

        AdobeCallbackWithOptimizeError<Event> callbackMockEvent =
                Mockito.mock(AdobeCallbackWithOptimizeError.class);

        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {

            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            mobileCoreMockedStatic
                    .when(
                            () ->
                                    MobileCore.dispatchEventWithResponseCallback(
                                            ArgumentMatchers.any(Event.class),
                                            ArgumentMatchers.anyLong(),
                                            ArgumentMatchers.any(AdobeCallbackWithError.class)))
                    .thenAnswer(
                            (Answer<Void>)
                                    invocation -> {
                                        Optimize.failWithOptimizeError(
                                                callbackMockEvent,
                                                AEPOptimizeError.Companion.getTimeoutError());
                                        return null;
                                    });

            Optimize.getPropositions(scopes, timeoutSeconds, callbackMock);
            ArgumentCaptor<AEPOptimizeError> errorCaptor =
                    ArgumentCaptor.forClass(AEPOptimizeError.class);
            verify(callbackMockEvent, times(1)).fail(errorCaptor.capture());
            Assert.assertEquals(
                    AEPOptimizeError.Companion.getTimeoutError(), errorCaptor.getValue());
        }
    }

    @Test
    public void testUpdatePropositionsWithCallback_invalidError() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

            Optimize.updatePropositions(
                    scopes,
                    null,
                    null,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);

            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));

            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("updatepropositions", eventData.get("requesttype"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(1, scopesList.size());

            final Map<String, Object> scopeData = scopesList.get(0);
            Assert.assertNotNull(scopeData);
            Assert.assertEquals(1, scopeData.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData.get("name"));

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put(OptimizeConstants.EventDataKeys.RESPONSE_ERROR, "");
            final Event responseEvent =
                    new Event.Builder(
                                    "Optimize Response",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.responseContent")
                            .setEventData(responseEventData)
                            .build();
            callbackWithError.call(responseEvent);

            Assert.assertNull(responseError);
        }
    }

    @Test
    public void testUpdatePropositionsWithCallback_noPropositions() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            // setup
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));

            // test
            final List<DecisionScope> scopes = new ArrayList<>();
            scopes.add(
                    new DecisionScope(
                            "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));

            Optimize.updatePropositions(
                    scopes,
                    null,
                    null,
                    new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
                        @Override
                        public void fail(AdobeError adobeError) {
                            responseError = adobeError;
                        }

                        @Override
                        public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                            responseMap = propositionsMap;
                        }
                    });

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor =
                    ArgumentCaptor.forClass(AdobeCallbackWithError.class);

            mobileCoreMockedStatic.verify(
                    () ->
                            MobileCore.dispatchEventWithResponseCallback(
                                    eventCaptor.capture(),
                                    ArgumentMatchers.anyLong(),
                                    callbackCaptor.capture()));

            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            Assert.assertNotNull(event);
            Assert.assertEquals("com.adobe.eventType.optimize", event.getType());
            Assert.assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            Assert.assertEquals("updatepropositions", eventData.get("requesttype"));

            final List<Map<String, Object>> scopesList =
                    (List<Map<String, Object>>) eventData.get("decisionscopes");
            Assert.assertEquals(1, scopesList.size());

            final Map<String, Object> scopeData = scopesList.get(0);
            Assert.assertNotNull(scopeData);
            Assert.assertEquals(1, scopeData.size());
            Assert.assertEquals(
                    "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    scopeData.get("name"));

            // verify callback response
            final Map<String, Object> propositionData =
                    new ObjectMapper()
                            .readValue(
                                    getClass()
                                            .getClassLoader()
                                            .getResource("json/PROPOSITION_VALID_ODE.json"),
                                    HashMap.class);
            final OptimizeProposition optimizeProposition =
                    OptimizeProposition.fromEventData(propositionData);
            Assert.assertNotNull(optimizeProposition);

            final List<Map<String, Object>> propositionsList = new ArrayList<>();
            propositionsList.add(optimizeProposition.toEventData());

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("missedKey", propositionsList);
            final Event responseEvent =
                    new Event.Builder(
                                    "Optimize Response",
                                    "com.adobe.eventType.optimize",
                                    "com.adobe.eventSource.responseContent")
                            .setEventData(responseEventData)
                            .build();
            callbackWithError.call(responseEvent);

            Assert.assertNull(responseError);
        }
    }
}
