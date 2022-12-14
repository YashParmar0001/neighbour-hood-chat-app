package com.example.android.neighbourhood

import android.app.Application
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.OneSignal.PostNotificationResponseHandler
import org.json.JSONObject

class ApplicationClass : Application() {
    private val oneSignalAppId = "93ee221d-02dd-4301-822c-d33a39fc89cf"
    override fun onCreate() {
        super.onCreate()

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)

        // OneSignal Initialization
        OneSignal.initWithContext(this)
        OneSignal.setAppId(oneSignalAppId)

        // promptForPushNotifications will show the native Android notification permission prompt.
        // We recommend removing the following code and instead using an In-App Message to prompt for notification permission (See step 7)
        OneSignal.promptForPushNotifications()

        // If app is in focus then notification will not be displayed
        OneSignal.setNotificationWillShowInForegroundHandler { notificationEvent ->
            Log.d("Application", "Notification received")
            notificationEvent?.complete(
                null
            )
        }
    }

    class Handler : PostNotificationResponseHandler {
        override fun onSuccess(response: JSONObject?) {
            Log.d("ApplicationClass", "Notifications sent")
            Log.d("ApplicationClass", response.toString())
        }

        override fun onFailure(response: JSONObject?) {
            Log.d("ApplicationClass", "Notifications don't sent")
            Log.d("ApplicationClass", response.toString())
        }
    }
}