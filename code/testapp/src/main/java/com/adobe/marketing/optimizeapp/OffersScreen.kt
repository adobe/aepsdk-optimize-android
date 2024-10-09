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

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.identity.IdentityItem
import com.adobe.marketing.mobile.edge.identity.IdentityMap
import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Offer
import com.adobe.marketing.mobile.optimize.OfferType
import com.adobe.marketing.mobile.optimize.Optimize.trackDisplayedPropositions
import com.adobe.marketing.mobile.optimize.OptimizeProposition
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val clickHandler: (Offer) -> Unit = { offer ->
    offer.tapped()
}

private val displayHandler: (Offer) -> Unit = { offer ->
    offer.displayed()
}

private val displayPropositionsHandler: (List<OptimizeProposition>) -> Unit = { props ->
    trackDisplayedPropositions(props)
}

@Composable
fun OffersView(viewModel: MainViewModel) {
    var listState = rememberLazyListState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        if (viewModel.optimizePropositionStateMap.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.85f)
                    .verticalScroll(state = rememberScrollState())
            ) {

                OffersSectionText(sectionName = "Text Offers")
                TextOffers()
                OffersSectionText(sectionName = "Image Offers")
                ImageOffers()
                OffersSectionText(sectionName = "HTML Offers")
                HTMLOffers()
                OffersSectionText(sectionName = "JSON Offers")
                JSONOffers()
                OffersSectionText(sectionName = "Target Offers")
                TargetOffersView()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.85f),
                state = listState
            ) {
                items(items = viewModel.optimizePropositionStateMap.keys.toList().sorted(), key = { item -> item }, itemContent = { item ->
                    when(item) {
                        viewModel.textDecisionScope?.name -> {
                            OffersSectionText(sectionName = "Text Offers")
                            TextOffers(offers = viewModel.optimizePropositionStateMap[viewModel.textDecisionScope?.name]?.offers, listState = listState)
                        }
                        viewModel.imageDecisionScope?.name -> {
                            OffersSectionText(sectionName = "Image Offers")
                            ImageOffers(offers = viewModel.optimizePropositionStateMap[viewModel.imageDecisionScope?.name]?.offers, listState = listState)
                        }
                        viewModel.htmlDecisionScope?.name -> {
                            OffersSectionText(sectionName = "HTML Offers")
                            HTMLOffers(offers = viewModel.optimizePropositionStateMap[viewModel.htmlDecisionScope?.name]?.offers, listState = listState)
                        }
                        viewModel.jsonDecisionScope?.name -> {
                            OffersSectionText(sectionName = "JSON Offers")
                            JSONOffers(offers = viewModel.optimizePropositionStateMap[viewModel.jsonDecisionScope?.name]?.offers, listState = listState)
                        }
                        viewModel.targetMboxDecisionScope?.name -> {
                            OffersSectionText(sectionName = "Target Offers")
                            TargetOffersView(offers = viewModel.optimizePropositionStateMap[viewModel.targetMboxDecisionScope?.name]?.offers, listState = listState)
                        }
                    }
                })
            }.also {
                // uncomment this if you want to send the displayPropositions in batches to the Edge network and don't forget to comment all the displayHandler calls
                // displayPropositionsHandler(viewModel.optimizePropositionStateMap.values.toList())
            }
        }


        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.005f)
                .background(color = Color.Gray)
        )

        Surface(
            elevation = 1.5.dp
        ) {
            Box(modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                ) {
                Button(modifier = Modifier.align(Alignment.CenterStart), onClick = {
                    viewModel.updateDecisionScopes()
                    val decisionScopeList = arrayListOf<DecisionScope>()
                    viewModel.textDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.imageDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.htmlDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.jsonDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.targetMboxDecisionScope?.also { decisionScopeList.add(it) }

                    // Send a custom Identity in IdentityMap as primary identifier to Edge network in personalization query request.
                    val identityMap = IdentityMap()
                    identityMap.addItem(IdentityItem("1111", AuthenticatedState.AUTHENTICATED, true), "userCRMID")
                    Identity.updateIdentities(identityMap)

                    val data = mutableMapOf<String, Any>()
                    val targetParams = mutableMapOf<String, String>()

                    if(viewModel.targetMboxDecisionScope?.name?.isNotEmpty() == true) {
                        viewModel.targetParamsMbox.forEach {
                            if (it.key.isNotEmpty() && it.value.isNotEmpty()) {
                                targetParams[it.key] = it.value
                            }
                        }

                        viewModel.targetParamsProfile.forEach {
                            if(!it.key.isNullOrEmpty() && !it.value.isNullOrEmpty()){
                                targetParams[it.key] = it.value
                            }
                        }

                        if(viewModel.isValidOrder){
                            targetParams["orderId"] = viewModel.textTargetOrderId
                            targetParams["orderTotal"] = viewModel.textTargetOrderTotal
                            targetParams["purchasedProductIds"] = viewModel.textTargetPurchaseId
                        }

                        if(viewModel.isValidProduct){
                            targetParams["productId"] = viewModel.textTargetProductId
                        targetParams["categoryId"] = viewModel.textTargetProductCategoryId
                        }

                        if (targetParams.isNotEmpty()) {
                            data["__adobe"] = mapOf<String, Any>(Pair("target", targetParams))
                        }
                    }
                    data["dataKey"] = "5678"
                    viewModel.updatePropositions(
                        decisionScopes = decisionScopeList,
                        xdm = mapOf(Pair("xdmKey", "1234")),
                        data = data
                    )
                }) {
                    Text(
                        text = "Update \n Propositions",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.button
                    )
                }

                Button(modifier = Modifier.align(Alignment.Center), onClick = {
                    viewModel.updateDecisionScopes()
                    val decisionScopeList = arrayListOf<DecisionScope>()
                    viewModel.textDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.imageDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.htmlDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.jsonDecisionScope?.also { decisionScopeList.add(it) }
                    viewModel.targetMboxDecisionScope?.also { decisionScopeList.add(it) }

                    viewModel.getPropositions(decisionScopes = decisionScopeList)
                }) {
                    Text(
                        text = "Get \n Propositions",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.button
                    )
                }

                Button(modifier = Modifier.align(Alignment.CenterEnd), onClick = {
                    viewModel.clearCachedPropositions()
                }) {
                    Text(
                        text = "Clear \n Propositions",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }
}



@Composable
fun OffersSectionText(sectionName: String) {
    Text(
        text = sectionName,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.LightGray)
            .padding(10.dp),
        textAlign = TextAlign.Left,
        style = MaterialTheme.typography.subtitle1
    )
}

@Composable
fun TextOffers(offers: List<Offer>? = null, placeholder: String = "Placeholder Text", listState: LazyListState? = null) {

    offers?.let { offersList ->
        offersList.forEach { offer ->
            TextOffer(offer = offer)
        }
    } ?: Text(
        text = placeholder,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
            .height(100.dp),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )

    listState?.also {
        LaunchedEffect(it) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { lazyListItemInfo -> lazyListItemInfo.key } }
                .map { visibleItemKeys -> visibleItemKeys.contains(offers?.get(0)?.proposition?.scope ?: "") }
                .distinctUntilChanged()
                .filter { result -> result }
                .collect {
                    offers?.forEach(displayHandler)
                }
        }
    }
}

