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
package com.adobe.marketing.optimizeapp.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Offer
import com.adobe.marketing.mobile.optimize.Optimize
import com.adobe.marketing.mobile.optimize.Proposition
import com.adobe.marketing.optimizeapp.models.OptimizePair

class MainViewModel: ViewModel() {

    //Settings textField Values
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
            }
        }

        override fun fail(error: AdobeError?) {
            print("Error in updating Proposition:: ${error?.errorName ?: "Undefined"}.")
        }
    }

    init {
        Optimize.onPropositionsUpdate(propositionUpdateCallback)
    }

    //Begin: Calls to Optimize SDK APIs

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
        get() = textTargetOrderId.isNotEmpty() && (textTargetOrderTotal.isNotEmpty() && textTargetOrderTotal.toDouble() != null) && textTargetPurchaseId.isNotEmpty()

    val isValidProduct: Boolean
        get() = textTargetProductId.isNotEmpty() && textTargetProductCategoryId.isNotEmpty()
}