package com.bringg.example.callback

import android.util.Log
import driver_sdk.ErrorCallback
import driver_sdk.connection.http.models.WrappedRequest
import driver_sdk.models.ResponseData
import java.io.IOException

class EmptyErrorCallback : ErrorCallback {
    val TAG = "EmptyApiErrorCallback"

    override fun onConnectionError(p0: IOException, p1: WrappedRequest?) {
        Log.d(TAG, "onConnectionError() called with: p0 = [$p0], p1 = [$p1]")
    }

    override fun onApiError(p0: ResponseData?) {
        Log.d(TAG, "onApiError() called with: p0 = [$p0]")
    }

    override fun onAccessNotAllowed(p0: ResponseData?) {
        Log.d(TAG, "onAccessNotAllowed() called with: p0 = [$p0]")
    }

    override fun onApiThrottle(p0: String?) {
        Log.d(TAG, "onApiThrottle() called with: p0 = [$p0]")
    }

    override fun onHttpError(p0: WrappedRequest?, p1: Int, p2: String?, p3: Int?): Boolean {
        Log.d(TAG, "onHttpError() called with: p0 = [$p0], p1 = [$p1], p2 = [$p2], p3 = [$p3]")
        return false
    }

    override fun onError(p0: String?, p1: String?) {
        Log.d(TAG, "onError() called with: p0 = [$p0], p1 = [$p1]")
    }
}