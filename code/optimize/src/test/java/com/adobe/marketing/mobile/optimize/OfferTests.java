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
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unchecked")
public class OfferTests {
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
}
