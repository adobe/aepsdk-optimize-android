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
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.identity.IdentityItem
import com.adobe.marketing.mobile.edge.identity.IdentityMap
import com.adobe.marketing.mobile.optimize.AEPOptimizeError
import com.adobe.marketing.mobile.optimize.AdobeCallbackWithOptimizeError
import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Optimize
import com.adobe.marketing.mobile.optimize.OptimizeProposition
import com.adobe.marketing.optimizeapp.impl.LogManager
import com.adobe.marketing.optimizeapp.models.OptimizePair
import com.adobe.marketing.optimizeapp.ui.model.TimeoutConfigsCardData

class MainViewModel : ViewModel() {

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

    var targetParamsMbox = mutableStateListOf(OptimizePair("", ""))
    var targetParamsProfile = mutableStateListOf(OptimizePair("", ""))

    var optimizePropositionStateMap = mutableStateMapOf<String, OptimizeProposition>()

    val showLogs = mutableStateOf(true)
    val logBoxManager = LogManager(maxLogCount = 200)

    private val _dialogContent = mutableStateOf("")
    val dialogContent: State<String> = _dialogContent

    private val _mutableTimeoutConfig = mutableStateOf(
        TimeoutConfigsCardData(
            "10.0",
            pref1Txt = "Use default timeout",
            pref2Txt = "Use custom timeout (in seconds)",
            isCustomTimeoutOpted = false
        )
    )
    val timeoutConfig: State<TimeoutConfigsCardData> = _mutableTimeoutConfig

    fun updateTimeoutConfig(value: TimeoutConfigsCardData) {
        _mutableTimeoutConfig.value = value
    }

    fun showDialog(content: String) {
        _dialogContent.value = content
    }

    fun hideDialog() {
        _dialogContent.value = ""
    }

    //This callback is triggered when there is an update in the propositions.
    private val optimizePropositionUpdateCallback =
        object : AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> {
            override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                logBoxManager.addLog("onUpdateProposition | Success | ${propositions?.size} propositions: \n" +
                        "Propositions updated: ${propositions?.keys?.joinToString { it.name }}")
                propositions?.forEach {
                    optimizePropositionStateMap[it.key.name] = it.value
                }
            }

            override fun fail(error: AdobeError?) {
                showDialog("Error in updating OptimizeProposition:: ${error?.errorName ?: "Undefined"}.")
                logBoxManager.addLog("onUpdateProposition | Failed | ${error?.errorName}")
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
     * Calls the Optimize SDK API to get the Propositions that are already fetched. [Optimize.getPropositions]
     */
    fun getPropositions() {
        optimizePropositionStateMap.clear()

        val decisionScopeList = getDecisionScopes()
        val callback = object : AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> {
            override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                logBoxManager.addLog("Getting Propositions | Success | ${propositions?.size} propositions: \n" +
                        "Propositions received: ${propositions?.keys?.joinToString { it.name }}")
                propositions?.forEach {
                    optimizePropositionStateMap[it.key.name] = it.value
                }
            }

            override fun fail(error: AdobeError?) {
                showDialog("Error in Get Propositions:: ${error?.errorName}")
                logBoxManager.addLog("Getting Propositions | Failed | ${error?.errorName}")
                print("Error in getting Propositions.")
            }
        }

        logBoxManager.addLog("Getting Propositions Called | ${decisionScopeList.size} scopes \n" +
                "Decision Scopes: ${decisionScopeList.joinToString { it.name }}")
        val customTimeoutData = _mutableTimeoutConfig.value
        if (customTimeoutData.isCustomTimeoutOpted && customTimeoutData.value.toDoubleOrNull() != null)
            Optimize.getPropositions(
                decisionScopeList,
                _mutableTimeoutConfig.value.value.toDouble(),
                callback
            )
        else Optimize.getPropositions(decisionScopeList, callback)
    }

