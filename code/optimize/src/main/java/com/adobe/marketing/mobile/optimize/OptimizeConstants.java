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
    static final String EXTENSION_VERSION = "3.2.4";
    static final String EXTENSION_NAME = "com.adobe.optimize";
    static final String FRIENDLY_NAME = "Optimize";
    static final double GET_RESPONSE_CALLBACK_TIMEOUT = 10;
    static final double EDGE_CONTENT_COMPLETE_RESPONSE_TIMEOUT = 10;
    static final long TIMEOUT_CONVERSION_FACTOR = 1000;

    static final String ACTIVITY_ID = "activityId";
    static final String XDM_ACTIVITY_ID = "xdm:activityId";
    static final String PLACEMENT_ID = "placementId";
    static final String XDM_PLACEMENT_ID = "xdm:placementId";
    static final String ITEM_COUNT = "itemCount";
    static final String XDM_ITEM_COUNT = "xdm:itemCount";
    static final String XDM_NAME = "xdm:name";

    static final String ERROR_UNKNOWN = "unknown";
    static final Integer UNKNOWN_STATUS = 0;

    private OptimizeConstants() {}

    static final class EventNames {
        static final String UPDATE_PROPOSITIONS_REQUEST = "Optimize Update Propositions Request";
        static final String GET_PROPOSITIONS_REQUEST = "Optimize Get Propositions Request";
        static final String TRACK_PROPOSITIONS_REQUEST = "Optimize Track Propositions Request";
        static final String CLEAR_PROPOSITIONS_REQUEST = "Optimize Clear Propositions Request";
        static final String OPTIMIZE_NOTIFICATION = "Optimize Notification";
        static final String EDGE_PERSONALIZATION_REQUEST = "Edge Optimize Personalization Request";
        static final String EDGE_PROPOSITION_INTERACTION_REQUEST =
                "Edge Optimize Proposition Interaction Request";
        static final String OPTIMIZE_RESPONSE = "Optimize Response";
        static final String OPTIMIZE_UPDATE_COMPLETE = "Optimize Update Propositions Complete";

        private EventNames() {}
    }

    static final class EventType {
        static final String OPTIMIZE = "com.adobe.eventType.optimize";
        static final String EDGE = "com.adobe.eventType.edge";
        static final String GENERIC_IDENTITY = "com.adobe.eventType.generic.identity";

        private EventType() {}
    }

    static final class EventSource {
        static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        static final String REQUEST_RESET = "com.adobe.eventSource.requestReset";
        static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        static final String ERROR_RESPONSE_CONTENT = "com.adobe.eventSource.errorResponseContent";
        static final String NOTIFICATION = "com.adobe.eventSource.notification";
        static final String EDGE_PERSONALIZATION_DECISIONS = "personalization:decisions";
        static final String CONTENT_COMPLETE = "com.adobe.eventSource.contentComplete";
        static final String DEBUG = "com.adobe.eventSource.debug";

        private EventSource() {}
    }

    static final class EventDataKeys {
        static final String REQUEST_TYPE = "requesttype";
        static final String DECISION_SCOPES = "decisionscopes";
        static final String DECISION_SCOPE_NAME = "name";
        static final String XDM = "xdm";
        static final String DATA = "data";
        static final String TIMEOUT = "timeout";
        static final String PROPOSITIONS = "propositions";
        static final String RESPONSE_ERROR = "responseerror";
        static final String PROPOSITION_INTERACTIONS = "propositioninteractions";
        static final String REQUEST_EVENT_ID = "requestEventId";
        static final String COMPLETED_UPDATE_EVENT_ID = "completedUpdateRequestForEventId";

        private EventDataKeys() {}
    }

    static final class EventDataValues {
        static final String REQUEST_TYPE_UPDATE = "updatepropositions";
        static final String REQUEST_TYPE_GET = "getpropositions";
        static final String REQUEST_TYPE_TRACK = "trackpropositions";

        private EventDataValues() {}
    }

    static final class Edge {
        static final String EXTENSION_NAME = "com.adobe.edge";
        static final String EVENT_HANDLE = "type";
        static final String EVENT_HANDLE_TYPE_PERSONALIZATION = "personalization:decisions";
        static final String PAYLOAD = "payload";
        static final String DEBUG = "debug";
        static final String EVENT_SOURCE = "eventSource";
        static final String EVENT_TYPE = "eventType";

        static final class ErrorKeys {
            static final String TYPE = "type";
            static final String DETAIL = "detail";
            static final String STATUS = "status";
            static final String TITLE = "title";
            static final String REPORT = "report";

            private ErrorKeys() {}
        }

        private Edge() {}
    }

    static final class Configuration {
        static final String EXTENSION_NAME = "com.adobe.module.configuration";
        static final String OPTIMIZE_OVERRIDE_DATASET_ID = "optimize.datasetId";

        private Configuration() {}
    }

    static final class JsonKeys {
        static final String PAYLOAD_ID = "id";
        static final String PAYLOAD_SCOPE = "scope";
        static final String PAYLOAD_SCOPEDETAILS = "scopeDetails";
        static final String PAYLOAD_ITEMS = "items";

        static final String PAYLOAD_ITEM_ID = "id";
        static final String PAYLOAD_ITEM_ETAG = "etag";
        static final String PAYLOAD_ITEM_SCORE = "score";
        static final String PAYLOAD_ITEM_SCHEMA = "schema";
        static final String PAYLOAD_ITEM_META = "meta";
        static final String PAYLOAD_ITEM_DATA = "data";
        static final String PAYLOAD_ITEM_DATA_ID = "id";
        static final String PAYLOAD_ITEM_DATA_CONTENT = "content";
        static final String PAYLOAD_ITEM_DATA_DELIVERYURL = "deliveryURL";
        static final String PAYLOAD_ITEM_DATA_FORMAT = "format";
        static final String PAYLOAD_ITEM_DATA_TYPE = "type";
        static final String PAYLOAD_ITEM_DATA_LANGUAGE = "language";
        static final String PAYLOAD_ITEM_DATA_CHARACTERISTICS = "characteristics";

        static final String XDM = "xdm";
        static final String QUERY = "query";
        static final String QUERY_PERSONALIZATION = "personalization";
        static final String SCHEMAS = "schemas";
        static final String DECISION_SCOPES = "decisionScopes";
        static final String DATA = "data";
        static final String DATASET_ID = "datasetId";
        static final String EXPERIENCE_EVENT_TYPE = "eventType";
        static final String EXPERIENCE = "_experience";
        static final String EXPERIENCE_DECISIONING = "decisioning";
        static final String DECISIONING_PROPOSITION_ID = "propositionID";
        static final String DECISIONING_PROPOSITIONS = "propositions";
        static final String DECISIONING_PROPOSITIONS_ID = "id";
        static final String DECISIONING_PROPOSITIONS_SCOPE = "scope";
        static final String DECISIONING_PROPOSITIONS_SCOPEDETAILS = "scopeDetails";
        static final String DECISIONING_PROPOSITIONS_ITEMS = "items";
        static final String DECISIONING_PROPOSITIONS_ITEMS_ID = "id";
        static final String REQUEST = "request";
        static final String REQUEST_SEND_COMPLETION = "sendCompletion";

        private JsonKeys() {}
    }

    static final class JsonValues {
        static final String EE_EVENT_TYPE_PERSONALIZATION = "personalization.request";
        static final String EE_EVENT_TYPE_PROPOSITION_DISPLAY = "decisioning.propositionDisplay";
        static final String EE_EVENT_TYPE_PROPOSITION_INTERACT = "decisioning.propositionInteract";

        // Target schemas
        static final String SCHEMA_TARGET_HTML =
                "https://ns.adobe.com/personalization/html-content-item";
        static final String SCHEMA_TARGET_JSON =
                "https://ns.adobe.com/personalization/json-content-item";
        static final String SCHEMA_TARGET_DEFAULT =
                "https://ns.adobe.com/personalization/default-content-item";

        // Offer Decisioning schemas
        static final String SCHEMA_OFFER_HTML =
                "https://ns.adobe.com/experience/offer-management/content-component-html";
        static final String SCHEMA_OFFER_JSON =
                "https://ns.adobe.com/experience/offer-management/content-component-json";
        static final String SCHEMA_OFFER_IMAGE =
                "https://ns.adobe.com/experience/offer-management/content-component-imagelink";
        static final String SCHEMA_OFFER_TEXT =
                "https://ns.adobe.com/experience/offer-management/content-component-text";

        private JsonValues() {}
    }

    static final class ErrorData {
        static final class Timeout {
            static final Integer STATUS = 408;
            static final String TITLE = "Request Timeout";
            static final String DETAIL = "Update/Get proposition request resulted in a timeout.";

            private Timeout() {}
        }

        static final class Unexpected {
            static final String TITLE = "Unexpected Error";
            static final String DETAIL = "An unexpected error occurred.";

            private Unexpected() {}
        }

        private ErrorData() {}
    }

    static final class HTTPResponseCodes {
        static final int success = 200;
        static final int noContent = 204;
        static final int multiStatus = 207;
        static final int invalidRequest = 400;
        static final int clientTimeout = 408;
        static final int tooManyRequests = 429;
        static final int internalServerError = 500;
        static final int badGateway = 502;
        static final int serviceUnavailable = 503;
        static final int gatewayTimeout = 504;

        private HTTPResponseCodes() {}
    }
}
