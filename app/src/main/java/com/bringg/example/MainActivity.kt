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
import driver_sdk.customer.ConnectResult
import driver_sdk.customer.DisconnectResult
import driver_sdk.customer.SdkSettings
import driver_sdk.tasks.start.StartTaskResult
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    val TAG = "CustomerActions"

    private val customerActions by lazy {
        initCustomerActions()
    }

    private fun initCustomerActions(): ActiveCustomerActions {
        // configure your sdk implementation
        val builder = SdkSettings.Builder()
            .autoArriveByLocation(true)
            .autoLeaveByLocation(true)

        // initialize the sdk
        // initialize the sdk
        val sdkInstance = ActiveCustomerSDKFactory.init(this, ExampleNotificationProvider(this), builder.build())
        return sdkInstance.getActiveCustomerActions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.button_login -> login()

            R.id.button_logout -> customerActions.logout()

            R.id.button_start_task -> {
                customerActions.startTask(
                    taskIdEditText.number(),
                    object : ResultCallback<StartTaskResult> {
                        override fun onResult(result: StartTaskResult) {
                            if (result == StartTaskResult.SUCCESS)
                                onActionSuccess("Task $taskId started")
                            else
                                onActionFailure("Task $taskId failed to start (error = $result)")
                        }
                    })
            }

            R.id.button_start_sharing_location -> customerActions.beOnline(object :
                ResultCallback<ConnectResult> {
                override fun onResult(result: ConnectResult) {
                    if (result == ConnectResult.SUCCESS)
                        onActionSuccess("onResult() called with: result = [$result]")
                    else onActionFailure("onResult() called with error = [$result]")
                }
            })

            R.id.button_stop_sharing_location -> customerActions.beOffline(object :
                ResultCallback<DisconnectResult> {
                override fun onResult(result: DisconnectResult) {
                    if (result == DisconnectResult.SUCCESS)
                        onActionSuccess("onResult() called with: result = [$result]")
                    else onActionFailure("onResult() called with error = [$result]")
                }
            })

            R.id.button_waypoint_arrive -> customerActions.arriveToWaypoint(
                taskWpIdEditText.number(),
                wpIdEditText.number()
            )

            R.id.button_waypoint_leave -> customerActions.leaveWaypoint(
                taskWpIdEditText.number(),
                wpIdEditText.number()
            )
        }
    }

    private fun login() {
        TODO("Not yet implemented")
    }


    private fun loginWithQR(result: String) {
        val json = JSONObject(result)
        val token = json.optString("token")
        val secret = json.optString("secret")
        val region = json.optString("region")

        customerActions.login(token, secret, region, object : ResultCallback<Boolean> {
            override fun onResult(result: Boolean) {
                if (result)
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
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [$requestCode], permissions = [$permissions], grantResults = [$grantResults]")
    }
}

fun EditText.number() = text.toString().toLongOrNull() ?: 0
