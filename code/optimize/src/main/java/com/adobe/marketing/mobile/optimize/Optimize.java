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

import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import static com.adobe.marketing.mobile.optimize.OptimizeConstants.LOG_TAG;

/**
 * Public class containing APIs for the Optimize extension.
 */
public class Optimize {
    private Optimize() {}

    /**
     * Returns the version of the {@code Optimize} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    public static String extensionVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile Core.
     * <p>
     * Note: This method should be called only once in your application class.
     */
    public static void registerExtension() {
        MobileCore.registerExtension(OptimizeExtension.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, LOG_TAG,
                        "An error occurred while registering the Optimize extension: " + extensionError.getErrorName());
            }
        });
    }
}