package com.adobe.marketing.optimizeapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adobe.marketing.optimizeapp.ui.model.PreferenceGroupData
import com.adobe.marketing.optimizeapp.ui.model.PreferenceItemData

@Composable
fun SettingsPreferencesCard(preferences: List<PreferenceGroupData>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column {
            preferences.forEachIndexed { index, preference ->
                PreferenceGroup(preference)
                if (index < preferences.lastIndex) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTextFieldWithRadio() {
    SettingsPreferencesCard(
        listOf(
            PreferenceGroupData(
                heading = "GET & UPDATE propositions request timeout",
                options = listOf(
                    PreferenceItemData(
                        isSelected = true,
                        text = "Default timeout | Config timeout",
                        hasTextField = false
                    ),
                    PreferenceItemData(
                        isSelected = false,
                        text = "Custom timeout (in seconds)",
                        hasTextField = true,
                        inputValue = "30"
                    )
                ),
                selectedOption = 0,
                preferenceIndex = 0,
            ),
            PreferenceGroupData(
                heading = "Display propositions tracking mode",
                options = listOf(
                    PreferenceItemData(
                        isSelected = true,
                        text = "Offer wise tracking",
                        hasTextField = false
                    ),
                    PreferenceItemData(
                        isSelected = false,
                        text = "Proposition wise tracking",
                        hasTextField = false
                    )
                ),
                selectedOption = 0,
                preferenceIndex = 1
            )
        )
    )
}
