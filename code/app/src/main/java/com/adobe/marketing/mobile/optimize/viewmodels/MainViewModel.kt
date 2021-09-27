package com.adobe.marketing.mobile.optimize.viewmodels

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.optimize.DecisionScope
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

    var propositionStateMap = mutableStateMapOf<DecisionScope, Proposition>()

    private val propositionUpdateCallback = object : AdobeCallbackWithError<Map<DecisionScope, Proposition>> {
        override fun call(propositions: Map<DecisionScope, Proposition>?) {
            propositionStateMap.clear()
            propositionStateMap.putAll(propositions ?: mapOf())
        }

        override fun fail(error: AdobeError?) {
            print("Error in updating Proposition:: ${error?.errorName ?: "Undefined"}.")
        }
    }

    init {
        Optimize.onPropositionsUpdate(propositionUpdateCallback)
    }

    fun getOptimizeExtensionVersion(): String = Optimize.extensionVersion()

    fun getPropositions(decisionScopes: List<DecisionScope>) {
        Optimize.getPropositions(decisionScopes, object: AdobeCallbackWithError<Map<DecisionScope, Proposition>>{
            override fun call(propositionMap: Map<DecisionScope, Proposition>?) {
                propositionStateMap.clear()
                propositionStateMap.putAll(propositionMap ?: emptyMap())
            }

            override fun fail(error: AdobeError?) {
                print("Error in getting Propositions.")
            }

        })
    }

    fun updatePropositions(decisionScopes: List<DecisionScope> , xdm: Map<String, String> , data: Map<String, Any>) {
        Optimize.updatePropositions(decisionScopes, xdm, data)
    }

    fun clearCachedPropositions() {
        Optimize.clearCachedPropositions()
    }


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

fun SnapshotStateList<OptimizePair>.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    forEach { optimizePair ->
        map[optimizePair.key] = optimizePair.value
    }
    return map
}