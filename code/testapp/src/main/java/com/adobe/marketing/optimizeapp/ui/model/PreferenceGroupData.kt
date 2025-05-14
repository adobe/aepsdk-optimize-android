package com.adobe.marketing.optimizeapp.ui.model

data class PreferenceGroupData(
    val heading: String,
    val options: List<PreferenceItemData>,
    val selectedOption: Int,
    val preferenceIndex: Int,
    val onSelectionUpdate: (Int, Int) -> Unit = { _, _ -> },
    val onTextChange: (Int, Int, String) -> Unit = { _, _,_ -> }
)