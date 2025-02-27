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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings({"rawtypes"})
public class OptimizeUtilsTest {

    @Test
    public void testIsNullOrEmpty_nullMap() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty((Map<String, Object>) null));
    }

    @Test
    public void testIsNullOrEmpty_emptyMap() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(new HashMap<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyMap() {
        // test
        final Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        Assert.assertFalse(OptimizeUtils.isNullOrEmpty(map));
    }

    @Test
    public void testIsNullOrEmpty_nullList() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty((List<Object>) null));
    }

    @Test
    public void testIsNullOrEmpty_emptyList() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(new ArrayList<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyList() {
        // test
        final List<Object> list = new ArrayList<>();
        list.add("someString");

        Assert.assertFalse(OptimizeUtils.isNullOrEmpty(list));
    }

    @Test
    public void testIsNullOrEmpty_nullString() {
        // test
        final String input = null;
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testIsNullOrEmpty_emptyString() {
        // test
        final String input = "";
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyString() {
        // test
        final String input = "This is a test string!";
        Assert.assertFalse(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testBase64encode_validString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.encodeToString(
                                            ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer)
                                    invocation ->
                                            java.util.Base64.getEncoder()
                                                    .encodeToString(
                                                            (byte[]) invocation.getArguments()[0]));
            // test
            final String input = "This is a test string!";
            Assert.assertEquals(
                    "VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==", OptimizeUtils.base64Encode(input));
        }
    }

    @Test
    public void testBase64encode_emptyString() {
        // test
        final String input = "";
        Assert.assertEquals("", OptimizeUtils.base64Encode(input));
    }

    @Test
    public void testBase64decode_validString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
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
            final String input = "VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==";
            Assert.assertEquals("This is a test string!", OptimizeUtils.base64Decode(input));
        }
    }

    @Test
    public void testBase64decode_emptyString() {
        // test
        final String input = "";
        Assert.assertEquals("", OptimizeUtils.base64Decode(input));
    }

    @Test
    public void testBase64decode_invalidString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
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
            final String input = "VGhp=";
            Assert.assertNull(OptimizeUtils.base64Decode(input));
        }
    }

    @Test
    public void testBase64decode_mboxString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
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
            final String input = "myTargetLocationB";
            Assert.assertNull(OptimizeUtils.base64Decode(input));
        }
    }

    @Test
    public void testConvertToAdobeError_knownErrorCode() {
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, OptimizeUtils.convertToAdobeError(0));
        Assert.assertEquals(AdobeError.CALLBACK_TIMEOUT, OptimizeUtils.convertToAdobeError(1));
        Assert.assertEquals(AdobeError.CALLBACK_NULL, OptimizeUtils.convertToAdobeError(2));
        Assert.assertEquals(
                AdobeError.EXTENSION_NOT_INITIALIZED, OptimizeUtils.convertToAdobeError(11));
    }

    @Test
    public void testConvertToAdobeError_unknownErrorCode() {
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, OptimizeUtils.convertToAdobeError(123));
    }
}
