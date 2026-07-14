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
import com.adobe.marketing.mobile.optimize.Offer
import com.adobe.marketing.mobile.optimize.OfferUtils.displayed
import com.adobe.marketing.mobile.optimize.Optimize
import com.adobe.marketing.mobile.optimize.OptimizeProposition
import com.adobe.marketing.optimizeapp.impl.LogManager
import com.adobe.marketing.optimizeapp.models.OptimizePair
import com.adobe.marketing.optimizeapp.ui.model.PreferenceGroupData
import com.adobe.marketing.optimizeapp.ui.model.PreferenceItemData

class MainViewModel : ViewModel() {

    /**
     * XDM-encoded Offer Decisioning scope used only by [updatePropositionsSequence]'s call2/5/6.
     * Not provisioned in this sandbox/datastream, so it 404s at Edge (see BATCHING_OBSERVATIONS.md
     * Section 6/11) — kept out of the textOdeText Settings field default so the plain
     * "Update Propositions" button (used as a warm-up call in measurement) succeeds cleanly.
     */
    private val SEQUENCE_ODE_SCOPE =
        "eyJ4ZG06YWN0aXZpdHlJZCI6Inhjb3JlOm9mZmVyLWFjdGl2aXR5OjE4ZTBlZjZlZDg5MWI5NTEiLCJ4ZG06cGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MThlMGVlMDQ5NGRkMTdjNCJ9"

    // Spacing between updatePropositionsSequence()'s 6 calls — see that function's doc for why.
    private val INTER_CALL_DELAY_MS = 20L

    //Settings textField Values
    var textAssuranceUrl by mutableStateOf("")
    var textOdeText by mutableStateOf("")
    var textOdeImage by mutableStateOf("")
    var textOdeHtml by mutableStateOf("")
    var textOdeJson by mutableStateOf("")

    var textTargetMbox by mutableStateOf("mboxAug")
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

    private val _mutablePreferences = mutableStateOf(
        listOf(
            PreferenceGroupData(
                heading = "GET & UPDATE propositions request timeout",
                options = listOf(
                    PreferenceItemData(
                        isSelected = true,
                        text = "Default timeout | Config timeout",
                        hasTextField = false
                    ),
                    PreferenceItemData(
                        isSelected = false,
                        text = "Custom timeout (in seconds)",
                        hasTextField = true,
                        inputValue = "30"
                    )
                ),
                selectedOption = 0,
                preferenceIndex = 0,
                onSelectionUpdate = { prefIdx, selIdx -> updateSelectedOption(prefIdx, selIdx) },
                onTextChange = { preferenceIndex, selectionIndex, value ->
                    updateTextFieldValue(
                        preferenceIndex,
                        selectionIndex,
                        value
                    )
                }
            ),
            PreferenceGroupData(
                heading = "Display propositions tracking mode",
                options = listOf(
                    PreferenceItemData(
                        isSelected = true,
                        text = "Offer wise tracking",
                        hasTextField = false
                    ),
                    PreferenceItemData(
                        isSelected = false,
                        text = "Proposition wise tracking",
                        hasTextField = false
                    )
                ),
                selectedOption = 0,
                preferenceIndex = 1,
                onSelectionUpdate = { prefIdx, selIdx -> updateSelectedOption(prefIdx, selIdx) },
                onTextChange = { preferenceIndex, selectionIndex, value ->
                    updateTextFieldValue(
                        preferenceIndex,
                        selectionIndex,
                        value
                    )
                }
            )
        )
    )
    val preferences: State<List<PreferenceGroupData>> = _mutablePreferences

    private fun updateSelectedOption(preferenceIndex: Int, selectedIndex: Int) {
        _mutablePreferences.value = _mutablePreferences.value.mapIndexed { index, group ->
            if (index == preferenceIndex) group.copy(selectedOption = selectedIndex) else group
        }
    }

    private fun updateTextFieldValue(preferenceIndex: Int, selectedIndex: Int, value: String) {
        _mutablePreferences.value = _mutablePreferences.value.mapIndexed { index, group ->
            if (index == preferenceIndex) group.copy(
                    options = group.options.mapIndexed { optionIndex, preference ->
                        if (optionIndex == selectedIndex) preference.copy(inputValue = value) else preference
                    }
                )
            else group
        }
    }


