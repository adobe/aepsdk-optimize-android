package com.adobe.marketing.mobile.optimize.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    val decisionScopeLiveData: LiveData<Map<DecisionScope, Proposition>>
        get() = mutablePropositionLiveData

    private val mutablePropositionLiveData = MutableLiveData<Map<DecisionScope, Proposition>>()

    fun getOptimizeExtensionVersion(): String = Optimize.extensionVersion()

    fun getPropositions(decisionScopes: List<DecisionScope>) {
        Optimize.getPropositions(decisionScopes, object: AdobeCallbackWithError<Map<DecisionScope, Proposition>>{
            override fun call(propositionMap: Map<DecisionScope, Proposition>?) {
                mutablePropositionLiveData.postValue(propositionMap ?: emptyMap())
            }

            override fun fail(error: AdobeError?) {
                print("Error in getting Propositions.")
            }

        })
    }

    fun updatePropositions(decisionScopes: List<DecisionScope> , xdm: Map<String, Any> , data: Map<String, Any>) {
        Optimize.updatePropositions(decisionScopes, xdm, data)
    }

    fun clearCachedPropositions() {
        Optimize.clearCachedPropositions()
    }
}