@Composable
fun JSONOffers(offers: List<Offer>? = null, placeholder: String = """{"placeholder": true}""", listState: LazyListState? = null) {

    offers?.let { offersList ->
        offersList.forEach { offer ->
            TextOffer(offer = offer)
        }
    } ?: Text(
        text = placeholder,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
            .height(100.dp),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )

    listState?.also {
        LaunchedEffect(it) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { lazyListItemInfo -> lazyListItemInfo.key } }
                .map { visibleItemKeys -> visibleItemKeys.contains(offers?.get(0)?.proposition?.scope ?: "") }
                .distinctUntilChanged()
                .filter { result -> result }
                .collect {
                    offers?.forEach(displayHandler)
                }
        }
    }
}

@Composable
fun TextOffer(offer: Offer) {
    Text(
        text = offer.content,
        modifier = Modifier
            .absolutePadding(top = 5.dp, bottom = 5.dp)
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                clickHandler(offer)
            },
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center)
}

@Composable
fun ImageOffers(offers: List<Offer>? = null, listState: LazyListState? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        offers?.onEach { offer ->
            Image(
                painter = rememberImagePainter(offer.content),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(all = 20.dp)
                    .width(100.dp)
                    .height(100.dp)
                    .clickable {
                        clickHandler(offer)
                    }
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.adobe),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(all = 20.dp)
                .width(100.dp)
                .height(100.dp)
        )
    }

    listState?.also {
        LaunchedEffect(it) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { lazyListItemInfo -> lazyListItemInfo.key } }
                .map { visibleItemKeys -> visibleItemKeys.contains(offers?.get(0)?.proposition?.scope ?: "") }
                .distinctUntilChanged()
                .filter { result -> result }
                .collect {
                    offers?.forEach(displayHandler)
                }
        }
    }
}

@Composable
fun HTMLOffers(offers: List<Offer>?= null, placeholderHtml: String = "<html><body><p style=\"color:green; font-size:20px;position: absolute;top: 50%;left: 50%;margin-right: -50%;transform: translate(-50%, -50%)\">Placeholder Html</p></body></html>", listState: LazyListState? = null) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            offers?.onEach {
                HtmlOfferWebView(html = it.content, onclick = {
                    clickHandler(it)
                    }
                )
            } ?: HtmlOfferWebView(html = placeholderHtml)
        }

    listState?.also {
        LaunchedEffect(it) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { lazyListItemInfo -> lazyListItemInfo.key } }
                .map { visibleItemKeys -> visibleItemKeys.contains(offers?.get(0)?.proposition?.scope ?: "") }
                .distinctUntilChanged()
                .filter { result -> result }
                .collect {
                    offers?.forEach(displayHandler)
                }
        }
    }
}

@Composable
fun HtmlOfferWebView(html: String, onclick: (() -> Unit)? = null) {
    AndroidView(modifier = Modifier
        .padding(vertical = 20.dp)
        .fillMaxWidth()
        .wrapContentHeight(), factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setOnTouchListener { _, _ ->
                onclick?.invoke()
                true
            }
        }
    }, update = {
            it.loadData(html, "text/html", "UTF-8")
        }
    )
}

@Composable
fun TargetOffersView(offers: List<Offer>? = null, listState: LazyListState? = null) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        offers?.onEach {
            when (it.type) {
                OfferType.HTML -> HtmlOfferWebView(html = it.content, onclick = {clickHandler(it)})
                else -> Text(text = it.content, modifier = Modifier
                    .padding(vertical = 20.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable { clickHandler(it) }, textAlign = TextAlign.Center)
            }
        } ?: TextOffers(offers = null, placeholder = "Placeholder Target Text")
    }

    listState?.also {
        LaunchedEffect(it) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { lazyListItemInfo -> lazyListItemInfo.key } }
                .map { visibleItemKeys -> visibleItemKeys.contains(offers?.get(0)?.proposition?.scope ?: "") }
                .distinctUntilChanged()
                .filter { result -> result }
                .collect {
                    offers?.forEach(displayHandler)
                }
        }
    }
}
