package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import androidx.annotation.VisibleForTesting
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

class Offer private constructor() {

    companion object {
        private const val SELF_TAG = "Offer"

       @JvmStatic
       fun fromEventData(data: Map<String, Any?>?): Offer? {
            if (OptimizeUtils.isNullOrEmpty(data)) {
                Log.debug(
                    OptimizeConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot create Offer object, provided data Map is empty or null."
                )
                return null
            }
            return try {
                val id = DataReader.getString(data, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_ID)
                val etag = DataReader.getString(data, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_ETAG)
                val score = DataReader.optDouble(data, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_SCORE, 0.0)
                val schema = DataReader.getString(data, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_SCHEMA)
                val meta = DataReader.getTypedMap(Any::class.java, data, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_META)
                val offerData = DataReader.getTypedMap(Any::class.java, data, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA)

                if (!OptimizeUtils.isNullOrEmpty(offerData)) {
                    val nestedId = DataReader.getString(offerData, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_ID)
                    if (nestedId.isNullOrEmpty() || nestedId != id) {
                        Log.debug(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "Cannot create Offer object, provided item id is null or empty or doesn't match item data id."
                        )
                        return null
                    }
                    val format = DataReader.getString(offerData, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_FORMAT)
                    val offerType = format?.let { OfferType.from(it) }
                        ?: OfferType.from(DataReader.getString(offerData, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_TYPE))
                    val language = DataReader.getStringList(offerData, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_LANGUAGE)
                    val characteristics = DataReader.getStringMap(offerData, OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_CHARACTERISTICS)

                    val content = getContentFromOfferData(offerData) ?: DataReader.optString(
                        offerData,
                        OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_DELIVERYURL,
                        null
                    ) ?: run {
                        Log.debug(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "Cannot create Offer object, provided data Map doesn't contain valid item data content or deliveryURL."
                        )
                        return null
                    }

                    return Builder(id, offerType, content)
                        .setEtag(etag)
                        .setScore(score)
                        .setSchema(schema)
                        .setMeta(meta)
                        .setLanguage(language)
                        .setCharacteristics(characteristics)
                        .build()
                } else {
                    if (schema != OptimizeConstants.JsonValues.SCHEMA_TARGET_DEFAULT) {
                        Log.debug(
                            OptimizeConstants.LOG_TAG,
                            SELF_TAG,
                            "Cannot create Offer object, provided data Map doesn't contain valid item data."
                        )
                        return null
                    }
                    Log.trace(
                        OptimizeConstants.LOG_TAG,
                        SELF_TAG,
                        "Received default content proposition item, Offer content will be set to an empty string."
                    )
                    return Builder(id, OfferType.UNKNOWN, "").build()
                }
            } catch (e: ClassCastException) {
                Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "Cannot create Offer object, provided data contains invalid fields.")
                null
            } catch (e: DataReaderException) {
                Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "Cannot create Offer object, provided data contains invalid fields.")
                null
            }
        }

        /**
         * Creates a Map<String, Any> using this Offer's attributes.
         *
         * @return Map<String, Any> containing Offer data.
         */
        @JvmStatic
        fun Offer.toEventData(): Map<String, Any> {
            val offerMap = mutableMapOf<String, Any>()
            offerMap[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_ID] = id
            offerMap[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_ETAG] = etag
            offerMap[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_SCORE] = score
            offerMap[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_SCHEMA] = schema
            offerMap[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_META] = meta

            val data = mutableMapOf<String, Any>()
            data[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_ID] = id
            data[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_TYPE] = type.toString()
            data[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_CONTENT] = content
            data[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_LANGUAGE] = language
            data[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_CHARACTERISTICS] = characteristics

            offerMap[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA] = data

            return offerMap
        }


        private fun getContentFromOfferData(offerData: Map<String, Any?>): String {
            val offerContent = offerData[OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_CONTENT]
            return when (offerContent) {
                is String -> offerContent
                is Map<*, *> -> JSONObject(offerContent).toString()
                else -> ""
            }
        }
    }

    var id: String = ""
        private set
    var etag: String = ""
        private set
    var score: Double = 0.0
        private set
    var schema: String = ""
        private set
    var meta: Map<String, Any> = HashMap()
        private set
    var type: OfferType = OfferType.UNKNOWN
        private set
    var language: List<String> = ArrayList()
        private set
    var content: String = ""
        private set
    var characteristics: Map<String, String> = HashMap()
        private set
    var propositionReference: SoftReference<OptimizeProposition>? = null

    class Builder(private val id: String?, private val type: OfferType?, private val content: String?) {
        private val offer = Offer()
        private var didBuild = false

        init {
            offer.id = id ?: ""
            offer.type = type ?: OfferType.UNKNOWN
            offer.content = content ?: ""
        }

        fun setEtag(etag: String?) = apply {
            throwIfAlreadyBuilt()
            offer.etag = etag ?: ""
        }

        fun setScore(score: Double) = apply {
            throwIfAlreadyBuilt()
            offer.score = score
        }

        fun setSchema(schema: String?) = apply {
            throwIfAlreadyBuilt()
            offer.schema = schema ?: ""
        }

        fun setMeta(meta: Map<String, Any>?) = apply {
            throwIfAlreadyBuilt()
            offer.meta = meta ?: HashMap()
        }

        fun setLanguage(language: List<String>?) = apply {
            throwIfAlreadyBuilt()
            offer.language = language ?: ArrayList()
        }

        fun setCharacteristics(characteristics: Map<String, String>?) = apply {
            throwIfAlreadyBuilt()
            offer.characteristics = characteristics ?: HashMap()
        }

        fun build(): Offer {
            throwIfAlreadyBuilt()
            didBuild = true
            return offer
        }

    private static String getContentFromOfferData(final Map<String, Object> offerData) {
        try {
            Object data;

            Object offerContent =
                    offerData.get(OptimizeConstants.JsonKeys.PAYLOAD_ITEM_DATA_CONTENT);
            if (offerContent instanceof List) {
                data = new JSONArray((List<?>) offerContent);
            } else if (offerContent instanceof Map) {
                data = new JSONObject((Map<?, ?>) offerContent);
            } else if (offerContent instanceof String) {
                data = offerContent;
            } else {
                throw new ClassCastException();
            }

            return data.toString();
        } catch (Exception e) {
            throw new ClassCastException();
        }
    }

    fun displayed() {
        OptimizeUtils.trackWithData(generateDisplayInteractionXdm())
    }

    fun tapped() {
        OptimizeUtils.trackWithData(generateTapInteractionXdm())
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun generateDisplayInteractionXdm(): Map<String, Any>? {
        return propositionReference?.get()?.let {
            OptimizeUtils.generateInteractionXdm(
                OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY,
                listOf(it)
            )
        }
    }

    fun generateTapInteractionXdm(): Map<String, Any>? {
        return propositionReference?.get()?.let {
            OptimizeUtils.generateInteractionXdm(
                OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_INTERACT,
                listOf(it)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Offer) return false

        return id == other.id &&
                etag == other.etag &&
                score == other.score &&
                schema == other.schema &&
                meta == other.meta &&
                type == other.type &&
                language == other.language &&
                content == other.content &&
                characteristics == other.characteristics
    }

    override fun hashCode(): Int {
        return Objects.hash(id, etag, score, schema, meta, type, language, content, characteristics)
    }
}
