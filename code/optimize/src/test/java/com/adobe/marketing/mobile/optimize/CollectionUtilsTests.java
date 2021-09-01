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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class CollectionUtilsTests {
    @Test
    public void testIsNullOrEmpty_nullMap() {
        // test
        assertTrue(CollectionUtils.isNullOrEmpty((Map<String, Object>)null));
    }

    @Test
    public void testIsNullOrEmpty_emptyMap() {
        // test
        assertTrue(CollectionUtils.isNullOrEmpty(new HashMap<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyMap() {
        // test
        final Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertFalse(CollectionUtils.isNullOrEmpty(map));
    }

    @Test
    public void testIsNullOrEmpty_nullList() {
        // test
        assertTrue(CollectionUtils.isNullOrEmpty((List<Object>)null));
    }

    @Test
    public void testIsNullOrEmpty_emptyList() {
        // test
        assertTrue(CollectionUtils.isNullOrEmpty(new ArrayList<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyList() {
        // test
        final List<Object> list = new ArrayList<>();
        list.add("someString");

        assertFalse(CollectionUtils.isNullOrEmpty(list));
    }
}
