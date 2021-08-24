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

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class OptimizeTests {

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
    }

    @Test
    public void test_extensionVersion() {
        // test
        final String extensionVersion = Optimize.extensionVersion();
        assertEquals("extensionVersion API should return the correct version string.", "1.0.0",
                extensionVersion);
    }

    @Test
    public void test_registerExtension() {
        // test
        Optimize.registerExtension();
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // The Optimize extension should successfully register with Mobile Core
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(ArgumentMatchers.eq(OptimizeExtension.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension error callback should not be null.", extensionErrorCallback);

        // should be able to invoke the callback
        // extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }
}
