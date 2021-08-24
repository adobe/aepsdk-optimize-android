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

import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;

import static com.adobe.marketing.mobile.optimize.OptimizeConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.optimize.OptimizeConstants.EXTENSION_VERSION;

class OptimizeExtension extends Extension {
    /**
     * Constructor for {@code OptimizeExtension}.
     * <p>
     * It is invoked during the extension registration to retrieve the extension's details such as name and version.
     *
     * @param extensionApi {@link ExtensionApi} instance.
     */
    protected OptimizeExtension(ExtensionApi extensionApi) {
        super(extensionApi);
    }

    /**
     * Retrieve the extension name.
     *
     * @return {@link String} containing the unique name for this extension.
     */
    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Retrieve the extension version.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }
}