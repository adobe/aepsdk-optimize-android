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

public class OptimizeTestConstants {

    static final String EXTENSION_VERSION = "3.2.4";
    public static final String LOG_TAG = "OptimizeTest";
    static final String CONFIG_DATA_STORE = "AdobeMobile_ConfigState";

    public static final class EventType {
        public static final String MONITOR = "com.adobe.functional.eventType.monitor";
        public static final String OPTIMIZE = "com.adobe.eventType.optimize";
        public static final String EDGE = "com.adobe.eventType.edge";
        public static final String IDENTITY = "com.adobe.eventType.generic.identity";
    }

    public static final class EventSource {
        public static final String UNREGISTER = "com.adobe.eventSource.unregister";
        public static final String SHARED_STATE_REQUEST =
                "com.adobe.eventSource.sharedStateRequest";
        public static final String XDM_SHARED_STATE_REQUEST =
                "com.adobe.eventSource.xdmSharedStateRequest";
        public static final String XDM_SHARED_STATE_RESPONSE =
                "com.adobe.eventSource.xdmSharedStateResponse";
        public static final String SHARED_STATE_RESPONSE =
                "com.adobe.eventSource.sharedStateResponse";
        public static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        public static final String PERSONALIZATION = "personalization:decisions";
        public static final String NOTIFICATION = "com.adobe.eventSource.notification";
        public static final String EDGE_ERROR_RESPONSE =
                "com.adobe.eventSource.errorResponseContent";
        public static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        public static final String REQUEST_RESET = "com.adobe.eventSource.requestReset";
        public static final String CONTENT_COMPLETE = "com.adobe.eventSource.contentComplete";
    }

    public static final class EventDataKeys {
        public static final String STATE_OWNER = "stateowner";
    }
}
