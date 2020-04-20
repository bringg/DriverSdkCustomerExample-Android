package com.bringg.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bringg.example.ui.WaypointView
import driver_sdk.ActiveCustomerSDKFactory
import driver_sdk.content.ResultCallback
import driver_sdk.customer.ActiveCustomerActions
import driver_sdk.customer.ActiveCustomerTaskStateListener
import driver_sdk.customer.ConnectResult
import driver_sdk.customer.SdkSettings
import driver_sdk.models.Task
import driver_sdk.models.TaskActionConstants
import driver_sdk.tasks.start.StartTaskResult
import kotlinx.android.synthetic.main.activity_customer.*

class CustomerActivity : AppCompatActivity(), ActiveCustomerTaskStateListener {

    private lateinit var customerActionsSDK: ActiveCustomerActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)
        setSupportActionBar(toolbar)

        // configure your sdk implementation
        val builder = SdkSettings.Builder()
            .autoArriveByLocation(true)
            .autoLeaveByLocation(true)

        // initialize the sdk
        val sdkInstance = ActiveCustomerSDKFactory.init(this, ExampleNotificationProvider(this), builder.build())
        customerActionsSDK = sdkInstance.getActiveCustomerActions()

        if (customerActionsSDK.isLoggedIn()) {
            onSuccessfulLogin()
        } else {
            button_next_customer_action.text = "Login to Bringg"
            button_next_customer_action.setOnClickListener {
                login()
            }
        }
    }

    private fun login() {
        current_state.text = "Logging in to Bringg..."

        // login tokens should be generated using:
        // https://developers.bringg.com/reference#generate-customer-one-time-code
        // response looks like this:
        //            {
        //               "success": true,
        //                "rc": 0,
        //                "access_token": "92f5b501-72a8-4ac9-a75f-6800191bc5cf",
        //                "secret": "e0fca3a3-9725-4ddb-b659-6b1d64f613af",
        //                "region": "us-east-1"
        //            }

        customerActionsSDK.login("17952066-f1a7-456c-a794-e1c3b8005691", "524680ec-a2e5-42ac-8528-ad65c0771539", "us-east-1", object : ResultCallback<Boolean> {
            override fun onResult(result: Boolean) {
                // true = successful login, false = login error.
                if (result)
                    onSuccessfulLogin()
                else
                    current_state.text = "Login error - login failed, check logs for reason"
            }
        })
    }

    private fun onSuccessfulLogin() {
        current_state.text = "Logged in to Bringg"

        customerActionsSDK.activeTaskLiveData().observe(this, Observer { onActiveTaskChanged(it) })

        button_next_customer_action.text = "Become Online"
        button_next_customer_action.setOnClickListener { becomeOnline() }
    }

    private fun onActiveTaskChanged(task: Task?) {
        waypoints_container.removeAllViews()
        if (task == null) {
            onBecomeOnline();
        } else {
            current_state.text = "Got active order, order status=${task.status}"
            task.wayPoints.forEach { waypoints_container.addView(WaypointView.newInstance(this, task, it.id)) }
            button_next_customer_action.text = "Start Order"
            button_next_customer_action.setOnClickListener {
                startOrder(task)
            }
        }
    }

    private fun startOrder(task: Task) {
        customerActionsSDK.startTask(task.getId(), object : ResultCallback<StartTaskResult> {
            override fun onResult(result: StartTaskResult) {
                if (StartTaskResult.SUCCESS == result) {
                    onOrderStarted(task)
                } else {
                    current_state.text = "Error trying to start order, check logs for reason, result=${result.name}"
                }
            }
        })
    }

    private fun onOrderStarted(task: Task) {
        current_state.text = "Started active order, order status=${task?.status}"
        button_next_customer_action.text = "I Arrived!!"
        button_next_customer_action.setOnClickListener {
            arrive(task)
        }
    }

    private fun arrive(task: Task) {
        val result = customerActionsSDK.arriveToWaypoint(task.getId(), task.currentWayPoint.id)
        if (result.result == TaskActionConstants.ACTION_SUCCESS) {
            onArrived(task)
        } else {
            current_state.text = "Error trying to arrive to waypoint ${task.currentWayPoint.id}, check logs for reason, result=${result.result}"
        }
    }

    private fun onArrived(task: Task) {
        current_state.text = "Arrived to destination"

        val result = customerActionsSDK.leaveWaypoint(task.getId(), task.currentWayPoint.id)
        if (result.success()) {
            onLeft(task)
        } else {
            current_state.text = "Error trying to leave waypoint ${task.currentWayPoint.id}, check logs for reason, result=${result}"
        }
    }

    private fun onLeft(task: Task) {
        current_state.text = "Left destination, order is done=${task.isDone}"
    }

    private fun becomeOnline() {
        customerActionsSDK.beOnline(object : ResultCallback<ConnectResult> {
            override fun onResult(result: ConnectResult) {
                when (result) {
                    ConnectResult.SUCCESS -> onBecomeOnline()
                    else -> current_state.text = "Error trying to get online, check logs for reason, result=${result.name}"
                }
            }
        })
    }

    private fun onBecomeOnline() {
        current_state.text = "Online, all monitoring services are on, waiting for order"
    }

    override fun onTaskArrived() {
        current_state.text = "Arrived to destination"
    }

    override fun onTaskDone() {
        current_state.text = "Order done"
    }

}
