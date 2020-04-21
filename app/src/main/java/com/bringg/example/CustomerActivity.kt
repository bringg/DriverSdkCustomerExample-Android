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

        // when user already logged in we can skip login
        if (customerActionsSDK.isLoggedIn()) {
            showLoggedInUI()
        } else {
            showLoginUI()
        }
    }

    /**
     * Log in user to Bringg platform
     * this has to be done before all other SDK calls
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
     */
    private fun login() {
        customerActionsSDK.login("ac77c2d3-6555-461b-9b61-4444ecbb9f19", "72f0ae3d-d1aa-436e-b117-2d04159849f9", "us-east-1", object : ResultCallback<Boolean> {
            override fun onResult(result: Boolean) {
                // true = successful login, false = login error.
                if (result)
                    showLoggedInUI()
                else
                    current_state.text = "Login error - login failed, check logs for reason"
            }
        })
    }

    /**
     * Be Online will start the SDK background services, location and ETA tracking and network communication to Bringg cloud services.
     * Calling this has to be done after user is logged in to Bringg platform see #login() for login example.
     * After user is online active order will be fetched automatically from Bringg cloud services
     * Bringg SDK will be now monitoring locations, beacons, events, orders, etc.
     * SDK monitoring will stop after the active order is done or after calling #customerActionsSDK.beOffline
     * This should be called when the user is starting the journey to the pickup destination.
     * customerActionsSDK expose active user order live data used to display order UI and perform manual actions (start/arrive/left)
     */
    private fun becomeOnline() {
        // observe active user order, display order UI and perform manual actions (start/arrive/left)
        customerActionsSDK.activeTaskLiveData().observe(this, Observer { onActiveTaskChanged(it) })

        customerActionsSDK.beOnline(object : ResultCallback<ConnectResult> {
            override fun onResult(result: ConnectResult) {
                when (result) {
                    ConnectResult.SUCCESS -> showOnlineUI()
                    else -> current_state.text = "Error trying to get online, check logs for reason, result=${result.name}"
                }
            }
        })
    }

    /**
     * This is a simple implementation observing the current active order for current online user
     * Observer is registered to customerActionsSDK.activeTaskLiveData to get user active order
     * see #becomeOnline for observer registration example
     */
    private fun onActiveTaskChanged(task: Task?) {
        waypoints_container.removeAllViews()
        if (task == null) {
            showOnlineUI();
        } else {
            current_state.text = "Got active order, order status=${task.status}"
            task.wayPoints.forEach { waypoints_container.addView(WaypointView.newInstance(this, task, it.id)) }
            if (!task.isStarted)
                showStartOrderUI(task)
            else if (task.currentWayPoint.isCheckedIn)
                showArrivedUI(task)
            else if (task.isDone)
                showOrderDoneUI()
            else
                showLeftDestinationUI(task)
        }
    }

    /**
     * When a new order is created and assigned to the user first step on the order lifecycle is start order
     * This call will notify that the user has started his journey to the first destination of this order and after the order
     */
    private fun startOrder(task: Task) {
        customerActionsSDK.startTask(task.getId(), object : ResultCallback<StartTaskResult> {
            override fun onResult(result: StartTaskResult) {
                if (StartTaskResult.SUCCESS == result) {
                    showOrderStartedUI(task)
                } else {
                    current_state.text = "Error trying to start order, check logs for reason, result=${result.name}"
                }
            }
        })
    }

    /**
     * After successfully starting the order (see #startOrder) you can manually notify your arrival to the destination
     * When SdkSetting is configured with autoArriveByLocation = true arriving to the destination will be done
     * automatically by SDK monitoring and tracking services.
     * Manual action could be used otherwise or as a fallback.
     */
    private fun arrive(task: Task) {
        val result = customerActionsSDK.arriveToWaypoint(task.getId())
        if (result.result == TaskActionConstants.ACTION_SUCCESS) {
            showArrivedUI(task)
        } else {
            current_state.text = "Error trying to arrive to waypoint ${task.currentWayPoint.id}, check logs for reason, result=${result.result}"
        }
    }

    /**
     * After the user has arrived to the destination, leaving the destination will complete the current journey.
     * Order with a single destination will now be done, orders with multiple destinations will proceed to the next destination.
     */
    private fun leave(task: Task) {
        val result = customerActionsSDK.leaveWaypoint(task.getId())
        if (result.success()) {
            showLeftDestinationUI(task)
        } else {
            current_state.text = "Error trying to leave waypoint ${task.currentWayPoint.id}, check logs for reason, result=${result}"
        }
    }

    private fun showLoginUI() {
        button_next_customer_action.text = "Login to Bringg"
        button_next_customer_action.setOnClickListener {
            current_state.text = "Logging in to Bringg..."
            login()
        }
    }

    private fun showLoggedInUI() {
        current_state.text = "Logged in to Bringg"
        button_next_customer_action.text = "Become Online"
        button_next_customer_action.setOnClickListener {
            current_state.text = "Starting SDK services"
            becomeOnline()
        }
    }

    private fun showOnlineUI() {
        // SDK requires location permissions to track gps locations
        PermissionVerifier.requestLocationPermissions(this)
        current_state.text = "Online, all monitoring services are on, waiting for order"
    }

    private fun showStartOrderUI(task: Task) {
        button_next_customer_action.text = "Start Order"
        button_next_customer_action.setOnClickListener {
            startOrder(task)
        }
    }

    private fun showOrderStartedUI(task: Task) {
        current_state.text = "Started active waypoint,\n" +
                "destination=${task.currentWayPoint.address}\n" +
                "order status=${task.status}\n" +
                "waypoint status=${task.currentWayPoint.state}"
        button_next_customer_action.text = "I Arrived!!"
        button_next_customer_action.setOnClickListener {
            arrive(task)
        }
    }

    private fun showArrivedUI(task: Task) {
        current_state.text = "Arrived to destination"
        button_next_customer_action.text = "I've Left the destination"
        button_next_customer_action.setOnClickListener {
            leave(task)
        }
    }

    private fun showLeftDestinationUI(task: Task) {
        current_state.text = "Left destination, order is done=${task.isDone}"
        if (task.isDone || task.currentWayPoint == null) {
            button_next_customer_action.setOnClickListener(null)
            button_next_customer_action.isEnabled = false
            button_next_customer_action.text = "Order Done"
        } else {
            showOrderStartedUI(task)
        }
    }

    private fun showOrderDoneUI() {
        button_next_customer_action.text = "Order Done"
        button_next_customer_action.setOnClickListener(null)
    }

    override fun onTaskArrived() {
        current_state.text = "Arrived to destination"
    }

    override fun onTaskDone() {
        current_state.text = "Order done"
    }
}