    /**
     * Calls the Optimize SDK API to get the Propositions according to given scopes and other data [Optimize.getPropositions]
     */
    fun updatePropositions() {
        updateIdentity()

        val decisionScopeList = getDecisionScopes()
        val targetParams = getTargetParams()
        val data = getDataMap(targetParams)
        val xdmData = mapOf(Pair("xdmKey", "1234"))

        val callback =
            object : AdobeCallbackWithOptimizeError<Map<DecisionScope, OptimizeProposition>> {
                override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                    logBoxManager.addLog("Update Propositions | Success | ${propositions?.size} propositions: \n" +
                            "Propositions updated: ${propositions?.keys?.joinToString { it.name }}")
                    Log.i("Optimize Test App", "Propositions updated successfully.")
                }

                override fun fail(error: AEPOptimizeError?) {
                    showDialog("Error in Update Propositions:: ${error?.adobeError?.errorName ?: "Undefined"}.")
                    logBoxManager.addLog("Update Propositions | Failed | ${error?.adobeError?.errorName}")
                    Log.i(
                        "Optimize Test App",
                        "Error in updating Propositions:: ${error?.title ?: "Undefined"}."
                    )
                }
            }
        optimizePropositionStateMap.clear()
        logBoxManager.addLog("Update Propositions Called | ${decisionScopeList.size} scopes \n" +
                "Decision Scopes: ${decisionScopeList.joinToString { it.name }}\n" +
                "Data: $data\n" +
                "XDM Data: $xdmData")
        val customTimeoutData = _mutableTimeoutConfig.value
        if (customTimeoutData.isCustomTimeoutOpted && customTimeoutData.value.toDoubleOrNull() != null) Optimize.updatePropositions(
            decisionScopeList,
            xdmData,
            data,
            _mutableTimeoutConfig.value.value.toDouble(),
            callback
        ) else Optimize.updatePropositions(decisionScopeList, xdmData, data, callback)
    }

    /**
     * Calls the Optimize SDK API to clear the cached Propositions  [Optimize.clearCachedPropositions]
     */
    fun clearCachedPropositions() {
        logBoxManager.addLog("Clearing Propositions :\n" +
                "Propositions before clearing: ${optimizePropositionStateMap.keys}")
        optimizePropositionStateMap.clear()
        Optimize.clearCachedPropositions()
    }

    private fun updateIdentity() {
        // Send a custom Identity in IdentityMap as primary identifier to Edge network in personalization query request.
        val identityMap = IdentityMap()
        identityMap.addItem(
            IdentityItem("1111", AuthenticatedState.AUTHENTICATED, true),
            "userCRMID"
        )
        Identity.updateIdentities(identityMap)
    }

    //End: Calls to Optimize SDK APIs


    private fun getDecisionScopes(): List<DecisionScope> {
        return listOf(
            DecisionScope(textOdeText),
            DecisionScope(textOdeImage),
            DecisionScope(textOdeHtml),
            DecisionScope(textOdeJson),
            DecisionScope(textTargetMbox)
        )
    }

    private fun getDataMap(targetParams: Map<String, String>): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>()
        if (targetParams.isNotEmpty()) {
            data["__adobe"] = mapOf<String, Any>(Pair("target", targetParams))
        }
        data["dataKey"] = "5678"
        return data
    }

    private fun getTargetParams(): Map<String, String> {
        val targetParams = mutableMapOf<String, String>()

        if (textTargetMbox.isNotEmpty()) {
            targetParamsMbox.forEach {
                if (it.key.isNotEmpty() && it.value.isNotEmpty()) {
                    targetParams[it.key] = it.value
                }
            }

            targetParamsProfile.forEach {
                if (it.key.isNotEmpty() && it.value.isNotEmpty()) {
                    targetParams[it.key] = it.value
                }
            }

            if (isValidOrder) {
                targetParams["orderId"] = textTargetOrderId
                targetParams["orderTotal"] = textTargetOrderTotal
                targetParams["purchasedProductIds"] = textTargetPurchaseId
            }

            if (isValidProduct) {
                targetParams["productId"] = textTargetProductId
                targetParams["categoryId"] = textTargetProductCategoryId
            }
        }
        return targetParams
    }

    private val isValidOrder: Boolean
        get() = textTargetOrderId.isNotEmpty() && (textTargetOrderTotal.isNotEmpty()) && textTargetPurchaseId.isNotEmpty()

    private val isValidProduct: Boolean
        get() = textTargetProductId.isNotEmpty() && textTargetProductCategoryId.isNotEmpty()
}