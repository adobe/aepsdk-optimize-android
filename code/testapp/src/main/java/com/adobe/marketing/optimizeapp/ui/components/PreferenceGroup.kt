package com.adobe.marketing.optimizeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.optimizeapp.ui.model.PreferenceGroupData
import com.adobe.marketing.optimizeapp.ui.model.PreferenceItemData

@Composable
fun PreferenceGroup(data: PreferenceGroupData) = with(data) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        Text(
            text = heading,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = selectedOption == index,
                        onClick = { onSelectionUpdate(data.preferenceIndex, index) }
                    )
                    Text(option.text)

                    if (option.hasTextField && selectedOption == index) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            value = option.inputValue,
                            onValueChange = { newValue ->
                                if (newValue.toDoubleOrNull() != null || newValue.isEmpty()) {
                                    onTextChange(preferenceIndex, index, newValue)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(size = 10.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.White,
                                focusedIndicatorColor = Color.Gray,
                                unfocusedIndicatorColor = Color.Gray
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewPreferenceGroupOfTimeout() {
    Surface {
        PreferenceGroup(
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
                preferenceIndex = 0
            )
        )
    }
}

@Preview
@Composable
fun PreviewPreferenceGroupOfDisplayProposition() {
    Surface {
        PreferenceGroup(
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
    }
}
