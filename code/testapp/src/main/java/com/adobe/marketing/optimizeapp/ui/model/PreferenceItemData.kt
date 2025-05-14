package com.adobe.marketing.optimizeapp.ui.model

data class PreferenceItemData(
    val isSelected: Boolean,
    val text: String,
    val hasTextField: Boolean = false,
    val inputValue: String = ""
)