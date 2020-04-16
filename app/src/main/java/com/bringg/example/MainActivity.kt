package com.bringg.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import driver_sdk.account.LoginCallback
import driver_sdk.account.LoginError
import driver_sdk.account.LoginMerchant
import driver_sdk.account.oidc.OpenIdConnectLoginConfig
import driver_sdk.content.ResultCallback
import driver_sdk.customer.*
import driver_sdk.tasks.start.StartTaskResult
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    val TAG = "CustomerActions"

    private val customerActions by lazy {
        initCustomerActions()
    }

    private fun initCustomerActions(): ActiveCustomerActions {
        val settings = SdkSettings.Builder()
            .autoArriveByLocation(true)
            .autoLeaveByLocation(true)
        return sdkClient.getActiveCustomerActions(settings.build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.button_login -> initiateQrScan()

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

            R.id.button_get_active_task -> {
                val task = customerActions.getActiveTask()
                onActionSuccess("active task = $task")
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

    private fun initiateQrScan() {
        IntentIntegrator(this).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            loginWithQR(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [$requestCode], permissions = [$permissions], grantResults = [$grantResults]")
    }

    private fun loginWithQR(result: String) {
        val json = JSONObject(result)
        val token = json.optString("token")
        val secret = json.optString("secret")
        val region = json.optString("region")

        customerActions.login(token, secret, region, object : LoginCallback {
            override fun onLoginSuccess() {
                onActionSuccess("onLoginSuccess()")
            }

            override fun onLoginMultipleResults(merchants: MutableList<LoginMerchant>) {
                onActionFailure("onLoginMultipleResults() called with: merchants = [$merchants]")
            }

            override fun onLoginFailed(error: LoginError) {
                onActionFailure("onLoginFailed() called with: error = [$error]")
            }

            override fun onShouldLoginWithSSO(config: OpenIdConnectLoginConfig) {
                onActionFailure("onShouldLoginWithSSO() called with: config = [$config]")
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
}

val Activity.sdkClient get() = (application as ExampleApp).sdkClient
fun EditText.number() = text.toString().toLongOrNull() ?: 0
