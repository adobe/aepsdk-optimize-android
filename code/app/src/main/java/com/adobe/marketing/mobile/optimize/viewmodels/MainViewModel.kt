package com.adobe.marketing.mobile.optimize.viewmodels

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Offer
import com.adobe.marketing.mobile.optimize.Optimize
import com.adobe.marketing.mobile.optimize.Proposition
import com.adobe.marketing.mobile.optimize.models.OptimizePair

class MainViewModel: ViewModel() {

    //Settings textField Values
    var textLaunchId by mutableStateOf("")
    var textAssuranceUrl by mutableStateOf("")
    var textOdeText by mutableStateOf("")
    var textOdeImage by mutableStateOf("")
    var textOdeHtml by mutableStateOf("")
    var textOdeJson by mutableStateOf("")

    var textTargetMbox by mutableStateOf("")
    var textTargetOrderId by mutableStateOf("")
    var textTargetOrderTotal by mutableStateOf("")
    var textTargetPurchaseId by mutableStateOf("")
    var textTargetProductId by mutableStateOf("")
    var textTargetProductCategoryId by mutableStateOf("")

    var targetParamsMbox = mutableStateListOf(OptimizePair("",""))
    var targetParamsProfile = mutableStateListOf(OptimizePair("",""))

    var propositionStateMap = mutableStateMapOf<String, Proposition>()

    private val propositionUpdateCallback = object : AdobeCallbackWithError<Map<DecisionScope, Proposition>> {
        override fun call(propositions: Map<DecisionScope, Proposition>?) {
            propositions?.forEach {
                propositionStateMap[it.key.name] = it.value
                it.value.offers.onEach { offer -> trackOfferDisplayed(offer = offer) }
            }
        }

        override fun fail(error: AdobeError?) {
            print("Error in updating Proposition:: ${error?.errorName ?: "Undefined"}.")
        }
    }

    init {
        Optimize.onPropositionsUpdate(propositionUpdateCallback)
    }

    //Begin: Calls to Optimize SDK API's

    /**
     * Calls the Optimize SDK API to get the extension version see [Optimize.extensionVersion]
     */
    fun getOptimizeExtensionVersion(): String = Optimize.extensionVersion()

    /**
     * Calls the Optimize SDK API to get the Propositions see [Optimize.getPropositions]
     *
     * @param [decisionScopes] a [List] of [DecisionScope]
     */
    fun getPropositions(decisionScopes: List<DecisionScope>) {
        propositionStateMap.clear()
        Optimize.getPropositions(decisionScopes, object: AdobeCallbackWithError<Map<DecisionScope, Proposition>>{
            override fun call(propositions: Map<DecisionScope, Proposition>?) {
                propositions?.forEach {
                    propositionStateMap[it.key.name] = it.value
                    it.value.offers.onEach { offer -> trackOfferDisplayed(offer = offer) }
                }
            }

            override fun fail(error: AdobeError?) {
                print("Error in getting Propositions.")
            }

        })
    }

    /**
     * Calls the Optimize SDK API to update Propositions see [Optimize.updatePropositions]
     *
     * @param decisionScopes a [List] of [DecisionScope]
     * @param xdm a [Map] of xdm params
     * @param data a [Map] of data
     */
    fun updatePropositions(decisionScopes: List<DecisionScope> , xdm: Map<String, String> , data: Map<String, Any>) {
        propositionStateMap.clear()
        Optimize.updatePropositions(decisionScopes, xdm, data)
    }

    /**
     * Calls the Optimize SDK API to clear the cached Propositions [Optimize.clearCachedPropositions]
     */
    fun clearCachedPropositions() {
        propositionStateMap.clear()
        Optimize.clearCachedPropositions()
    }

    /**
     * Calls the Optimize SDK API to track [Offer] displayed.
     */
    fun trackOfferDisplayed(offer: Offer?) {
        offer?.displayed()
    }

    /**
     * Calls the Optimize SDK API to track [Offer] tapped.
     */
    fun trackOfferTapped(offer: Offer?) {
        offer?.tapped()
    }

    //End: Calls to Optimize SDK API's


    var textDecisionScope: DecisionScope? = null
    var imageDecisionScope: DecisionScope? = null
    var htmlDecisionScope: DecisionScope? = null
    var jsonDecisionScope: DecisionScope? = null
    var targetMboxDecisionScope: DecisionScope? = null

    fun updateDecisionScopes() {
        textDecisionScope = DecisionScope(textOdeText)
        imageDecisionScope = DecisionScope(textOdeImage)
        htmlDecisionScope = DecisionScope(textOdeHtml)
        jsonDecisionScope = DecisionScope(textOdeJson)
        targetMboxDecisionScope = DecisionScope(textTargetMbox)
    }

    val isValidOrder: Boolean
        get() = !textTargetOrderId.isNullOrEmpty() && (!textTargetOrderTotal.isNullOrEmpty() && textTargetOrderTotal.toDouble() != null) && !textTargetPurchaseId.isNullOrEmpty()

    val isValidProduct: Boolean
        get() = !textTargetProductId.isNullOrEmpty() && !textTargetProductCategoryId.isNullOrEmpty()
}