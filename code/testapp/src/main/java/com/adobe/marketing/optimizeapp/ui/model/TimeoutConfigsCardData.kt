package com.adobe.marketing.optimizeapp.ui.model

data class TimeoutConfigsCardData(
    val value: String,
    val pref1Txt: String = "Use default value",
    val pref2Txt: String = "Use custom value",
    val isCustomTimeoutOpted: Boolean = false
)
