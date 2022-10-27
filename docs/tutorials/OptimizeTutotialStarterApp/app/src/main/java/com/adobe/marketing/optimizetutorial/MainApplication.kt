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
package com.adobe.marketing.optimizetutorial

import android.app.Application
import com.adobe.marketing.mobile.MobileCore

/* Optimize Tutorial: CODE SECTION 1/10 BEGINS
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Lifecycle
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.optimize.Optimize
// Optimize Tutorial: CODE SECTION 1 ENDS */

class MainApplication : Application() {

    companion object {
        const val DATACOLLECTION_ENVIRONMENT_FILE_ID = ""
        const val OVERRIDE_DATASET_ID = ""
    }

    override fun onCreate() {
        super.onCreate()

        /* Optimize Tutorial: CODE SECTION 2/10 BEGINS
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Identity.registerExtension()
        Lifecycle.registerExtension()
        Edge.registerExtension()
        Optimize.registerExtension()
        Assurance.registerExtension()

        MobileCore.configureWithAppID(DATACOLLECTION_ENVIRONMENT_FILE_ID)
        MobileCore.start {
            print("Adobe mobile SDKs are successfully registered.")
        }
        // FOR DEMO PURPOSE ONLY: Update Configuration with reduced lifecycle timeout.
        MobileCore.updateConfiguration(mapOf("lifecycle.sessionTimeout" to 10))

        // Optimize Tutorial: CODE SECTION 2 ENDS */

        // Update Configuration with override dataset identifier
        // MobileCore.updateConfiguration(mapOf("optimize.datasetId" to OVERRIDE_DATASET_ID))
    }
}