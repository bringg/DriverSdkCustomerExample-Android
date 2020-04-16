package com.bringg.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import driver_sdk.PermissionVerifier
import driver_sdk.PermissionVerifier.OnPermissionsResultListener

class ExamplePermissionVerifier : PermissionVerifier {
    val TAG = "ExamplePermissionVerifier"
    val REQUEST_CODE = 555

    override fun requestPermissionWithResult(
        context: Context,
        requestStringId: Int,
        resultListener: OnPermissionsResultListener?,
        vararg permissions: String
    ) {
        Log.d(TAG, "requestPermissionWithResult() called with: context = [$context], requestStringId = [$requestStringId], resultListener = [$resultListener], permissions = [$permissions]")
    }

    override fun getPendingLocationPermissions(context: Context): Array<String> {
        Log.d(TAG, "getPendingLocationPermissions() called with: context = [$context]")
        return getPendingPermissions(context, *getLocationPermissions())
    }

    override fun requestPermission(context: Context, requestStringId: Int, vararg permissions: String) {
        Log.d(TAG, "requestPermission() called with: context = [$context], requestStringId = [$requestStringId], permissions = [$permissions]")
        try {
            ActivityCompat.requestPermissions((context as Activity),
                permissions.filter { permission -> !isGranted(context, permission) }.toTypedArray(), REQUEST_CODE)
        } catch (e: Exception) {
            Log.w(TAG, "requestPermission: ${e.message}", e)
        }
    }

    private fun getLocationPermissions(): Array<String> {
        val permissionList = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return permissionList.toTypedArray()
    }

    private fun getPendingPermissions(
        context: Context,
        vararg permissions: String
    ): Array<String> {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            emptyArray()
        else permissions.filter { permission -> !isGranted(context, permission) }.toTypedArray()
    }

    private fun isGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}