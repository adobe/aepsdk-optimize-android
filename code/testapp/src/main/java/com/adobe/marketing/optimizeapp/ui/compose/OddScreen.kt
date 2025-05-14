package com.adobe.marketing.optimizeapp.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.optimize.Offer

@Composable
fun OddScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val response = viewModel.oddManager.response.collectAsState()
    val baseUrl = viewModel.oddManager.baseUrl.collectAsState()
    val inputScope = viewModel.oddManager.inputScope.collectAsState()
    val inputUrl = remember { mutableStateOf(TextFieldValue(baseUrl.value)) }
    val inputScopeState = remember { mutableStateOf(TextFieldValue(inputScope.value)) }
    val getResponse = remember { mutableStateOf<List<Offer>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicTextField(
            value = inputUrl.value,
            onValueChange = {
                inputUrl.value = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .padding(8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* Handle Done action */ })
        )

        Button(
            onClick = {
                viewModel.oddManager.updateBaseUrl(inputUrl.value.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Update Base URL")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputScopeState.value,
                onValueChange = {
                    inputScopeState.value = it
                },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* Handle Done action */ })
            )

            Button(
                onClick = {
                    getResponse.value =
                        viewModel.oddManager.getProposition(inputScopeState.value.text)
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "Get")
            }

        }

        Box(modifier = Modifier.weight(1f)) {
            TargetOffersView(
                getResponse.value
            )
        }

        Text(
            text = response.value,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )

        Button(
            onClick = {
                viewModel.oddManager.fetchResponse()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Fetch Propositions")
        }


    }
}

@Preview(showBackground = true)
@Composable
private fun OddScreenPreview() {
    val fakeViewModel = MainViewModel().apply { oddManager.fetchResponse() }
    MainScreen(viewModel = fakeViewModel)
}