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

class OptimizeConstants {
    static final String LOG_TAG = "Optimize";
    static final String EXTENSION_VERSION = "1.0.0";
    static final String EXTENSION_NAME = "com.adobe.optimize";

    static final String ACTIVITY_ID = "activityId";
    static final String XDM_ACTIVITY_ID = "xdm:activityId";
    static final String PLACEMENT_ID = "placementId";
    static final String XDM_PLACEMENT_ID = "xdm:placementId";
    static final String ITEM_COUNT = "itemCount";
    static final String XDM_ITEM_COUNT = "xdm:itemCount";


    private OptimizeConstants() {}

    final class JsonKeys {
        static final String PAYLOAD_ID = "id";
        static final String PAYLOAD_SCOPE = "scope";
        static final String PAYLOAD_SCOPEDETAILS = "scopeDetails";
        static final String PAYLOAD_ITEMS = "items";

        static final String PAYLOAD_ITEM_ID = "id";
        static final String PAYLOAD_ITEM_ETAG = "etag";
        static final String PAYLOAD_ITEM_SCHEMA = "schema";
        static final String PAYLOAD_ITEM_DATA = "data";
        static final String PAYLOAD_ITEM_DATA_ID = "id";
        static final String PAYLOAD_ITEM_DATA_CONTENT = "content";
        static final String PAYLOAD_ITEM_DATA_DELIVERYURL = "deliveryURL";
        static final String PAYLOAD_ITEM_DATA_FORMAT = "format";
        static final String PAYLOAD_ITEM_DATA_LANGUAGE = "language";
        static final String PAYLOAD_ITEM_DATA_CHARACTERISTICS = "characteristics";

        private JsonKeys() {}
    }
}