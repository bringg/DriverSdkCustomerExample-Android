package com.bringg.example.debug

import android.content.Intent
import android.util.Log
import driver_sdk.ActiveCustomerSdkFactory
import driver_sdk.BringgSDKClient
import driver_sdk.connection.http.api.ConfigurationApiManager
import driver_sdk.connection.http.models.ServerConfig
import driver_sdk.controllers.env.DynamicRegion

class LocalEnvironmentSetter {
    private val sdkClient: BringgSDKClient

    init {
        val field = ActiveCustomerSdkFactory::class.java.getDeclaredField("sdkClient").apply {
            isAccessible = true
        }
        sdkClient = field.get(null) as BringgSDKClient
    }

    var isLocalEnvironment = false

    fun setServerEnvironmentFromIntent(intent: Intent) {
        val url = intent.extras?.getString("url")
        val realtime = intent.extras?.getString("realtime")
        if (url != null && realtime != null)
            setServerEnvironment(url, realtime)
    }

    private fun setServerEnvironment(url: String, realtime: String) = with(sdkClient) {
        Log.d(TAG, "setServerEnvironment() called with: url=$url, realtime=$realtime")
        isLocalEnvironment = true
        sharedPreferencesWrapper.edit().putString(KEY_MANUAL_SERVER, url).apply()
        setSelectedServerConfig(ServerConfig(ServerConfig.NAME_LOCAL, validateLocalApiUrl(url), validateLocalRealTimeUrl(realtime)))
        environment = ConfigurationApiManager.ENVIRONMENT_LOCAL
        region = DynamicRegion.LOCAL_REGION
    }

    private fun validateLocalApiUrl(_url: String): String {
        return if (_url.startsWith("http")) _url else "http://$_url"
    }

    private fun validateLocalRealTimeUrl(_url: String): String {
        return if (_url.startsWith("http")) _url else "http://$_url"
    }

    companion object {
        const val TAG = "LocalEnvironmentSetter"
        const val KEY_MANUAL_SERVER = "manual_server"
    }
}