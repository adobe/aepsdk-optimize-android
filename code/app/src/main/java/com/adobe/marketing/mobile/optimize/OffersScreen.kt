package com.adobe.marketing.mobile.optimize

import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adobe.marketing.mobile.optimize.viewmodels.MainViewModel

@Composable
fun OffersView(viewModel: MainViewModel) {
    val proposition by viewModel.decisionScopeLiveData.observeAsState(initial = emptyMap())
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
            TextOffers(text = "Placeholder Text.")
            OffersSectionText(sectionName = "Image Offers")
            ImageOffers(R.drawable.adobe)
            OffersSectionText(sectionName = "HTML Offers")
            HTMLOffers(html = "<html><body><p>HTML Placeholder!!</p></body></html>")
            OffersSectionText(sectionName = "JSON Offers")
            TextOffers(text = "{\"JSON Offers Key\":\"JSON Offers value\"}")
            OffersSectionText(sectionName = "Target Offers")
            TextOffers(text = "Target Offers Placeholder.")
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
//                    viewModel.updatePropositions()
                }) {
                    Text(
                        text = "Update \n Propositions",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.button
                    )
                }

                Button(modifier = Modifier.align(Alignment.Center), onClick = {
                    val listDecisionScopes = arrayListOf(
                        DecisionScope(viewModel.textOdeText),
                        DecisionScope(viewModel.textOdeImage),
                        DecisionScope(viewModel.textOdeHtml),
                        DecisionScope(viewModel.textOdeJson),
                        DecisionScope(viewModel.textTargetMbox)
                    )
                    viewModel.getPropositions(listDecisionScopes)
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
fun TextOffers(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 200.dp)
    ) {
    Text(
        text = text,
        modifier = Modifier.align(Alignment.Center),
        style = MaterialTheme.typography.body1
    )
}
}

@Composable
fun ImageOffers(@DrawableRes imageResId: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = null,
            modifier = Modifier.padding(all = 20.dp)
        )
    }
}

@Composable
fun HTMLOffers(html: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), elevation = 1.dp
    ) {

        AndroidView(factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
            update = {
                it.loadData(html, "text/html", "UTF-8")
            }
        )

    }
}
