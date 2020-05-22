package com.bringg.example

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import driver_sdk.ActiveCustomerSDKFactory
import driver_sdk.content.ResultCallback
import driver_sdk.customer.ActiveCustomerActions
import driver_sdk.customer.SdkSettings
import driver_sdk.models.enums.LoginResult
import driver_sdk.models.enums.LogoutResult
import driver_sdk.tasks.ArriveWaypointResult
import driver_sdk.tasks.LeaveWaypointResult
import driver_sdk.tasks.start.StartTaskResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val TAG = "CustomerActions"

    private val customerActions by lazy {
        initCustomerActions()
    }

    private fun initCustomerActions(): ActiveCustomerActions {
        // configure your sdk implementation
        val builder = SdkSettings.Builder()

        // initialize the sdk
        val sdkInstance =
            ActiveCustomerSDKFactory.init(this, ExampleNotificationProvider(this), builder.build())
        return sdkInstance.getActiveCustomerActions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.button_login -> login()

            R.id.button_logout -> customerActions.logout(object : ResultCallback<LogoutResult> {
                override fun onResult(result: LogoutResult) {
                    if (result == LogoutResult.SUCCESS)
                        onActionSuccess("Logged out from Bringg")
                    else
                        onActionFailure("Failed to logout (error = $result)")
                }
            })

            R.id.button_start_task -> {
                customerActions.startTask(
                    taskIdEditText.number(),
                    object : ResultCallback<StartTaskResult> {
                        override fun onResult(result: StartTaskResult) {
                            if (result.success())
                                onActionSuccess("Task $taskId started")
                            else
                                onActionFailure("Task $taskId failed to start (error = $result)")
                        }
                    })
            }

            R.id.button_waypoint_arrive -> customerActions.arriveToWaypoint(object : ResultCallback<ArriveWaypointResult> {
                override fun onResult(result: ArriveWaypointResult) {
                    if (result.success())
                        onActionSuccess("Task $taskId arrived")
                    else
                        onActionFailure("Task $taskId failed to arrive (error = $result)")
                }
            })

            R.id.button_waypoint_leave -> customerActions.leaveWaypoint(object : ResultCallback<LeaveWaypointResult> {
                override fun onResult(result: LeaveWaypointResult) {
                    if (result.success())
                        onActionSuccess("Task $taskId left")
                    else
                        onActionFailure("Task $taskId failed to leave (error = $result)")
                }
            })
        }
    }

    private fun login() {
        customerActions.login(
            "e9896364-e545-42c5-80f8-53107b7c3df8",
            "3ea3d8fe-443a-488f-b024-3adb61f762e6",
            "us-east-1",
            object : ResultCallback<LoginResult> {
                override fun onResult(result: LoginResult) {
                    // true = successful login, false = login error.
                    if (result.success())
                        onActionSuccess("onLoginSuccess()")
                    else
                        onActionFailure("onLoginFailed() - check the log for failure reason")
                }
            })
    }

    private fun onActionSuccess(text: String) {
        Log.i(TAG, text)
        ExampleToast.makeText(this, text, R.drawable.ic_success, Toast.LENGTH_LONG).show()
    }

    private fun onActionFailure(text: String) {
        Log.e(TAG, text)
        ExampleToast.makeText(this, text, R.drawable.ic_error, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(
            TAG,
            "onRequestPermissionsResult() called with: requestCode = [$requestCode], permissions = [$permissions], grantResults = [$grantResults]"
        )
    }
}

fun EditText.number() = text.toString().toLongOrNull() ?: 0
