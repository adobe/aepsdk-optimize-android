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

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.optimize.AEPOptimizeError
import com.adobe.marketing.mobile.optimize.AdobeCallbackWithOptimizeError
import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Optimize
import com.adobe.marketing.mobile.optimize.OptimizeProposition
import com.adobe.marketing.optimizeapp.models.OptimizePair
import kotlinx.coroutines.flow.MutableStateFlow

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

    var optimizePropositionStateMap = mutableStateMapOf<String, OptimizeProposition>()

    var optimizeErrors = MutableStateFlow("")

    private val optimizePropositionUpdateCallback = object : AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> {
        override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
            propositions?.forEach {
                optimizePropositionStateMap[it.key.name] = it.value
            }
        }

        override fun fail(error: AdobeError?) {
            print("Error in updating OptimizeProposition:: ${error?.errorName ?: "Undefined"}.")
        }
    }

    init {
        Optimize.onPropositionsUpdate(optimizePropositionUpdateCallback)
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
        optimizePropositionStateMap.clear()
        Optimize.getPropositions(decisionScopes, object: AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>{
            override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                propositions?.forEach {
                    optimizePropositionStateMap[it.key.name] = it.value
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
        optimizePropositionStateMap.clear()
        Optimize.updatePropositions(decisionScopes, xdm, data, object: AdobeCallbackWithOptimizeError<Map<DecisionScope, OptimizeProposition>>{
            override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                propositions?.let {
                    Log.i("Optimize Test App","Propositions updated successfully.")
                } ?: run {
                    Log.i("Optimize Test App","No propositions provided.")
                }
            }

            override fun fail(error: AEPOptimizeError?) {
                Log.i("Optimize Test App","Error in updating Propositions:: Status: ${error?.status ?: "Undefined"}, Title: ${error?.title ?: "Undefined"}.")
                optimizeErrors.value = "Error in updating Propositions:: ${error?.status ?: "Undefined"}."
            }

        })
    }

    /**
     * Calls the Optimize SDK API to clear the cached Propositions [Optimize.clearCachedPropositions]
     */
    fun clearCachedPropositions() {
        optimizePropositionStateMap.clear()
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