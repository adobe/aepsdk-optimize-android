# Adobe Journey Optimizer - Decisioning API reference

## Prerequisites

Refer to the [Getting Started Guide](getting-started.md).

## API reference

- [clearPropositions](#clearPropositions)
- [extensionVersion](#extensionVersion)
- [getPropositions](#getPropositions)
- [onPropositionsUpdate](#onPropositionsUpdate)
- [resetIdentities](#resetIdentities)
- [updatePropositions](#updatePropositions)
- [updatePropositionsWithCompletionHandler](#updatePropositionsWithCompletionHandler)

## Public classes

- [DecisionScope](#DecisionScope)
- [OptimizeProposition](#OptimizeProposition)
- [Offer](#Offer)
- [OfferType](#OfferType)

## API reference

## clearPropositions

This API clears out the client-side in-memory propositions cache.

### Java

#### Syntax

```java
public static void clearCachedPropositions()
```

#### Example

```java
Optimize.clearCachedPropositions();
```

## extensionVersion

The `extensionVersion()` method (on Android) or the `extensionVersion` property (on iOS) returns the version information for currently installed AEPOptimize extension.

### Java

#### Syntax

```java
public static String extensionVersion()
```

#### Example

```java
Optimize.extensionVersion();
```

## getPropositions

This API retrieves the previously fetched propositions, for the provided decision scopes, from the in-memory extension propositions cache. The completion callback is invoked with the decision propositions corresponding to the given decision scopes. If a certain decision scope has not already been fetched prior to this API call, it will not be contained in the returned propositions.

### Java

#### Syntax

```java
public static void getPropositions(final List<DecisionScope> decisionScopes, final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback)
```

* _decisionScopes_ is a list of decision scopes for which propositions are requested.
* _callback_ `call` method is invoked with propositions map of type `Map<DecisionScope, OptimizeProposition>`. If the callback is an instance of [AdobeCallbackWithError](https://developer.adobe.com/client-sdks/documentation/mobile-core/api-reference/#adobecallbackwitherror), and if the operation times out or an error occurs in retrieving propositions, the `fail` method is invoked with the appropriate [AdobeError](https://developer.adobe.com/client-sdks/documentation/mobile-core/api-reference/#adobeerror).

#### Example

```java
final DecisionScope decisionScope1 = DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 2);
final DecisionScope decisionScope2 = new DecisionScope("myScope");

final List<DecisionScope> decisionScopes = new ArrayList<>();
decisionScopes.add(decisionScope1);
decisionScopes.add(decisionScope2);

Optimize.getPropositions(scopes, new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
    @Override
    public void fail(final AdobeError adobeError) {
        // handle error
    }

    @Override
    public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
        if (propositionsMap != null && !propositionsMap.isEmpty()) {
            // get the propositions for the given decision scopes
            if (propositionsMap.contains(decisionScope1)) {
                final OptimizeProposition proposition1 = propsMap.get(decisionScope1)
                // read proposition1 offers
            }
            if (propositionsMap.contains(decisionScope2)) {
                final OptimizeProposition proposition2 = propsMap.get(decisionScope2)
                // read proposition2 offers
            }
        }
    }
});
```

## onPropositionsUpdate

This API registers a permanent callback which is invoked whenever the Edge extension dispatches a response Event received from the Experience Edge Network upon a personalization query. The personalization query requests can be triggered by the `updatePropositions` API, Edge extension `sendEvent` API or launch consequence rules.

### Java

#### Syntax

```java
public static void onPropositionsUpdate(final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback)
```

* _callback_ `call` method is invoked with propositions map of type `Map<DecisionScope, OptimizeProposition>`. If the callback is an instance of `AdobeCallbackWithError`, and if the operation times out or an error occurs in retrieving propositions, the `fail` method is invoked with the appropriate `AdobeError`.

#### Example

```java
Optimize.onPropositionsUpdate(new AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>() {
    @Override
    public void fail(final AdobeError adobeError) {
        // handle error
    }

    @Override
    public void call(final Map<DecisionScope, OptimizeProposition> propositionsMap) {
        if (propositionsMap != null && !propositionsMap.isEmpty()) {
            // handle propositions
        }
    }
});
```

## resetIdentities

This MobileCore API is a request to each extension to reset its identities. Every extension responds to this request in its own unique manner. For example, Optimize extension uses this API call to clear out its client-side in-memory propositions cache. For details, see [Mobile Core - API reference](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md).

> **Warning
> This API call can lead to unintended SDK behavior, e.g. resetting of Experience Cloud ID (ECID). So it should be sparingly used and extreme caution should be followed!

## updatePropositions

This API dispatches an Event for the Edge network extension to fetch decision propositions, for the provided decision scopes array, from the decisioning services enabled in the Experience Edge. The returned decision propositions are cached in-memory in the Optimize SDK extension and can be retrieved using `getPropositions` API.

### Java

#### Syntax

```java
public static void updatePropositions(final List<DecisionScope> decisionScopes, final Map<String, Object> xdm, final Map<String, Object> data)
```

* _decisionScopes_ is a list of decision scopes for which propositions need updating.
* _xdm_ is a map containing additional xdm formatted data to be attached to the Experience Event.
* _data_ is a map containing additional freeform data to be attached to the Experience Event.

#### Example

```java
final DecisionScope decisionScope1 = DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 2);
final DecisionScope decisionScope2 = new DecisionScope("myScope");

final List<DecisionScope> decisionScopes = new ArrayList<>();
decisionScopes.add(decisionScope1);
decisionScopes.add(decisionScope2);

Optimize.updatePropositions(decisionScopes, 
                            new HashMap<String, Object>() {
                                {
                                    put("xdmKey", "xdmValue");
                                }
                            },
                            new HashMap<String, Object>() {
                                {
                                    put("dataKey", "dataValue");
                                }
                            });
```

## updatePropositionsWithCompletionHandler

This API dispatches an event for the Edge network extension to fetch decision propositions, for the provided decision scopes array, from the decisioning services enabled in the Experience Edge. The returned decision propositions are cached in-memory in the Optimize SDK extension and can be retrieved using `getPropositions` API.

> [!TIP]
> Completion callback passed to `updatePropositions` supports network timeout and fatal errors returned by edge network along with fetched propositions data. The SDK's internal retry mechanism handles the recoverable HTTP errors. As a result, recoverable HTTP errors are not returned through this callback.

### Java

#### Syntax

```java
public static void updatePropositions(final List<DecisionScope> decisionScopes, 
                                      final Map<String, Object> xdm,
                                      final Map<String, Object> data,
                                      final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback)
```

* _decisionScopes_ is a list of decision scopes for which propositions need updating.
* _xdm_ is a map containing additional xdm formatted data to be attached to the Experience Event.
* _data_ is a map containing additional freeform data to be attached to the Experience Event.
* _callback_ is an optional completion handler that is invoked at the completion of the edge request. `call` method is invoked with propositions map of type `Map<DecisionScope, OptimizeProposition>`. If the callback is an instance of `AdobeCallbackWithOptimizeError`, and if the operation times out or an error occurs in retrieving propositions, the `fail` method is invoked with the appropriate [AEPOptimizeError](https://developer.adobe.com/client-sdks/edge/adobe-journey-optimizer-decisioning/api-reference/#aepoptimizeerror). _Note:_ In certain cases, both the success and failure callbacks may be triggered. To handle these cases, ensure that your implementation checks for both successful propositions and errors within the callback, as both may be present simultaneously.

#### Example

```java
final DecisionScope decisionScope1 = DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 2);
final DecisionScope decisionScope2 = new DecisionScope("myScope");

final List<DecisionScope> decisionScopes = new ArrayList<>();
decisionScopes.add(decisionScope1);
decisionScopes.add(decisionScope2);

Optimize.updatePropositions(decisionScopes,
                            new HashMap<String, Object>() {
                                {
                                    put("xdmKey", "xdmValue");
                                }
                            },
                            new HashMap<String, Object>() {
                                {
                                    put("dataKey", "dataValue");
                                }
                            },
                            new AdobeCallbackWithOptimizeError<Map<DecisionScope, OptimizeProposition>>() {
                                @Override
                                public void fail(AEPOptimizeError optimizeError) {
                                    responseError = optimizeError;
                                }

                                @Override
                                public void call(Map<DecisionScope, OptimizeProposition> propositionsMap) {
                                    responseMap = propositionsMap;
                                }
                            });
```

## Public classes

### DecisionScope

This class represents the decision scope which is used to fetch the decision propositions from the Edge decisioning services. The encapsulated scope name can also represent the Base64-encoded JSON string created using the provided activityId, placementId, and itemCount.

#### Java

```java
/**
 * {@code DecisionScope} class represents a scope used to fetch personalized offers from the Experience Edge network.
 */
public class DecisionScope {

    /**
     * Constructor creates a {@code DecisionScope} using the provided {@code name}.
     *
     * @param name {@link String} containing scope name.
     */
    public DecisionScope(final String name) {...}

    /**
     * Constructor creates a {@code DecisionScope} using the provided {@code activityId} and {@code placementId}.
     *
     * This constructor assumes the item count for the given scope to be {@value #DEFAULT_ITEM_COUNT}.
     *
     * @param activityId {@link String} containing activity identifier for the given scope.
     * @param placementId {@code String} containing placement identifier for the given scope.
     */
    public DecisionScope(final String activityId, final String placementId) {...}

    /**
     * Constructor creates a {@code DecisionScope} using the provided {@code activityId} and {@code placementId}.
     *
     * @param activityId {@link String} containing activity identifier for the given scope.
     * @param placementId {@code String} containing placement identifier for the given scope.
     * @param itemCount {@code String} containing number of items to be returned for the given scope.
     */
    public DecisionScope(final String activityId, final String placementId, final int itemCount) {...}

    /**
     * Gets the name for this scope.
     *
     * @return {@link String} containing the scope name.
     */
    public String getName() {...}
}
```

### OptimizeProposition

This class represents the decision propositions received from the decisioning services, upon a personalization query request to the Experience Edge network.

#### Java

```java
public class OptimizeProposition {

    /**
     * Constructor creates a {@code OptimizeProposition} using the provided propostion {@code id}, {@code offers}, {@code scope} and {@code scopeDetails}.
     *
     * @param id {@link String} containing proposition identifier.
     * @param offers {@code List<Offer>} containing proposition items.
     * @param scope {@code String} containing encoded scope.
     * @param scopeDetails {@code Map<String, Object>} containing scope details.
     */
    OptimizeProposition(final String id, final List<Offer> offers, final String scope, final Map<String, Object> scopeDetails) {...}

    /**
     * Gets the {@code OptimizeProposition} identifier.
     *
     * @return {@link String} containing the {@link OptimizeProposition} identifier.
     */
    public String getId() {...}

    /**
     * Gets the {@code OptimizeProposition} items.
     *
     * @return {@code List<Offer>} containing the {@link OptimizeProposition} items.
     */
    public List<Offer> getOffers() {...}

    /**
     * Gets the {@code OptimizeProposition} scope.
     *
     * @return {@link String} containing the encoded {@link OptimizeProposition} scope.
     */
    public String getScope() {...}

    /**
     * Gets the {@code OptimizeProposition} scope details.
     *
     * @return {@code Map<String, Object>} containing the {@link OptimizeProposition} scope details.
     */
    public Map<String, Object> getScopeDetails() {...}

    /**
     * Generates a map containing XDM formatted data for {@code Experience Event - OptimizeProposition Reference} field group from this {@code OptimizeProposition}.
     *
     * The returned XDM data does not contain {@code eventType} for the Experience Event.
     *
     * @return {@code Map<String, Object>} containing the XDM data for the proposition reference.
     */
    public Map<String, Object> generateReferenceXdm() {...}
}
```

### Offer

This class represents the proposition option received from the decisioning services, upon a personalization query to the Experience Edge network.

#### Java

```java
public class Offer {

    /**
     * {@code Offer} Builder.
     */
    public static class Builder {
        
        /**
        * Builder constructor with required {@code Offer} attributes as parameters.
        *
        * It sets default values for remaining {@link Offer} attributes.
        *
        * @param id required {@link String} containing {@code Offer} identifier.
        * @param type required {@link OfferType} indicating the {@code Offer} type.
        * @param content required {@code String} containing the {@code Offer} content.
        */
        public Builder(final String id, final OfferType type, final String content) {...}

        /**
        * Sets the etag for this {@code Offer}.
        *
        * @param etag {@link String} containing {@link Offer} etag.
        * @return this Offer {@link Builder}
        * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
        */
        public Builder setEtag(final String etag) {...}

        /**
         * Sets the score for this {@code Offer}.
         *
         * @param score {@code double} containing {@link Offer} score.
         * @return this Offer {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setScore(final double score) {...}

        /**
        * Sets the schema for this {@code Offer}.
        *
        * @param schema {@link String} containing {@link Offer} schema.
        * @return this Offer {@link Builder}
        * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
        */
        public Builder setSchema(final String schema) {...} 

        /**
         * Sets the metadata for this {@code Offer}.
         *
         * @param meta {@code Map<String, Object>} containing {@link Offer} metadata.
         * @return this Offer {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setMeta(final Map<String, Object> meta) {...}

        /**
        * Sets the language for this {@code Offer}.
        *
        * @param language {@code List<String>} containing supported {@link Offer} language.
        * @return this Offer {@link Builder}
        * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
        */
        public Builder setLanguage(final List<String> language) {...}

        /**
        * Sets the characteristics for this {@code Offer}.
        *
        * @param characteristics {@code Map<String, String>} containing {@link Offer} characteristics.
        * @return this Offer {@link Builder}
        * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
        */
        public Builder setCharacteristics(final Map<String, String> characteristics) {...}

        /**
        * Builds and returns the {@code Offer} object.
        *
        * @return {@link Offer} object or null.
        * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
        */
        public Offer build() {...}
    }

    /**
     * Gets the {@code Offer} identifier.
     *
     * @return {@link String} containing the {@link Offer} identifier.
     */
    public String getId() {...}

    /**
     * Gets the {@code Offer} etag.
     *
     * @return {@link String} containing the {@link Offer} etag.
     */
    public String getEtag() {...}

    /**
     * Gets the {@code Offer} score.
     *
     * @return {@code double} containing the {@link Offer} score.
     */
    public double getScore() {...}

    /**
     * Gets the {@code Offer} schema.
     *
     * @return {@link String} containing the {@link Offer} schema.
     */
    public String getSchema() {...}

    /**
     * Gets the {@code Offer} metadata.
     *
     * @return {@code Map<String, Object>} containing the {@link Offer} metadata.
     */
    public Map<String, Object> getMeta() {...}

    /**
     * Gets the {@code Offer} type.
     *
     * @return {@link OfferType} indicating the {@link Offer} type.
     */
    public OfferType getType() {...}

    /**
     * Gets the {@code Offer} language.
     *
     * @return {@code List<String>} containing the supported {@link Offer} language.
     */
    public List<String> getLanguage() {...}

    /**
     * Gets the {@code Offer} content.
     *
     * @return {@link String} containing the {@link Offer} content.
     */
    public String getContent() {...}

    /**
     * Gets the {@code Offer} characteristics.
     *
     * @return {@code Map<String, String>} containing the {@link Offer} characteristics.
     */
    public Map<String, String> getCharacteristics() {...}

    /**
     * Gets the containing {@code OptimizeProposition} for this {@code Offer}.
     *
     * @return {@link OptimizeProposition} instance.
     */
    public OptimizeProposition getProposition() {...}

    /**
     * Dispatches an event for the Edge network extension to send an Experience Event to the Edge network with the display interaction data for the
     * given {@code OptimizeProposition} offer.
     */
    public void displayed() {...}

    /**
     * Dispatches an event for the Edge network extension to send an Experience Event to the Edge network with the tap interaction data for the
     * given {@code OptimizeProposition} offer.
     */
    public void tapped() {...}

    /**
     * Generates a map containing XDM formatted data for {@code Experience Event - OptimizeProposition Interactions} field group from this {@code OptimizeProposition} item.
     *
     * The returned XDM data does contain the {@code eventType} for the Experience Event with value {@code decisioning.propositionDisplay}.
     *
     * Note: The Edge sendEvent API can be used to dispatch this data in an Experience Event along with any additional XDM, free-form data, and override
     * dataset identifier.
     *
     * @return {@code Map<String, Object>} containing the XDM data for the proposition interaction.
     */
    public Map<String, Object> generateDisplayInteractionXdm() {...}

    /**
     * Generates a map containing XDM formatted data for {@code Experience Event - OptimizeProposition Interactions} field group from this {@code OptimizeProposition} offer.
     *
     * The returned XDM data contains the {@code eventType} for the Experience Event with value {@code decisioning.propositionInteract}.
     *
     * Note: The Edge sendEvent API can be used to dispatch this data in an Experience Event along with any additional XDM, free-form data, and override
     * dataset identifier.
     *
     * @return {@code Map<String, Object>} containing the XDM data for the proposition interaction.
     */
    public Map<String, Object> generateTapInteractionXdm() {...}

}
```

### OfferType

An enum indicating the type of an offer, derived from the proposition item `format` field in personalization query response.

#### Java

```java
public enum OfferType {
    UNKNOWN, JSON, TEXT, HTML, IMAGE;

    @Override
    public String toString() {...}

    /**
     * Returns the {@code OfferType} for the given {@code format}.
     *
     * @param format {@link String} containing the {@link Offer} format.
     * @return {@link OfferType} indicating the {@code Offer} format.
     */
    public static OfferType from(final String format) {...}
}
```
