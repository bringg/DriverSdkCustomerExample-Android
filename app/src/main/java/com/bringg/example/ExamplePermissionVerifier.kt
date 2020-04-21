package com.bringg.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionVerifier {
    private val TAG = "ExamplePermissionVerifier"
    private val REQUEST_CODE = 555

    fun requestLocationPermissions(activity: Activity) {
        try {
            val missingPermissions = getLocationPermissions().filter { permission -> !isGranted(activity, permission) }
            if (missingPermissions.isEmpty())
                return

            ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CODE)
        } catch (e: Exception) {
            Log.w(TAG, "requestPermission: ${e.message}", e)
        }
    }

    private fun getLocationPermissions(): List<String> {
        val permissionList = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return permissionList
    }

    private fun isGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}