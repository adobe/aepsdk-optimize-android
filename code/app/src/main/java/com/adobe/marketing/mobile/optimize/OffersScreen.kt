package com.adobe.marketing.mobile.optimize

import android.graphics.Paint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.adobe.marketing.mobile.optimize.viewmodels.MainViewModel
import com.adobe.marketing.mobile.optimize.viewmodels.toMap

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
            TextOffers(offers = viewModel.propositionStateMap[viewModel.textDecisionScope?.name]?.offers)
            OffersSectionText(sectionName = "Image Offers")
            ImageOffers(offers = viewModel.propositionStateMap[viewModel.imageDecisionScope?.name]?.offers)
            OffersSectionText(sectionName = "HTML Offers")
            HTMLOffers(offers = viewModel.propositionStateMap[viewModel.htmlDecisionScope?.name]?.offers)
            OffersSectionText(sectionName = "JSON Offers")
            TextOffers(offers = viewModel.propositionStateMap[viewModel.jsonDecisionScope?.name]?.offers, placeHolder = """{"PlaceHolder": true}}""")
            OffersSectionText(sectionName = "Target Offers")
            TargetOffersView(offers = viewModel.propositionStateMap[viewModel.targetMboxDecisionScope?.name]?.offers)
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
fun TextOffers(offers: List<Offer>?, placeHolder: String = "Placeholder Text") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        var content = offers?.let {
            var offersContent = ""
            it.forEach {offer ->
                offersContent += offer.content + "\n"
            }
            return@let offersContent
        } ?: placeHolder
    Text(
        text = content,
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
fun ImageOffers(offers: List<Offer>?) {
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
fun HTMLOffers(offers: List<Offer>?, placeHolderHtml: String = "<html><body><p>HTML Placeholder!!</p></body></html>") {
        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            offers?.onEach {
                HtmlOfferWebView(html = it.content)
            } ?: HtmlOfferWebView(html = placeHolderHtml)
        }
}

@Composable
fun HtmlOfferWebView(html: String) {
    AndroidView(modifier = Modifier
        .padding(vertical = 20.dp)
        .fillMaxWidth()
        .wrapContentHeight(), factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }, update = {
            it.loadData(html, "text/html", "UTF-8")
        }
    )
}

@Composable
fun TargetOffersView(offers: List<Offer>?) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        offers?.onEach {
            when (it.type) {
                OfferType.HTML -> HtmlOfferWebView(html = it.content)
                else -> Text(text = it.content, modifier = Modifier
                    .padding(vertical = 20.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(), textAlign = TextAlign.Center)
            }
        } ?: TextOffers(offers = null, placeHolder = "Placeholder Target Text")
    }
}
