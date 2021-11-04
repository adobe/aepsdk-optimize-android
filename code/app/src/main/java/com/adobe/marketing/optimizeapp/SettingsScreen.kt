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
package com.adobe.marketing.optimizeapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.optimizeapp.models.OptimizePair
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel

@Composable
fun SettingsView(viewModel: MainViewModel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(state = rememberScrollState())
                .background(color = Color.LightGray)
        ) {
            // Launch Environment Id
            SettingsLabel(text = "Launch Environment File Id", align = TextAlign.Start, textStyle = MaterialTheme.typography.subtitle1)
            SettingTextField(value = viewModel.textLaunchId, placeholder = "Enter your appId") {
                viewModel.textLaunchId = it
            }

            // AEP Assurance Start URL
            SettingsLabel(text = "AEPAssurance Start URL", align = TextAlign.Start, textStyle = MaterialTheme.typography.subtitle1)
            SettingTextField(value = viewModel.textAssuranceUrl, placeholder = "Enter Assurance session URL") {
                viewModel.textAssuranceUrl = it
            }
            Button(modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp), onClick = { Assurance.startSession(viewModel.textAssuranceUrl)  }) {
                Text(text = "Start Assurance\nSession", textAlign = TextAlign.Start, style = MaterialTheme.typography.button)
            }

            // AEP Optimize ODE
            SettingsLabel(text = "AEPOptimize-ODE", align = TextAlign.Start, textStyle = MaterialTheme.typography.subtitle1)
            SettingTextField(value = viewModel.textOdeText, placeholder = "Enter Encoded Decision Scope (Text)") { viewModel.textOdeText = it }
            SettingTextField(value = viewModel.textOdeImage, placeholder = "Enter Encoded Decision Scope (Image)") { viewModel.textOdeImage = it }
            SettingTextField(value = viewModel.textOdeHtml, placeholder = "Enter Encoded Decision Scope (HTML)") { viewModel.textOdeHtml = it }
            SettingTextField(value = viewModel.textOdeJson, placeholder = "Enter Encoded Decision Scope (JSON)") { viewModel.textOdeJson = it }

            // AEP Optimize Target
            SettingsLabel(text = "AEPOptimize-Target", align = TextAlign.Start, textStyle = MaterialTheme.typography.subtitle1)
            SettingTextField(value = viewModel.textTargetMbox, placeholder = "Enter Target Mbox") { viewModel.textTargetMbox = it }
            SettingsLabel(text = "Target Parameters - Mbox", align = TextAlign.Center, textStyle = MaterialTheme.typography.subtitle2)
            KeyValuePairsView(keyValuePairList = viewModel.targetParamsMbox)
            SettingsLabel(text = "Target Parameters - Profile", align = TextAlign.Center, textStyle = MaterialTheme.typography.subtitle2)
            KeyValuePairsView(keyValuePairList = viewModel.targetParamsProfile)
            SettingsLabel(text = "Target Parameters - Order", align = TextAlign.Center, textStyle = MaterialTheme.typography.subtitle2)
            SettingTextField(value = viewModel.textTargetOrderId, placeholder = "Enter Order Id") { viewModel.textTargetOrderId = it }
            SettingTextField(value = viewModel.textTargetOrderTotal, placeholder = "Enter Order Total") { viewModel.textTargetOrderTotal = it }
            SettingTextField(value = viewModel.textTargetPurchaseId, placeholder = "Enter Purchased Product Ids (comma-separated)") { viewModel.textTargetPurchaseId = it }
            SettingsLabel(text = "Target Parameters - Product", align = TextAlign.Center, textStyle = MaterialTheme.typography.subtitle2)
            SettingTextField(value = viewModel.textTargetProductId, placeholder = "Enter Product Id") { viewModel.textTargetProductId = it }
            SettingTextField(value = viewModel.textTargetProductCategoryId, placeholder = "Enter Product Category id") { viewModel.textTargetProductCategoryId = it }
            SettingsLabel(text = "About", align = TextAlign.Start, textStyle = MaterialTheme.typography.subtitle1)
            VersionLabel(viewModel.getOptimizeExtensionVersion())
        }
    }
}

@Composable
private fun SettingsLabel(text: String, align: TextAlign, textStyle: TextStyle){
    Text(text = text, modifier = Modifier
        .absolutePadding(top = 20.dp, left = 20.dp, right = 20.dp)
        .fillMaxWidth(), textAlign = align, style = textStyle)
}

@Composable
private fun SettingTextField(value: String, placeholder: String, valueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = {
        valueChange(it)
    }, placeholder = {
        Text(text = placeholder)
    }, modifier = Modifier
        .absolutePadding(left = 20.dp, right = 20.dp, top = 10.dp)
        .fillMaxWidth(),
        shape = RoundedCornerShape(size = 15.dp),
        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
    )
}

@Composable
private fun KeyValuePairsView(keyValuePairList: MutableList<OptimizePair>) {

    keyValuePairList.forEachIndexed { index, pair ->
        KeyValuePairRow(pair = pair, isLastRow = index == (keyValuePairList.size - 1), onclick = { addNew ->
            if(addNew){
                keyValuePairList.add(OptimizePair("",""))
            } else {
                keyValuePairList.removeAt(index)
            }
        }, onKeyChanged = {
            keyValuePairList[index] = OptimizePair(it, pair.value)
        }, onValueChanged = {
            keyValuePairList[index] = OptimizePair(pair.key, it)
        })
    }
}

@Composable
private fun KeyValuePairRow(pair: OptimizePair, isLastRow: Boolean, onclick: (isToAddNew: Boolean) -> Unit, onKeyChanged: (String) -> Unit, onValueChanged: (String) -> Unit) {

    ConstraintLayout(modifier = Modifier
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .fillMaxWidth()) {
        val (key, colon, value, button) = createRefs()

        Text(text = ":", fontWeight = FontWeight.Bold, modifier = Modifier
            .constrainAs(colon) {
                centerVerticallyTo(parent)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })

        TextField(value = pair.key, onValueChange = {
            onKeyChanged(it)
        }, modifier = Modifier
            .constrainAs(key) {
                start.linkTo(parent.start)
                end.linkTo(colon.start)
            }
            .width(150.dp), placeholder = { Text(text = "Enter Key") }, shape = RoundedCornerShape(size = 15.dp),
            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Image(painter = painterResource(id = if(isLastRow) R.drawable.add_circle else R.drawable.remove_circle), contentDescription = null, modifier = Modifier
            .clickable(enabled = true) {
                onclick(isLastRow)
            }
            .constrainAs(button) {
                end.linkTo(parent.end)
                centerVerticallyTo(parent)
            })

        TextField(value = pair.value, onValueChange = {
            onValueChanged(it)
        }, placeholder = {
            Text(text = "Enter Value")
        },
            modifier = Modifier
                .constrainAs(value) {
                    start.linkTo(colon.end)
                    end.linkTo(button.start)
                }
                .width(150.dp)
                ,shape = RoundedCornerShape(size = 15.dp),
            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
    }
}

@Composable
private fun VersionLabel(version: String){
    Box(
        modifier = Modifier
            .absolutePadding(left = 20.dp, right = 20.dp, top = 10.dp, bottom = 20.dp)
            .background(color = Color.White, shape = RoundedCornerShape(15.dp))
            .fillMaxWidth()
    ) {
        Text(
            text = "Version",
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterStart)
        )
        Text(
            text = version, modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterEnd)
        )
    }
}