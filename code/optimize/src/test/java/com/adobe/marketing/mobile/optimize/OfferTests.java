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
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class})
@SuppressWarnings("unchecked")
public class OfferTests {
    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
    }

    @Test
    public void testBuilder_validOffer() throws Exception {
        final Offer offer = new Offer.Builder("xcore:personalized-offer:2222222222222222", OfferType.TEXT, "This is a plain text content!")
                    .setEtag("7")
                    .setSchema("https://ns.adobe.com/experience/offer-management/content-component-text")
                    .setLanguage(new ArrayList<String>() {
                        {
                            add("en-us");
                        }
                    })
                    .setCharacteristics(new HashMap<String, String>() {
                        {
                            put("mobile", "true");
                        }
                    })
                    .build();

        assertEquals("xcore:personalized-offer:2222222222222222", offer.getId());
        assertEquals("7", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-text", offer.getSchema());
        assertEquals(OfferType.TEXT, offer.getType());
        assertEquals(1, offer.getLanguage().size());
        assertEquals("en-us", offer.getLanguage().get(0));
        assertEquals("This is a plain text content!", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validJsonOffer() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_VALID_JSON.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);

        assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        assertEquals("8", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-json", offer.getSchema());
        assertEquals(OfferType.JSON, offer.getType());
        assertEquals(1, offer.getLanguage().size());
        assertEquals("en-us", offer.getLanguage().get(0));
        assertEquals("{\"testing\":\"ho-ho\"}", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validTextOffer() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_VALID_TEXT.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);

        assertEquals("xcore:personalized-offer:2222222222222222", offer.getId());
        assertEquals("7", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-text", offer.getSchema());
        assertEquals(OfferType.TEXT, offer.getType());
        assertEquals(1, offer.getLanguage().size());
        assertEquals("en-us", offer.getLanguage().get(0));
        assertEquals("This is a plain text content!", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validHtmlOffer() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_VALID_HTML.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);

        assertEquals("xcore:personalized-offer:3333333333333333", offer.getId());
        assertEquals("8", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        assertEquals(OfferType.HTML, offer.getType());
        assertEquals(1, offer.getLanguage().size());
        assertEquals("en-us", offer.getLanguage().get(0));
        assertEquals("<h1>Hello, Welcome!</h1>", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validImageOffer() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_VALID_IMAGE.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);

        assertEquals("xcore:personalized-offer:4444444444444444", offer.getId());
        assertEquals("8", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-imagelink", offer.getSchema());
        assertEquals(OfferType.IMAGE, offer.getType());
        assertEquals(1, offer.getLanguage().size());
        assertEquals("en-us", offer.getLanguage().get(0));
        assertEquals("https://example.com/avatar1.png?alt=media", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validJsonOfferFromTarget() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_VALID_JSON_TARGET.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);

        assertEquals("222429", offer.getId());
        assertNull(offer.getEtag());
        assertEquals("https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        assertEquals(OfferType.JSON, offer.getType());
        assertEquals("{\"testing\":\"ho-ho\"}", offer.getContent());
        assertNull(offer.getLanguage());
        assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_validHtmlOfferFromTarget() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_VALID_HTML_TARGET.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);

        assertEquals("222428", offer.getId());
        assertNull(offer.getEtag());
        assertEquals("https://ns.adobe.com/personalization/html-content-item", offer.getSchema());
        assertEquals(OfferType.HTML, offer.getType());
        assertEquals("<h1>Hello, Welcome!</h1>", offer.getContent());
        assertNull(offer.getLanguage());
        assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_emptyOffer() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_INVALID_EMPTY.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoId() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_INVALID_MISSING_ID.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoContent() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_INVALID_MISSING_CONTENT.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoFormat() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_INVALID_MISSING_FORMAT.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoItemData() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_INVALID_MISSING_ITEM_DATA.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferIdMismatch() throws Exception {
        Map<String, Object> offerData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/OFFER_INVALID_ID_MISMATCH.json"), HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_nullData() throws Exception {
        final Offer offer = Offer.fromEventData(null);
        assertNull(offer);
    }

    @Test
    public void testFromEventData_emptyData() throws Exception {
        final Offer offer = Offer.fromEventData(new HashMap<String, Object>());
        assertNull(offer);
    }

    @Test
    public void testGenerateDisplayInteractionXdm_validProposition() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        // verify
        assertNotNull(propositionInteractionXdm);
        assertEquals("decisioning.propositionDisplay", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testGenerateDisplayInteractionXdm_validPropositionFromTarget() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        // verify
        assertNotNull(propositionInteractionXdm);
        assertEquals("decisioning.propositionDisplay", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(sdStrategies);
        assertEquals(1, sdStrategies.size());
        assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testGenerateDisplayInteractionXdm_nullPropositionReference() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);
        offer.propositionReference = null;

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        // verify
        assertNull(propositionInteractionXdm);
    }

    @Test
    public void testGenerateTapInteractionXdm_validProposition() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateTapInteractionXdm();

        // verify
        assertNotNull(propositionInteractionXdm);
        assertEquals("decisioning.propositionInteract", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testGenerateTapInteractionXdm_validPropositionFromTarget() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateTapInteractionXdm();

        // verify
        assertNotNull(propositionInteractionXdm);
        assertEquals("decisioning.propositionInteract", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(sdStrategies);
        assertEquals(1, sdStrategies.size());
        assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testGenerateTapInteractionXdm_nullPropositionReference() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);
        offer.propositionReference = null;

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateTapInteractionXdm();

        // verify
        assertNull(propositionInteractionXdm);
    }

    @Test
    public void testDisplayed_validProposition() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        offer.displayed();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        assertEquals("trackpropositions", eventData.get("requesttype"));
        final Map<String, Object> propositionInteractions = (Map<String, Object>)eventData.get("propositioninteractions");
        assertNotNull(propositionInteractions);
        assertEquals("decisioning.propositionDisplay", propositionInteractions.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractions.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testDisplayed_validPropositionFromTarget() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        offer.displayed();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        assertEquals("trackpropositions", eventData.get("requesttype"));
        final Map<String, Object> propositionInteractions = (Map<String, Object>)eventData.get("propositioninteractions");
        assertNotNull(propositionInteractions);
        assertEquals("decisioning.propositionDisplay", propositionInteractions.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractions.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(sdStrategies);
        assertEquals(1, sdStrategies.size());
        assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testDisplayed_nullPropositionReference() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);
        offer.propositionReference = null;

        // test
        offer.displayed();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }

    @Test
    public void testTapped_validProposition() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        offer.tapped();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        assertEquals("trackpropositions", eventData.get("requesttype"));
        final Map<String, Object> propositionInteractions = (Map<String, Object>)eventData.get("propositioninteractions");
        assertNotNull(propositionInteractions);
        assertEquals("decisioning.propositionInteract", propositionInteractions.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractions.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testTapped_validPropositionFromTarget() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID_TARGET.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        offer.tapped();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize".toLowerCase(), dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent".toLowerCase(), dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        assertEquals("trackpropositions", eventData.get("requesttype"));
        final Map<String, Object> propositionInteractions = (Map<String, Object>)eventData.get("propositioninteractions");
        assertNotNull(propositionInteractions);
        assertEquals("decisioning.propositionInteract", propositionInteractions.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractions.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = (Map<String, Object>)propositionInteractionDetailsList.get(0);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(sdStrategies);
        assertEquals(1, sdStrategies.size());
        assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testTapped_nullPropositionReference() throws Exception {
        // setup
        Map<String, Object> propositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition proposition = Proposition.fromEventData(propositionData);

        assertEquals(1, proposition.getOffers().size());
        Offer offer = proposition.getOffers().get(0);
        assertNotNull(offer);
        offer.propositionReference = null;

        // test
        offer.tapped();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.log(any(LoggingMode.class), anyString(), anyString());
    }
}