    fun onOfferDisplayed(offers: List<Offer>){
        val selectionOptionForTracking = preferences.value[1].selectedOption
        if(selectionOptionForTracking == 0) offers.forEach { it.displayed() } else offers.displayed()
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
                        "Propositions updated: ${propositions?.keys?.joinToString { it.name }}"
                )
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
                        "Propositions received: ${propositions?.keys?.joinToString { it.name }}"
                )
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
                "Decision Scopes: ${decisionScopeList.joinToString { it.name }}"
        )
        val customTimeoutOption = _mutablePreferences.value.firstOrNull()?.options?.getOrNull(1)
        if (customTimeoutOption?.isSelected == true && customTimeoutOption.hasTextField) {
            customTimeoutOption.text.toDoubleOrNull()?.let { timeout ->
                Optimize.getPropositions(decisionScopeList, timeout, callback)
            } ?: Optimize.getPropositions(decisionScopeList, callback)
        } else
            Optimize.getPropositions(decisionScopeList, callback)
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

        val startTime = System.currentTimeMillis()
        val callback =
            object : AdobeCallbackWithOptimizeError<Map<DecisionScope, OptimizeProposition>> {
                override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                    val elapsed = System.currentTimeMillis() - startTime
                    logBoxManager.addLog("Update Propositions | Success | $elapsed ms | ${propositions?.size} propositions: \n" +
                            "Propositions updated: ${propositions?.keys?.joinToString { it.name }}"
                    )
                    Log.i("Optimize Test App", "Propositions updated successfully in $elapsed ms.")
                }

                override fun fail(error: AEPOptimizeError?) {
                    val elapsed = System.currentTimeMillis() - startTime
                    showDialog("Error in Update Propositions:: ${error?.adobeError?.errorName ?: "Undefined"}.")
                    logBoxManager.addLog("Update Propositions | Failed | $elapsed ms | ${error?.adobeError?.errorName}")
                    Log.i(
                        "Optimize Test App",
                        "Error in updating Propositions:: ${error?.title ?: "Undefined"} (after $elapsed ms)."
                    )
                }
            }
        optimizePropositionStateMap.clear()
        logBoxManager.addLog(
            "Update Propositions Called | ${decisionScopeList.size} scopes \n" +
                    "Decision Scopes: ${decisionScopeList.joinToString { it.name }}\n" +
                    "Data: $data\n" +
                    "XDM Data: $xdmData"
        )
        val customTimeoutOption = _mutablePreferences.value.firstOrNull()?.options?.getOrNull(1)
        if (customTimeoutOption?.isSelected == true && customTimeoutOption.hasTextField) {
            customTimeoutOption.text.toDoubleOrNull()?.let { timeout ->
                Optimize.updatePropositions(
                    decisionScopeList,
                    xdmData,
                    data,
                    timeout,
                    callback
                )
            } ?: Optimize.updatePropositions(decisionScopeList, xdmData, data, 10.0, callback)
        } else
            Optimize.updatePropositions(decisionScopeList, xdmData, data, 10.0, callback)
    }

    /**
     * Batch-test helper: fires [count] updatePropositions calls in a tight loop. Each call emits one
     * Edge event; while the first request is in flight the rest accumulate in the Edge hit queue and
     * are sent together as a single batched /v1/interact request (when edge.batching.enabled=true).
     */
    fun updatePropositionsBatch(count: Int = 5) {
        logBoxManager.addLog("Update Propositions (batch) | firing $count updatePropositions calls")
        repeat(count) { updatePropositions() }
    }

    /**
     * Batch-test helper: fires a per-offer displayed() tracking call for every cached offer. Each
     * call emits one Edge proposition-interaction event; multiple offers therefore produce multiple
     * Edge events that the hit queue batches into one request.
     */
    fun trackReceivedPropositionsBatch() {
        val offers = optimizePropositionStateMap.values.flatMap { it.offers ?: emptyList() }
        logBoxManager.addLog("Track Displayed (batch) | firing displayed() on ${offers.size} offer(s)")
        offers.forEach { it.displayed() }
    }

    /**
     * Measurement harness: fires six DISTINCT updatePropositions calls — call1 alone, then a pause of
     * [INTER_CALL_DELAY_MS], then calls 2‑6 back‑to‑back with no further delay — exercising different
     * scope combinations plus repeats to observe caching:
     *   call1 = [mboxAug]                 call2 = [ODE decisionScope]
     *   call3 = [mboxAug] (repeat)        call4 = [invalidMbox]
     *   call5 = [mboxAug, ODE, invalidMbox]   call6 = [same three] (repeat)
     * Each call logs "SEQCALL <tag> | <result> | <elapsed> ms | ..." (to logBox and logcat via
     * Log.i("Optimize Test App", ...)) so per-call response time can be extracted for each config.
     *
     * Firing all 6 back-to-back races the hit queue's background executor: the first batch cycle
     * reads however many events happen to already be queued when it wakes up, which varies run to
     * run. A single pause after call1 (comfortably longer than the in-process time to schedule/run a
     * batch cycle) lets that first cycle reliably grab only call1. Calls 2‑6 are then fired with no
     * gap between them so they enqueue essentially simultaneously and land in one consistent batch
     * once call1's request clears — no per-call delay to race against each other.
     */
    fun updatePropositionsSequence() {
        val mbox = DecisionScope(textTargetMbox)   // "mboxAug" (valid Target mbox)
        // XDM-encoded Offer Decisioning scope. Hardcoded here (not read from the textOdeText
        // Settings field, which defaults to "" so the plain "Update Propositions" button/warm-up
        // call succeeds cleanly without the ODE-scope failure — see SEQUENCE_ODE_SCOPE for why.
        val ode = DecisionScope(SEQUENCE_ODE_SCOPE)
        val invalid = DecisionScope("invalidMbox") // not-configured mbox

        logBoxManager.addLog("Sequence | firing 6 distinct updatePropositions calls")
        Thread {
            updatePropositionsForScopes("call1_mbox", listOf(mbox))
            Thread.sleep(INTER_CALL_DELAY_MS)
            updatePropositionsForScopes("call2_ode", listOf(ode))
            updatePropositionsForScopes("call3_mbox", listOf(mbox))
            updatePropositionsForScopes("call4_invalidMbox", listOf(invalid))
            updatePropositionsForScopes("call5_all", listOf(mbox, ode, invalid))
            updatePropositionsForScopes("call6_all", listOf(mbox, ode, invalid))
        }.start()
    }

    /**
     * Issues a single updatePropositions call for the given [scopes], timing call → callback and
     * logging the elapsed with [callTag] for measurement. Mirrors [updatePropositions] but takes an
     * explicit scope list and does not clear the UI proposition map (so caching across the sequence
     * is observable).
     */
    private fun updatePropositionsForScopes(callTag: String, scopes: List<DecisionScope>) {
        val targetParams = getTargetParams()
        val data = getDataMap(targetParams)
        val xdmData = mapOf(Pair("xdmKey", "1234"))
        val startTime = System.currentTimeMillis()
        val callback =
            object : AdobeCallbackWithOptimizeError<Map<DecisionScope, OptimizeProposition>> {
                override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val msg = "SEQCALL $callTag | success | $elapsed ms | ${propositions?.size ?: 0} propositions"
                    logBoxManager.addLog(msg)
                    Log.i("Optimize Test App", msg)
                }

                override fun fail(error: AEPOptimizeError?) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val msg = "SEQCALL $callTag | fail | $elapsed ms | ${error?.adobeError?.errorName ?: "Undefined"}"
                    logBoxManager.addLog(msg)
                    Log.i("Optimize Test App", msg)
                }
            }
        Optimize.updatePropositions(scopes, xdmData, data, 10.0, callback)
    }

    /**
     * Calls the Optimize SDK API to clear the cached Propositions  [Optimize.clearCachedPropositions]
     */
    fun clearCachedPropositions() {
        logBoxManager.addLog(
            "Clearing Propositions :\n" +
                    "Propositions before clearing: ${optimizePropositionStateMap.keys.size}"
        )
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
            DecisionScope(textTargetMbox),
            DecisionScope("invalid_mbox")
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