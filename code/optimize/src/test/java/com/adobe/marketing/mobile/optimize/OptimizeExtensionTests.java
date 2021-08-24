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

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class})
public class OptimizeExtensionTests {
    private OptimizeExtension extension;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;

    @Mock
    Application mockApplication;

    @Mock
    Context mockContext;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        extension = new OptimizeExtension(mockExtensionApi);
    }

    @Test
    public void test_getName() {
        // test
        final String extensionName = extension.getName();
        assertEquals("getName should return the correct extension name.", "com.adobe.optimize", extensionName);
    }

    @Test
    public void test_getVersion() {
        // test
        final String extensionVersion = extension.getVersion();
        assertEquals("getVersion should return the correct extension version.", "1.0.0", extensionVersion);
    }
}

