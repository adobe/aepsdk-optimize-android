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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings({"unchecked"})
public class OptimizePropositionTests {
    @Test
    public void testFromEventData_validProposition() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_VALID_ODE.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNotNull(optimizeProposition);

        Assert.assertEquals("de03ac85-802a-4331-a905-a57053164d35", optimizeProposition.getId());
        Assert.assertEquals(
                "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                optimizeProposition.getScope());
        Assert.assertTrue(optimizeProposition.getScopeDetails().isEmpty());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());

        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("10", offer.getEtag());
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-html",
                offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
        Assert.assertNull(offer.getLanguage());
        Assert.assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_validPropositionFromTarget() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_VALID_TARGET.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNotNull(optimizeProposition);

        Assert.assertEquals(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                optimizeProposition.getId());
        Assert.assertEquals("myMbox", optimizeProposition.getScope());

        Map<String, Object> scopeDetails = optimizeProposition.getScopeDetails();
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(4, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        Map<String, Object> activity = (Map<String, Object>) scopeDetails.get("activity");
        Assert.assertNotNull(activity);
        Assert.assertEquals(1, activity.size());
        Assert.assertEquals("125589", activity.get("id"));
        Map<String, Object> experience = (Map<String, Object>) scopeDetails.get("experience");
        Assert.assertNotNull(experience);
        Assert.assertEquals(1, experience.size());
        Assert.assertEquals("0", experience.get("id"));
        List<Map<String, Object>> strategies =
                (List<Map<String, Object>>) scopeDetails.get("strategies");
        Assert.assertNotNull(strategies);
        Assert.assertEquals(1, strategies.size());
        Map<String, Object> strategy = strategies.get(0);
        Assert.assertNotNull(strategy);
        Assert.assertEquals(2, strategy.size());
        Assert.assertEquals("0", strategy.get("algorithmID"));
        Assert.assertEquals("0", strategy.get("trafficType"));

        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertEquals("246315", offer.getId());
        Assert.assertNull(offer.getEtag());
        Assert.assertEquals(
                "https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.JSON, offer.getType());
        Assert.assertEquals("{\"testing\":\"ho-ho\"}", offer.getContent());
        Assert.assertNull(offer.getLanguage());
        Assert.assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_invalidPropositionNoId() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_INVALID_MISSING_ID.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNull(optimizeProposition);
    }

    @Test
    public void testFromEventData_invalidPropositionNoScope() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_INVALID_MISSING_SCOPE.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNull(optimizeProposition);
    }

