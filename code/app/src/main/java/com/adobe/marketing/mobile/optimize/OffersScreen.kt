package com.adobe.marketing.mobile.optimize

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.identity.IdentityItem
import com.adobe.marketing.mobile.edge.identity.IdentityMap
import com.adobe.marketing.mobile.optimize.viewmodels.MainViewModel

@Composable
fun OffersView(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.85f)
                .verticalScroll(state = rememberScrollState())
        ) {
            OffersSectionText(sectionName = "Text Offers")
            TextOffers(offers = viewModel.propositionStateMap[viewModel.textDecisionScope?.name]?.offers, viewModel = viewModel)
            OffersSectionText(sectionName = "Image Offers")
            ImageOffers(offers = viewModel.propositionStateMap[viewModel.imageDecisionScope?.name]?.offers, viewModel = viewModel)
            OffersSectionText(sectionName = "HTML Offers")
            HTMLOffers(offers = viewModel.propositionStateMap[viewModel.htmlDecisionScope?.name]?.offers, viewModel = viewModel)
            OffersSectionText(sectionName = "JSON Offers")
            TextOffers(offers = viewModel.propositionStateMap[viewModel.jsonDecisionScope?.name]?.offers, placeHolder = """{"PlaceHolder": true}}""", viewModel = viewModel)
            OffersSectionText(sectionName = "Target Offers")
            TargetOffersView(offers = viewModel.propositionStateMap[viewModel.targetMboxDecisionScope?.name]?.offers, viewModel = viewModel)
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
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 10.dp)) {
                Button(modifier = Modifier.align(Alignment.CenterStart), onClick = {
                    viewModel.updateDecisionScopes()
                    var decisionScopeList = arrayListOf<DecisionScope>()
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
                            if (!it.key.isNullOrEmpty() && !it.value.isNullOrEmpty()) {
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
                    var decisionScopeList = arrayListOf<DecisionScope>()
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
fun TextOffers(offers: List<Offer>?, placeHolder: String = "Placeholder Text", viewModel: MainViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {

        offers?.let {offersList ->
            Column(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .wrapContentHeight()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                offersList.forEach { offer ->
                    Text(
                        text = offer.content,
                        modifier = Modifier
                            .absolutePadding(top = 5.dp)
                            .height(100.dp)
                            .clickable {
                                viewModel.trackOfferTapped(offer = offer)
                            },
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center)
                }

                }
        } ?: Text(
            text = placeHolder,
            modifier = Modifier
                .padding(vertical = 20.dp)
                .height(100.dp)
                .align(Alignment.Center),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ImageOffers(offers: List<Offer>?, viewModel: MainViewModel) {
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
                    .clickable { viewModel.trackOfferTapped(offer = offer) }
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
fun HTMLOffers(offers: List<Offer>?, placeHolderHtml: String = "<html><body><p>HTML Placeholder!!</p></body></html>", viewModel: MainViewModel) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            offers?.onEach {
                HtmlOfferWebView(html = it.content, onclick = {viewModel.trackOfferTapped(offer = it)})
            } ?: HtmlOfferWebView(html = placeHolderHtml)
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
fun TargetOffersView(offers: List<Offer>?, viewModel: MainViewModel) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        offers?.onEach {
            when (it.type) {
                OfferType.HTML -> HtmlOfferWebView(html = it.content, onclick = {viewModel.trackOfferTapped(it)})
                else -> Text(text = it.content, modifier = Modifier
                    .padding(vertical = 20.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable { viewModel.trackOfferTapped(it) }, textAlign = TextAlign.Center)
            }
        } ?: TextOffers(offers = null, placeHolder = "Placeholder Target Text", viewModel = viewModel)
    }
}
