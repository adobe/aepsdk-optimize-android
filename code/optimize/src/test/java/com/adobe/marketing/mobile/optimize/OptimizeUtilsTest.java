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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Base64.class)
public class OptimizeUtilsTest {
    @Before
    public void setup() {
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

    @Test
    public void testIsNullOrEmpty_nullMap() {
        // test
        assertTrue(OptimizeUtils.isNullOrEmpty((Map<String, Object>)null));
    }

    @Test
    public void testIsNullOrEmpty_emptyMap() {
        // test
        assertTrue(OptimizeUtils.isNullOrEmpty(new HashMap<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyMap() {
        // test
        final Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertFalse(OptimizeUtils.isNullOrEmpty(map));
    }

    @Test
    public void testIsNullOrEmpty_nullList() {
        // test
        assertTrue(OptimizeUtils.isNullOrEmpty((List<Object>)null));
    }

    @Test
    public void testIsNullOrEmpty_emptyList() {
        // test
        assertTrue(OptimizeUtils.isNullOrEmpty(new ArrayList<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyList() {
        // test
        final List<Object> list = new ArrayList<>();
        list.add("someString");

        assertFalse(OptimizeUtils.isNullOrEmpty(list));
    }

    @Test
    public void testIsNullOrEmpty_nullString() {
        // test
        final String input = null;
        assertTrue(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testIsNullOrEmpty_emptyString() {
        // test
        final String input = "";
        assertTrue(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyString() {
        // test
        final String input = "This is a test string!";
        assertFalse(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testBase64encode_validString() {
        // test
        final String input = "This is a test string!";
        assertEquals("VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==", OptimizeUtils.base64Encode(input));

    }

    @Test
    public void testBase64encode_emptyString() {
        // test
        final String input = "";
        assertEquals("", OptimizeUtils.base64Encode(input));

    }

    @Test
    public void testBase64decode_validString() {
        // test
        final String input = "VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==";
        assertEquals("This is a test string!", OptimizeUtils.base64Decode(input));

    }

    @Test
    public void testBase64decode_emptyString() {
        // test
        final String input = "";
        assertEquals("", OptimizeUtils.base64Decode(input));

    }

    @Test
    public void testBase64decode_invalidString() {
        // test
        final String input = "VGhp=";
        assertNull(OptimizeUtils.base64Decode(input));
    }
}
