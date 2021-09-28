package com.adobe.marketing.mobile.optimize

import android.app.Application
import com.adobe.marketing.mobile.*

class MainApplication : Application() {

    companion object {
        const val LAUNCH_ENVIRONMENT_FILE_ID = ""
    }

    override fun onCreate() {
        super.onCreate()
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.setApplication(this)

        try {
            Lifecycle.registerExtension()
            Signal.registerExtension()
            Identity.registerExtension()
            com.adobe.marketing.mobile.edge.identity.Identity.registerExtension()
            Edge.registerExtension()
            Optimize.registerExtension()
            Assurance.registerExtension()
            MobileCore.configureWithAppID(LAUNCH_ENVIRONMENT_FILE_ID)
            MobileCore.start(null)
        } catch (e: InvalidInitException) {
            e.printStackTrace()
        }
    }
}