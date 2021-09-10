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

    static final class EventNames {
        static final String UPDATE_PROPOSITIONS_REQUEST = "Optimize Update Propositions Request";
        static final String GET_PROPOSITIONS_REQUEST = "Optimize Get Propositions Request";
        static final String TRACK_PROPOSITIONS_REQUEST = "Optimize Track Propositions Request";
        static final String CLEAR_PROPOSITIONS_REQUEST = "Optimize Clear Propositions Request";
        static final String EDGE_PERSONALIZATION_REQUEST = "Edge Optimize Personalization Request";
        static final String OPTIMIZE_RESPONSE = "Optimize Response";

        private EventNames() {}
    }

    static final class EventType {
        static final String OPTIMIZE = "com.adobe.eventType.optimize";
        static final String EDGE = "com.adobe.eventType.edge";

        private EventType() {}
    }


    static final class EventSource {
        static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        static final String REQUEST_RESET = "com.adobe.eventSource.requestReset";
        static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        static final String NOTIFICATION = "com.adobe.eventSource.notification";

        private EventSource() {}
    }

    static final class EventDataKeys {
        static final String REQUEST_TYPE = "requesttype";
        static final String DECISION_SCOPES = "decisionscopes";
        static final String DECISION_SCOPE_NAME = "name";
        static final String XDM = "xdm";
        static final String DATA = "data";
        static final String PROPOSITIONS = "propositions";

        private EventDataKeys() {}
    }

    static final class EventDataValues {
        static final String REQUEST_TYPE_UPDATE = "updatepropositions";
        static final String REQUEST_TYPE_GET = "getpropositions";
        static final String REQUEST_TYPE_TRACK = "trackpropositions";

        private EventDataValues() {}
    }

    static final class Configuration {
        static final String EXTENSION_NAME = "com.adobe.module.configuration";
        static final String OPTIMIZE_OVERRIDE_DATASET_ID = "optimize.datasetId";
    }

    static final class JsonKeys {
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

        static final String DECISION_SCOPES = "decisionScopes";
        static final String XDM = "xdm";
        static final String QUERY = "query";
        static final String QUERY_PERSONALIZATION = "personalization";
        static final String DATA = "data";
        static final String DATASET_ID = "datasetId";
        static final String EXPERIENCE_EVENT_TYPE = "eventType";

        private JsonKeys() {}
    }

    static final class JsonValues {
        static final String EE_EVENT_TYPE_PERSONALIZATION = "personalization.request";

        private JsonValues() {}
    }
}