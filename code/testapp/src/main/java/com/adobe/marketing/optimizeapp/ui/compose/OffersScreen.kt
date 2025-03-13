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
package com.adobe.marketing.optimizeapp.ui.compose

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.adobe.marketing.mobile.optimize.Offer
import com.adobe.marketing.mobile.optimize.OfferType
import com.adobe.marketing.optimizeapp.R
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val clickHandler: (Offer) -> Unit = { offer ->
    offer.tapped()
}

@Composable
fun OffersView(viewModel: MainViewModel) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Box(modifier = Modifier.weight(1f)) {
            if (viewModel.optimizePropositionStateMap.isEmpty()) {
                PlaceHolderOffersList()
            } else {
                OffersList(viewModel = viewModel)
            }

            if (viewModel.showLogs.value.not()) {
                IconButton(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            color = Color(0xFF3949AB),
                            shape = CircleShape
                        ),
                    onClick = {
                        viewModel.showLogs.value = true
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logs),
                            tint = Color.White,
                            contentDescription = "Show Logs"
                        )
                        Text(
                            text = "Logs",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = Color.Gray)
        )

        if (viewModel.showLogs.value) {
            LogBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * 0.3f),
                viewModel = viewModel
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = Color.Gray)
            )
        }

        ActionButtons(viewModel = viewModel)
    }
}

@Composable
fun ActionButtons(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
) {
    Row(
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                viewModel.updatePropositions()
            }
        ) {
            Text(
                text = "Update Propositions",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.button
            )
        }

        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                viewModel.getPropositions()
            }
        ) {
            Text(
                text = "Get Propositions",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.button
            )
        }

        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                viewModel.clearCachedPropositions()
            }
        ) {
            Text(
                text = "Clear Propositions",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.button
            )
        }
    }
}

@Composable
fun OffersList(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        state = listState
    ) {
        items(
            items = viewModel.optimizePropositionStateMap.keys.toList().sorted(),
            key = { item -> item },
            itemContent = { offerType ->
                when (offerType) {
                    viewModel.textOdeText -> {
                        OffersSectionText(sectionName = "Text Offers")
                        TextOffers(
                            offers = viewModel.optimizePropositionStateMap[viewModel.textOdeText]?.offers
                        )
                    }

                    viewModel.textOdeImage -> {
                        OffersSectionText(sectionName = "Image Offers")
                        ImageOffers(viewModel.optimizePropositionStateMap[viewModel.textOdeImage]?.offers)
                    }

                    viewModel.textOdeHtml -> {
                        OffersSectionText(sectionName = "HTML Offers")
                        HTMLOffers(viewModel.optimizePropositionStateMap[viewModel.textOdeHtml]?.offers)
                    }

                    viewModel.textOdeJson -> {
                        OffersSectionText(sectionName = "JSON Offers")
                        JSONOffers(viewModel.optimizePropositionStateMap[viewModel.textOdeJson]?.offers)
                    }

                    viewModel.textTargetMbox -> {
                        OffersSectionText(sectionName = "Target Offers")
                        TargetOffersView(viewModel.optimizePropositionStateMap[viewModel.textTargetMbox]?.offers)
                    }

                    else -> {}
                }.also {
                    val propositions = viewModel.optimizePropositionStateMap[offerType]
                    listState.also {
                        LaunchedEffect(it) {
                            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { lazyListItemInfo -> lazyListItemInfo.key } }
                                .map { visibleItemKeys ->
                                    visibleItemKeys.contains(
                                        propositions?.offers?.get(0)?.proposition?.scope ?: ""
                                    )
                                }
                                .distinctUntilChanged()
                                .filter { result -> result }
                                .collect {
                                    propositions?.offers?.let { offers ->
                                        viewModel.onOfferDisplayed(offers)
                                    }
                                }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun PlaceHolderOffersList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
fun TextOffers(
    offers: List<Offer>? = null,
    placeholder: String = "Placeholder Text"
) {

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
}

@Composable
fun JSONOffers(
    offers: List<Offer>? = null,
    placeholder: String = """{"placeholder": true}"""
) {

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
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ImageOffers(offers: List<Offer>? = null) {
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
}

@Composable
fun HTMLOffers(
    offers: List<Offer>? = null,
    placeholderHtml: String = "<html><body><p style=\"color:green; font-size:20px;position: absolute;top: 50%;left: 50%;margin-right: -50%;transform: translate(-50%, -50%)\">Placeholder Html</p></body></html>",
) {
    Column(
        modifier = Modifier
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
                this.performClick()
            }
        }
    }, update = {
        it.loadData(html, "text/html", "UTF-8")
    }
    )
}

@Composable
fun TargetOffersView(offers: List<Offer>? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        offers?.onEach {
            when (it.type) {
                OfferType.HTML -> HtmlOfferWebView(
                    html = it.content,
                    onclick = { clickHandler(it) })

                else -> Text(
                    text = it.content, modifier = Modifier
                        .padding(vertical = 20.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable { clickHandler(it) }, textAlign = TextAlign.Center
                )
            }
        } ?: TextOffers(offers = null, placeholder = "Placeholder Target Text")
    }
}
