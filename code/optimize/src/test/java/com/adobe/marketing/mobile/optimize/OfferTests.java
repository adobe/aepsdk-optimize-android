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
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class OfferTests {

    double doubleAccuracy = 0.001;

    @Test
    public void testBuilder_validOffer() {
        final Offer offer =
                new Offer.Builder(
                                "xcore:personalized-offer:2222222222222222",
                                OfferType.TEXT,
                                "This is a plain text content!")
                        .setEtag("7")
                        .setScore(2)
                        .setSchema(
                                "https://ns.adobe.com/experience/offer-management/content-component-text")
                        .setLanguage(
                                new ArrayList<String>() {
                                    {
                                        add("en-us");
                                    }
                                })
                        .setCharacteristics(
                                new HashMap<String, String>() {
                                    {
                                        put("mobile", "true");
                                    }
                                })
                        .build();

        Assert.assertEquals("xcore:personalized-offer:2222222222222222", offer.getId());
        Assert.assertEquals("7", offer.getEtag());
        Assert.assertEquals(2, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-text",
                offer.getSchema());
        Assert.assertEquals(OfferType.TEXT, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("This is a plain text content!", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validJsonOffer() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_JSON.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("8", offer.getEtag());
        Assert.assertEquals(0, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-json",
                offer.getSchema());
        Assert.assertEquals(OfferType.JSON, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("{\"testing\":\"ho-ho\"}", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validTextOffer() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_TEXT.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:2222222222222222", offer.getId());
        Assert.assertEquals("7", offer.getEtag());
        Assert.assertEquals(0, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-text",
                offer.getSchema());
        Assert.assertEquals(OfferType.TEXT, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("This is a plain text content!", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validHtmlOffer() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_HTML.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:3333333333333333", offer.getId());
        Assert.assertEquals("8", offer.getEtag());
        Assert.assertEquals(0, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-html",
                offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("<h1>Hello, Welcome!</h1>", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validImageOffer() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_IMAGE.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:4444444444444444", offer.getId());
        Assert.assertEquals("8", offer.getEtag());
        Assert.assertEquals(0, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-imagelink",
                offer.getSchema());
        Assert.assertEquals(OfferType.IMAGE, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("https://example.com/avatar1.png?alt=media", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validOfferWithScore() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_WITH_SCORE.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:2222222222222222", offer.getId());
        Assert.assertEquals("7", offer.getEtag());
        Assert.assertEquals(1, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-text",
                offer.getSchema());
        Assert.assertEquals(OfferType.TEXT, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("This is a plain text content!", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_withDoubleScore() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_WITH_DOUBLE_SCORE.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:2222222222222222", offer.getId());
        Assert.assertEquals("7", offer.getEtag());

        // validate that the score is of type double and has the correct value
        Object offerScore = offer.getScore();
        Assert.assertTrue(offerScore instanceof Double);
        Assert.assertEquals(6.43, offer.getScore(), doubleAccuracy);

        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-text",
                offer.getSchema());
        Assert.assertEquals(OfferType.TEXT, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals("This is a plain text content!", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_validJsonOfferFromTarget() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_JSON_TARGET.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("222429", offer.getId());
        Assert.assertNull(offer.getEtag());
        Assert.assertEquals(
                "https://ns.adobe.com/personalization/json-content-item", offer.getSchema());

        Map<String, Object> metadata = offer.getMeta();
        Assert.assertNotNull(metadata);
        Assert.assertEquals(3, metadata.size());
        Assert.assertEquals("Demo AB Activity", (String) metadata.get("activity.name"));
        Assert.assertEquals("Experience A", (String) metadata.get("experience.name"));
        Assert.assertEquals(
                "67706174319866856517739865618220416768",
                (String) metadata.get("profile.marketingCloudVisitorId"));

        Assert.assertEquals(OfferType.JSON, offer.getType());
        Assert.assertEquals("{\"testing\":\"ho-ho\"}", offer.getContent());
        Assert.assertNull(offer.getLanguage());
        Assert.assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_validHtmlOfferFromTarget() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_HTML_TARGET.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("222428", offer.getId());
        Assert.assertNull(offer.getEtag());
        Assert.assertEquals(
                "https://ns.adobe.com/personalization/html-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>Hello, Welcome!</h1>", offer.getContent());
        Assert.assertNull(offer.getLanguage());
        Assert.assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_defaultContentOfferFromTarget() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource(
                                                "json/OFFER_VALID_DEFAULT_CONTENT_TARGET.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("222429", offer.getId());
        Assert.assertNull(offer.getEtag());
        Assert.assertEquals(
                "https://ns.adobe.com/personalization/default-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.UNKNOWN, offer.getType());
        Assert.assertEquals("", offer.getContent());
        Assert.assertNull(offer.getLanguage());
        Assert.assertNull(offer.getCharacteristics());
    }

    @Test
    public void testFromEventData_validArrayOffer() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_VALID_LIST.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNotNull(offer);

        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("8", offer.getEtag());
        Assert.assertEquals(0, offer.getScore(), doubleAccuracy);
        Assert.assertEquals(
                "https://ns.adobe.com/experience/offer-management/content-component-json",
                offer.getSchema());
        Assert.assertEquals(OfferType.JSON, offer.getType());
        Assert.assertEquals(1, offer.getLanguage().size());
        Assert.assertEquals("en-us", offer.getLanguage().get(0));
        Assert.assertEquals(
                "[{\"cars\":[\"Ford\",\"BMW\",\"Fiat\"],\"name\":\"John\",\"age\":30}]",
                offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("mobile"));
    }

    @Test
    public void testFromEventData_emptyOffer() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_EMPTY.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoId() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_MISSING_ID.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoContent() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_MISSING_CONTENT.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferInvalidContent() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_INVALID_CONTENT.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferNoItemData() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_MISSING_ITEM_DATA.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferIdMismatch() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_ID_MISMATCH.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_invalidOfferFormatTypeIsNotString() throws Exception {
        Map<String, Object> offerData =
                new ObjectMapper()
                        .readValue(
                                getClass()
                                        .getClassLoader()
                                        .getResource("json/OFFER_INVALID_FORMAT_TYPE.json"),
                                HashMap.class);
        final Offer offer = Offer.fromEventData(offerData);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_nullData() {
        final Offer offer = Offer.fromEventData(null);
        Assert.assertNull(offer);
    }

    @Test
    public void testFromEventData_emptyData() {
        final Offer offer = Offer.fromEventData(new HashMap<String, Object>());
        Assert.assertNull(offer);
    }

    @Test
    public void testGenerateDisplayInteractionXdm_validProposition() throws Exception {
        // setup
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
        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        // verify
        Assert.assertNotNull(propositionInteractionXdm);
        Assert.assertEquals(
                "decisioning.propositionDisplay", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience =
                (Map<String, Object>) propositionInteractionXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList =
                (List<Map<String, Object>>) decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap =
                propositionInteractionDetailsList.get(0);
        Assert.assertEquals(
                "de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals(
                "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails =
                (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertFalse(scopeDetails.isEmpty());
        Assert.assertNotNull(scopeDetails.get("activity"));
        Assert.assertNotNull(scopeDetails.get("placement"));
        final List<Map<String, Object>> items =
                (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testGenerateDisplayInteractionXdm_validPropositionFromTarget() throws Exception {
        // setup
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
        Assert.assertNotNull(optimizeProposition.getOffers());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        // verify
        Assert.assertNotNull(propositionInteractionXdm);
        Assert.assertEquals(
                "decisioning.propositionDisplay", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience =
                (Map<String, Object>) propositionInteractionXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList =
                (List<Map<String, Object>>) decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap =
                propositionInteractionDetailsList.get(0);
        Assert.assertEquals(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails =
                (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(4, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>) scopeDetails.get("activity");
        Assert.assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience =
                (Map<String, Object>) scopeDetails.get("experience");
        Assert.assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies =
                (List<Map<String, Object>>) scopeDetails.get("strategies");
        Assert.assertNotNull(sdStrategies);
        Assert.assertEquals(1, sdStrategies.size());
        Assert.assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        Assert.assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items =
                (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testGenerateDisplayInteractionXdm_nullPropositionReference() throws Exception {
        // setup
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
        Assert.assertNotNull(optimizeProposition.getOffers());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertNotNull(offer);
        offer.propositionReference = null;

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        // verify
        Assert.assertNull(propositionInteractionXdm);
    }

    @Test
    public void testGenerateTapInteractionXdm_validProposition() throws Exception {
        // setup
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
        Assert.assertNotNull(optimizeProposition.getOffers());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateTapInteractionXdm();

        // verify
        Assert.assertNotNull(propositionInteractionXdm);
        Assert.assertEquals(
                "decisioning.propositionInteract", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience =
                (Map<String, Object>) propositionInteractionXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList =
                (List<Map<String, Object>>) decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap =
                propositionInteractionDetailsList.get(0);
        Assert.assertEquals(
                "de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals(
                "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails =
                (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertFalse(scopeDetails.isEmpty());
        Assert.assertNotNull(scopeDetails.get("activity"));
        Assert.assertNotNull(scopeDetails.get("placement"));
        final List<Map<String, Object>> items =
                (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testGenerateTapInteractionXdm_validPropositionFromTarget() throws Exception {
        // setup
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
        Assert.assertNotNull(optimizeProposition.getOffers());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertNotNull(offer);

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateTapInteractionXdm();

        // verify
        Assert.assertNotNull(propositionInteractionXdm);
        Assert.assertEquals(
                "decisioning.propositionInteract", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience =
                (Map<String, Object>) propositionInteractionXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList =
                (List<Map<String, Object>>) decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap =
                propositionInteractionDetailsList.get(0);
        Assert.assertEquals(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails =
                (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertEquals(4, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>) scopeDetails.get("activity");
        Assert.assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience =
                (Map<String, Object>) scopeDetails.get("experience");
        Assert.assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies =
                (List<Map<String, Object>>) scopeDetails.get("strategies");
        Assert.assertNotNull(sdStrategies);
        Assert.assertEquals(1, sdStrategies.size());
        Assert.assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        Assert.assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items =
                (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testGenerateTapInteractionXdm_nullPropositionReference() throws Exception {
        // setup
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
        Assert.assertNotNull(optimizeProposition.getOffers());
        Assert.assertEquals(1, optimizeProposition.getOffers().size());
        Offer offer = optimizeProposition.getOffers().get(0);
        Assert.assertNotNull(offer);
        offer.propositionReference = null;

        // test
        final Map<String, Object> propositionInteractionXdm = offer.generateTapInteractionXdm();

        // verify
        Assert.assertNull(propositionInteractionXdm);
    }

    @Test
    public void testDisplayed_validProposition() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // setup
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

            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            offer.displayed();

            // verify
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals(
                    "com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
            final Map<String, Object> eventData = dispatchedEvent.getEventData();
            Assert.assertNotNull(eventData);
            Assert.assertEquals("trackpropositions", eventData.get("requesttype"));
            final Map<String, Object> propositionInteractions =
                    (Map<String, Object>) eventData.get("propositioninteractions");
            Assert.assertNotNull(propositionInteractions);
            Assert.assertEquals(
                    "decisioning.propositionDisplay", propositionInteractions.get("eventType"));
            final Map<String, Object> experience =
                    (Map<String, Object>) propositionInteractions.get("_experience");
            Assert.assertNotNull(experience);
            final Map<String, Object> decisioning =
                    (Map<String, Object>) experience.get("decisioning");
            Assert.assertNotNull(decisioning);
            final List<Map<String, Object>> propositionInteractionDetailsList =
                    (List<Map<String, Object>>) decisioning.get("propositions");
            Assert.assertNotNull(propositionInteractionDetailsList);
            Assert.assertEquals(1, propositionInteractionDetailsList.size());
            final Map<String, Object> propositionInteractionDetailsMap =
                    propositionInteractionDetailsList.get(0);
            Assert.assertEquals(
                    "de03ac85-802a-4331-a905-a57053164d35",
                    propositionInteractionDetailsMap.get("id"));
            Assert.assertEquals(
                    "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    propositionInteractionDetailsMap.get("scope"));
            final Map<String, Object> scopeDetails =
                    (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
            Assert.assertNotNull(scopeDetails);
            Assert.assertFalse(scopeDetails.isEmpty());
            Assert.assertNotNull(scopeDetails.get("activity"));
            Assert.assertNotNull(scopeDetails.get("placement"));
            final List<Map<String, Object>> items =
                    (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size());
            Assert.assertEquals(
                    "xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
        }
    }

    @Test
    public void testDisplayed_validPropositionFromTarget() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // setup
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
            Assert.assertNotNull(optimizeProposition.getOffers());
            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            offer.displayed();

            // verify
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals(
                    "com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
            final Map<String, Object> eventData = dispatchedEvent.getEventData();
            Assert.assertNotNull(eventData);
            Assert.assertEquals("trackpropositions", eventData.get("requesttype"));
            final Map<String, Object> propositionInteractions =
                    (Map<String, Object>) eventData.get("propositioninteractions");
            Assert.assertNotNull(propositionInteractions);
            Assert.assertEquals(
                    "decisioning.propositionDisplay", propositionInteractions.get("eventType"));
            final Map<String, Object> experience =
                    (Map<String, Object>) propositionInteractions.get("_experience");
            Assert.assertNotNull(experience);
            final Map<String, Object> decisioning =
                    (Map<String, Object>) experience.get("decisioning");
            Assert.assertNotNull(decisioning);
            final List<Map<String, Object>> propositionInteractionDetailsList =
                    (List<Map<String, Object>>) decisioning.get("propositions");
            Assert.assertNotNull(propositionInteractionDetailsList);
            Assert.assertEquals(1, propositionInteractionDetailsList.size());
            final Map<String, Object> propositionInteractionDetailsMap =
                    propositionInteractionDetailsList.get(0);
            Assert.assertEquals(
                    "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                    propositionInteractionDetailsMap.get("id"));
            Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
            final Map<String, Object> scopeDetails =
                    (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
            Assert.assertNotNull(scopeDetails);
            Assert.assertEquals(4, scopeDetails.size());
            Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
            final Map<String, Object> sdActivity =
                    (Map<String, Object>) scopeDetails.get("activity");
            Assert.assertEquals("125589", sdActivity.get("id"));
            final Map<String, Object> sdExperience =
                    (Map<String, Object>) scopeDetails.get("experience");
            Assert.assertEquals("0", sdExperience.get("id"));
            final List<Map<String, Object>> sdStrategies =
                    (List<Map<String, Object>>) scopeDetails.get("strategies");
            Assert.assertNotNull(sdStrategies);
            Assert.assertEquals(1, sdStrategies.size());
            Assert.assertEquals("0", sdStrategies.get(0).get("algorithmID"));
            Assert.assertEquals("0", sdStrategies.get(0).get("trafficType"));
            final List<Map<String, Object>> items =
                    (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size());
            Assert.assertEquals("246315", items.get(0).get("id"));
        }
    }

    @Test
    public void testDisplayed_nullPropositionReference() throws Exception {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
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
            Assert.assertNotNull(optimizeProposition.getOffers());
            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);
            offer.propositionReference = null;

            // test
            offer.displayed();

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.debug(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }

    @Test
    public void testTapped_validProposition() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // setup
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
            Assert.assertNotNull(optimizeProposition.getOffers());
            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            offer.tapped();

            // verify
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals(
                    "com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
            final Map<String, Object> eventData = dispatchedEvent.getEventData();
            Assert.assertNotNull(eventData);
            Assert.assertEquals("trackpropositions", eventData.get("requesttype"));
            final Map<String, Object> propositionInteractions =
                    (Map<String, Object>) eventData.get("propositioninteractions");
            Assert.assertNotNull(propositionInteractions);
            Assert.assertEquals(
                    "decisioning.propositionInteract", propositionInteractions.get("eventType"));
            final Map<String, Object> experience =
                    (Map<String, Object>) propositionInteractions.get("_experience");
            Assert.assertNotNull(experience);
            final Map<String, Object> decisioning =
                    (Map<String, Object>) experience.get("decisioning");
            Assert.assertNotNull(decisioning);
            final List<Map<String, Object>> propositionInteractionDetailsList =
                    (List<Map<String, Object>>) decisioning.get("propositions");
            Assert.assertNotNull(propositionInteractionDetailsList);
            Assert.assertEquals(1, propositionInteractionDetailsList.size());
            final Map<String, Object> propositionInteractionDetailsMap =
                    propositionInteractionDetailsList.get(0);
            Assert.assertEquals(
                    "de03ac85-802a-4331-a905-a57053164d35",
                    propositionInteractionDetailsMap.get("id"));
            Assert.assertEquals(
                    "eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                    propositionInteractionDetailsMap.get("scope"));
            final Map<String, Object> scopeDetails =
                    (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
            Assert.assertNotNull(scopeDetails);
            Assert.assertFalse(scopeDetails.isEmpty());
            Assert.assertNotNull(scopeDetails.get("activity"));
            Assert.assertNotNull(scopeDetails.get("placement"));
            final List<Map<String, Object>> items =
                    (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size());
            Assert.assertEquals(
                    "xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
        }
    }

    @Test
    public void testTapped_validPropositionFromTarget() throws Exception {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                Mockito.mockStatic(MobileCore.class)) {
            // setup
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
            Assert.assertNotNull(optimizeProposition.getOffers());
            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            offer.tapped();

            // verify
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event dispatchedEvent = eventCaptor.getValue();
            Assert.assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            Assert.assertEquals(
                    "com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
            final Map<String, Object> eventData = dispatchedEvent.getEventData();
            Assert.assertNotNull(eventData);
            Assert.assertEquals("trackpropositions", eventData.get("requesttype"));
            final Map<String, Object> propositionInteractions =
                    (Map<String, Object>) eventData.get("propositioninteractions");
            Assert.assertNotNull(propositionInteractions);
            Assert.assertEquals(
                    "decisioning.propositionInteract", propositionInteractions.get("eventType"));
            final Map<String, Object> experience =
                    (Map<String, Object>) propositionInteractions.get("_experience");
            Assert.assertNotNull(experience);
            final Map<String, Object> decisioning =
                    (Map<String, Object>) experience.get("decisioning");
            Assert.assertNotNull(decisioning);
            final List<Map<String, Object>> propositionInteractionDetailsList =
                    (List<Map<String, Object>>) decisioning.get("propositions");
            Assert.assertNotNull(propositionInteractionDetailsList);
            Assert.assertEquals(1, propositionInteractionDetailsList.size());
            final Map<String, Object> propositionInteractionDetailsMap =
                    propositionInteractionDetailsList.get(0);
            Assert.assertEquals(
                    "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                    propositionInteractionDetailsMap.get("id"));
            Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
            final Map<String, Object> scopeDetails =
                    (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
            Assert.assertNotNull(scopeDetails);
            Assert.assertEquals(4, scopeDetails.size());
            Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
            final Map<String, Object> sdActivity =
                    (Map<String, Object>) scopeDetails.get("activity");
            Assert.assertEquals("125589", sdActivity.get("id"));
            final Map<String, Object> sdExperience =
                    (Map<String, Object>) scopeDetails.get("experience");
            Assert.assertEquals("0", sdExperience.get("id"));
            final List<Map<String, Object>> sdStrategies =
                    (List<Map<String, Object>>) scopeDetails.get("strategies");
            Assert.assertNotNull(sdStrategies);
            Assert.assertEquals(1, sdStrategies.size());
            Assert.assertEquals("0", sdStrategies.get(0).get("algorithmID"));
            Assert.assertEquals("0", sdStrategies.get(0).get("trafficType"));
            final List<Map<String, Object>> items =
                    (List<Map<String, Object>>) propositionInteractionDetailsMap.get("items");
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size());
            Assert.assertEquals("246315", items.get(0).get("id"));
        }
    }

    @Test
    public void testTapped_nullPropositionReference() throws Exception {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
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
            Assert.assertNotNull(optimizeProposition.getOffers());
            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);
            offer.propositionReference = null;

            // test
            offer.tapped();

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.debug(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
        }
    }
}
