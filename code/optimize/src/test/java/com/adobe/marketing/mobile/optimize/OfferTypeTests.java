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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
public class OfferTypeTests {
    @Test
    public void testOfferType_fromJson() {
        // test
        final OfferType offerType = OfferType.from("application/json");
        assertEquals(OfferType.JSON, offerType);
    }

    @Test
    public void testOfferType_fromText() {
        // test
        final OfferType offerType = OfferType.from("text/plain");
        assertEquals(OfferType.TEXT, offerType);
    }

    @Test
    public void testOfferType_fromHtml() {
        // test
        final OfferType offerType = OfferType.from("text/html");
        assertEquals(OfferType.HTML, offerType);
    }

    @Test
    public void testOfferType_fromImage() {
        // test
        final OfferType offerType = OfferType.from("image/png");
        assertEquals(OfferType.IMAGE, offerType);
    }

    @Test
    public void testOfferType_fromUnknownString() {
        // test
        final OfferType offerType = OfferType.from("*/*");
        assertEquals(OfferType.UNKNOWN, offerType);
    }

    @Test
    public void testOfferType_fromEmptyString() {
        // test
        final OfferType offerType = OfferType.from("");
        assertEquals(OfferType.UNKNOWN, offerType);
    }

    @Test
    public void testOfferType_fromNullString() {
        // test
        final OfferType offerType = OfferType.from(null);
        assertEquals(OfferType.UNKNOWN, offerType);
    }

    @Test
    public void testOfferType_unknownFromValues() {
        // test
        assertEquals(OfferType.UNKNOWN, OfferType.values()[0]);
    }

    @Test
    public void testOfferType_jsonFromValues() {
        // test
        assertEquals(OfferType.JSON, OfferType.values()[1]);
    }

    @Test
    public void testOfferType_textFromValues() {
        // test
        assertEquals(OfferType.TEXT, OfferType.values()[2]);
    }

    @Test
    public void testOfferType_htmlFromValues() {
        // test
        assertEquals(OfferType.HTML, OfferType.values()[3]);
    }

    @Test
    public void testOfferType_imageFromValues() {
        // test
        assertEquals(OfferType.IMAGE, OfferType.values()[4]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOfferType_invalidFromValues() {
        // test
        assertEquals(OfferType.UNKNOWN, OfferType.values()[100]);
    }
}