    @Test
    public void testFromEventData_invalidPropositionEmptyItems() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_INVALID_EMPTY_ITEMS.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        final List<Offer> offers = optimizeProposition.getOffers();
        Assert.assertNotNull(optimizeProposition);
        Assert.assertNotNull(offers);
        Assert.assertTrue(offers.isEmpty());
    }

    @Test
    public void testFromEventData_invalidPropositionInvalidItem() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_INVALID_ITEM.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNull(optimizeProposition);
    }

    @Test
    public void testFromEventData_validPropositionWithActivityAndPlacement() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource(
                                                "json/PROPOSITION_VALID_WITH_ACTIVITY_PLACEMENT.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNotNull(optimizeProposition);

        Assert.assertEquals("test-id", optimizeProposition.getId());
        Assert.assertEquals("test-scope", optimizeProposition.getScope());
        Assert.assertTrue(optimizeProposition.getScopeDetails().isEmpty());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());

        Map<String, Object> activity = optimizeProposition.getActivity();
        Assert.assertNotNull(activity);
        Assert.assertEquals("activity-id", activity.get("id"));

        Map<String, Object> placement = optimizeProposition.getPlacement();
        Assert.assertNotNull(placement);
        Assert.assertEquals("placement-id", placement.get("id"));
    }

    @Test
    public void testFromEventData_validPropositionWithScopeDetails() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource(
                                                "json/PROPOSITION_VALID_WITH_SCOPE_DETAILS.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNotNull(optimizeProposition);

        Assert.assertEquals("test-id", optimizeProposition.getId());
        Assert.assertEquals("test-scope", optimizeProposition.getScope());
        Assert.assertNotNull(optimizeProposition.getScopeDetails());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());

        Map<String, Object> scopeDetails = optimizeProposition.getScopeDetails();
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(
                "activity-id", ((Map<String, Object>) scopeDetails.get("activity")).get("id"));
        Assert.assertEquals(
                "placement-id", ((Map<String, Object>) scopeDetails.get("placement")).get("id"));
    }

    @Test
    public void testGenerateReferenceXdm_validProposition() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_VALID_ODE.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNotNull(optimizeProposition);

        // test
        final Map<String, Object> propositionReferenceXdm =
                optimizeProposition.generateReferenceXdm();

        // verify
        Assert.assertNotNull(propositionReferenceXdm);
        Assert.assertNull(propositionReferenceXdm.get("eventType"));
        final Map<String, Object> experience =
                (Map<String, Object>) propositionReferenceXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        Assert.assertEquals(
                "de03ac85-802a-4331-a905-a57053164d35", decisioning.get("propositionID"));
    }

    @Test
    public void testGenerateReferenceXdm_validPropositionFromTarget() throws Exception {
        Map<String, Object> propositionData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/PROPOSITION_VALID_TARGET.json"),
                                HashMap.class);
        final OptimizeProposition optimizeProposition =
                OptimizeProposition.fromEventData(propositionData);
        Assert.assertNotNull(optimizeProposition);

        // test
        final Map<String, Object> propositionReferenceXdm =
                optimizeProposition.generateReferenceXdm();

        // verify
        Assert.assertNotNull(propositionReferenceXdm);
        Assert.assertNull(propositionReferenceXdm.get("eventType"));
        final Map<String, Object> experience =
                (Map<String, Object>) propositionReferenceXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        Assert.assertEquals(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                decisioning.get("propositionID"));
    }

    @Test
    public void testGenerateReferenceXdm_withActivityAndPlacement() throws Exception {
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "activity-id");
        Map<String, Object> placement = new HashMap<>();
        placement.put("id", "placement-id");

        OptimizeProposition proposition =
                new OptimizeProposition(
                        "test-id",
                        Collections.emptyList(),
                        "test-scope",
                        null,
                        activity,
                        placement);

        Map<String, Object> xdm = proposition.generateReferenceXdm();
        Assert.assertNotNull(xdm);
        Assert.assertNull(xdm.get("eventType"));

        Map<String, Object> experience = (Map<String, Object>) xdm.get("_experience");
        Assert.assertNotNull(experience);
        Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        Assert.assertEquals("test-id", decisioning.get("propositionID"));
    }

    @Test
    public void testGenerateReferenceXdm_withScopeDetails() throws Exception {
        Map<String, Object> scopeDetails = new HashMap<>();
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "activity-id");
        Map<String, Object> placement = new HashMap<>();
        placement.put("id", "placement-id");
        scopeDetails.put("activity", activity);
        scopeDetails.put("placement", placement);

        OptimizeProposition proposition =
                new OptimizeProposition(
                        "test-id", Collections.emptyList(), "test-scope", scopeDetails, null, null);

        Map<String, Object> xdm = proposition.generateReferenceXdm();
        Assert.assertNotNull(xdm);
        Assert.assertNull(xdm.get("eventType"));

        Map<String, Object> experience = (Map<String, Object>) xdm.get("_experience");
        Assert.assertNotNull(experience);
        Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        Assert.assertEquals("test-id", decisioning.get("propositionID"));
    }

    @Test
    public void testEqualsAndHashCode() {
        OptimizeProposition prop1 =
                new OptimizeProposition(
                        "test-id",
                        Collections.emptyList(),
                        "test-scope",
                        null,
                        Collections.emptyMap(),
                        Collections.emptyMap());

        OptimizeProposition prop2 =
                new OptimizeProposition(
                        "test-id",
                        Collections.emptyList(),
                        "test-scope",
                        null,
                        Collections.emptyMap(),
                        Collections.emptyMap());

        OptimizeProposition prop3 =
                new OptimizeProposition(
                        "different-id",
                        Collections.emptyList(),
                        "test-scope",
                        null,
                        Collections.emptyMap(),
                        Collections.emptyMap());

        Assert.assertEquals(prop1, prop2);
        Assert.assertEquals(prop1.hashCode(), prop2.hashCode());
        Assert.assertNotEquals(prop1, prop3);
        Assert.assertNotEquals(prop1.hashCode(), prop3.hashCode());
    }
}
