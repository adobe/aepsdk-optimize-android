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

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class PropositionTests {
    @Test
    public void testFromEventData_validProposition() throws Exception {
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

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
    public void testFromEventData_validPropositionFromTarget() throws Exception {
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", proposition.getId());
        assertEquals("myMbox", proposition.getScope());

        Map<String, Object> scopeDetails = proposition.getScopeDetails();
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        Map<String, Object> activity = (Map<String, Object>)scopeDetails.get("activity");
        assertNotNull(activity);
        assertEquals(1, activity.size());
        assertEquals("125589", activity.get("id"));
        Map<String, Object> experience = (Map<String, Object>)scopeDetails.get("experience");
        assertNotNull(experience);
        assertEquals(1, experience.size());
        assertEquals("0", experience.get("id"));
        List<Map<String, Object>> strategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(strategies);
        assertEquals(1, strategies.size());
        Map<String, Object> strategy = strategies.get(0);
        assertNotNull(strategy);
        assertEquals(2, strategy.size());
        assertEquals("0", strategy.get("algorithmID"));
        assertEquals("0", strategy.get("trafficType"));


        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertEquals("246315", offer.getId());
        assertNull(offer.getEtag());
        assertEquals("https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        assertEquals(OfferType.JSON, offer.getType());
        assertEquals("{\"testing\":\"ho-ho\"}", offer.getContent());
        assertNull(offer.getLanguage());
        assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_invalidPropositionNoId() throws Exception {
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_INVALID_MISSING_ID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        assertNull(proposition);
    }

    @Test
    public void testFromEventData_invalidPropositionNoScope() throws Exception {
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_INVALID_MISSING_SCOPE.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        assertNull(proposition);
    }

    @Test
    public void testGenerateReferenceXdm_validProposition() throws Exception {
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        assertNotNull(proposition);

        // test
        final Map<String, Object> propositionReferenceXdm = proposition.generateReferenceXdm();

        // verify
        assertNotNull(propositionReferenceXdm);
        assertNull(propositionReferenceXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionReferenceXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        assertEquals("de03ac85-802a-4331-a905-a57053164d35", decisioning.get("propositionID"));
    }

    @Test
    public void testGenerateReferenceXdm_validPropositionFromTarget() throws Exception {
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);
        assertNotNull(proposition);

        // test
        final Map<String, Object> propositionReferenceXdm = proposition.generateReferenceXdm();

        // verify
        assertNotNull(propositionReferenceXdm);
        assertNull(propositionReferenceXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionReferenceXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", decisioning.get("propositionID"));
    }
}
