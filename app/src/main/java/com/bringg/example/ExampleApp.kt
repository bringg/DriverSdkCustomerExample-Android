package com.bringg.example

import android.app.Application
import driver_sdk.ActiveCustomerSDKFactory
import driver_sdk.customer.BringgActiveCustomerSDKClient

class ExampleApp : Application() {
    lateinit var sdkClient: BringgActiveCustomerSDKClient

    override fun onCreate() {
        super.onCreate()
        initSdk()
    }

    private fun initSdk() {
        sdkClient = ActiveCustomerSDKFactory.init(
            this,
            ExamplePermissionVerifier(),
            ExampleNotificationProvider(this)
        )
    }
}