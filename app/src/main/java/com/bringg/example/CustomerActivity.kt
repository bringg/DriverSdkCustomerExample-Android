package com.bringg.example

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bringg.example.ui.WaypointView
import com.google.android.material.snackbar.Snackbar
import driver_sdk.ActiveCustomerSDKFactory
import driver_sdk.content.ResultCallback
import driver_sdk.customer.ActiveCustomerActions
import driver_sdk.customer.ConnectResult
import driver_sdk.customer.DisconnectResult
import driver_sdk.customer.SdkSettings
import driver_sdk.models.Task
import driver_sdk.models.TaskActionConstants
import driver_sdk.models.TaskState
import driver_sdk.models.TaskStates
import driver_sdk.tasks.start.StartTaskResult
import kotlinx.android.synthetic.main.activity_customer.*
import kotlinx.android.synthetic.main.login_layout.*
import java.util.*

class CustomerActivity : AppCompatActivity() {

    private lateinit var customerActionsSDK: ActiveCustomerActions

    private var menuBecomeOffline: MenuItem? = null
    private var menuLogout: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)
        setSupportActionBar(toolbar)

        PermissionVerifier.requestLocationPermissions(this)

        // configure your sdk implementation
        val builder = SdkSettings.Builder()
            .autoArriveByLocation(SdkSettings.FeatureMode.ENABLED)
            .autoLeaveByLocation(SdkSettings.FeatureMode.ENABLED)

        // initialize the sdk
        val sdkInstance = ActiveCustomerSDKFactory.init(this, ExampleNotificationProvider(this), builder.build())
        customerActionsSDK = sdkInstance.getActiveCustomerActions()
        // observe active user order, display order UI and perform manual actions (start/arrive/left)
        sdkInstance.activeTaskLiveData().observe(this, Observer { onActiveOrderChanged(it) })
    }

    /**
     * Log in user to Bringg platform
     * this has to be done before all other SDK calls
     * login tokens should be generated using:
     * https://developers.bringg.com/reference#generate-customer-one-time-code
     * response looks like this:
     *            {
     *               "success": true,
     *                "rc": 0,
     *                "access_token": "92f5b501-72a8-4ac9-a75f-6800191bc5cf",
     *                "secret": "e0fca3a3-9725-4ddb-b659-6b1d64f613af",
     *                "region": "us-east-1"
     *            }
     */
    private fun login() {
        val token = text_input_token.editText?.text.toString()
        val secret = text_input_secret.editText?.text.toString()
        val region = text_input_region.editText?.text.toString()
        if (token.isBlank()) {
            text_input_token.error = "Token is mandatory"
        }
        customerActionsSDK.login(token, secret, region, object : ResultCallback<Boolean> {
            override fun onResult(result: Boolean) {
                // true = successful login, false = login error.
                if (result)
                    showNoActiveOrder()
                else
                    current_state_text.text = "Login error - login failed, check logs for reason"
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
        customerActionsSDK.beOnline(object : ResultCallback<ConnectResult> {
            override fun onResult(result: ConnectResult) {
                when (result) {
                    ConnectResult.SUCCESS -> showNoActiveOrder()
                    else -> current_state_text.text = "Error trying to get online, check logs for reason, result=${result.name}"
                }
            }
        })
    }

    private fun becomeOffline() {
        customerActionsSDK.beOffline(object : ResultCallback<DisconnectResult> {
            override fun onResult(result: DisconnectResult) {
                when (result) {
                    DisconnectResult.SUCCESS -> showNoActiveOrder()
                    else -> current_state_text.text = "Error trying to get offline, check logs for reason, result=${result.name}"
                }
            }
        })
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
                    current_state_text.text = "Error trying to start order, check logs for reason, result=${result.name}"
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
        val result = customerActionsSDK.arriveToWaypoint()
        if (result.result == TaskActionConstants.ACTION_SUCCESS) {
            showArrivedUI(task)
        } else {
            current_state_text.text = "Error trying to arrive to waypoint ${task.currentWayPointId}, check logs for reason, result=${result.result}"
        }
    }

    /**
     * After the user has arrived to the destination, leaving the destination will complete the current journey.
     * Order with a single destination will now be done, orders with multiple destinations will proceed to the next destination.
     */
    private fun leave(task: Task) {
        val result = customerActionsSDK.leaveWaypoint()
        if (!result.success()) {
            current_state_text.text = "Error trying to leave waypoint ${task.currentWayPointId}, check logs for reason, result=${result}"
        }
    }

    /**
     * Handle order progress state changes from sdkInstance.activeTaskLiveData() observer:
     * This is a simple implementation observing the current active order for current online user
     * Observer is registered to customerActionsSDK.activeTaskLiveData to get user active order
     * see #becomeOnline for observer registration example
     */
    private fun onActiveOrderChanged(taskState: TaskState) {
        val task = taskState.task
        onOrderStateChangedEvent(task)
        when (taskState.currentState) {
            TaskStates.NO_TASK -> showNoActiveOrder()
            TaskStates.NEW_TASK -> showStartOrderUI(task!!)
            TaskStates.ON_THE_WAY_TO_FIRST_DESTINATION -> showOrderStartedUI(task!!)
            TaskStates.ARRIVED_AT_CURRENT_DESTINATION -> showArrivedUI(task!!)
            TaskStates.ON_THE_WAY_TO_NEXT_DESTINATION -> showLeftDestinationUI(task!!) // only happens on multiple destination orders
            TaskStates.DONE -> showOrderDoneUI(task!!)
            TaskStates.ORDER_DATA_UPDATED -> {
                // data change - refresh the UI to reflect updated data, no state change on this event
                onOrderStateChangedEvent(task!!)
                Snackbar.make(button_next_customer_action, "Order #${task.externalId} data was updated", Snackbar.LENGTH_LONG).show()
            }
            TaskStates.CANCELED -> {
                showOnlineUI()
                Snackbar.make(button_next_customer_action, "Order #${task!!.externalId} was canceled", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // region UI changes

    private fun onOrderStateChangedEvent(task: Task?) {
        task_state.text = if (task == null) "Waiting for active task..." else "Order Status: ${TaskStatusMap.getUserStatus(task.status).toUpperCase(Locale.US)} (${task.status})\nactive waypoint Id=${task.currentWayPointId}"
        waypoints_container.removeAllViews()
        task?.wayPoints?.forEach { waypoints_container.addView(WaypointView.newInstance(this, task, it.id)) }
    }

    private fun showNoActiveOrder() {
        if (!customerActionsSDK.isLoggedIn()) {
            showLoginUI()
        } else if (!customerActionsSDK.isOnline()) {
            showLoggedInUI()
        } else {
            showOnlineUI()
        }
    }

    private fun showLoginUI() {
        text_flipper.displayedChild = 0
        showNoOrderUI()
        current_state_text.text = "Logged out from Bringg"
        button_next_customer_action.text = "Login to Bringg"
        button_next_customer_action.isEnabled = true
        button_next_customer_action.setOnClickListener {
            current_state_text.text = "Logging in to Bringg..."
            login()
        }
    }

    private fun showLoggedInUI() {
        text_flipper.displayedChild = 1
        showNoOrderUI()
        logged_in_text.setText(R.string.logged_in_text)
        current_state_text.text = "Logged in to Bringg"
        button_next_customer_action.text = "Become Online"
        button_next_customer_action.isEnabled = true
        button_next_customer_action.setOnClickListener {
            current_state_text.text = "Starting SDK services"
            becomeOnline()
        }
    }

    private fun showOnlineUI() {
        text_flipper.displayedChild = 1
        logged_in_text.setText(R.string.create_order_desc)
        showNoOrderUI()
        current_state_text.text = "Online, all monitoring services are on, waiting for active order"
        current_state.text = "Online, no active order"
        button_next_customer_action.isEnabled = false
        button_next_customer_action.text = "waiting for active order..."
        button_next_customer_action.setOnClickListener(null)
    }

    private fun showStartOrderUI(task: Task) {
        showActiveOrderUI()
        current_state_text.text = "Got active order"
        current_state.text = "Online, active order not started"
        button_next_customer_action.text = "Start Order"
        button_next_customer_action.isEnabled = true
        button_next_customer_action.setOnClickListener {
            startOrder(task)
        }
    }

    private fun showOrderStartedUI(task: Task) {
        showActiveOrderUI()
        current_state_text.text = "Started active waypoint"
        current_state.text = "Online, active order is started"
        button_next_customer_action.text = "Arrived Destination"
        button_next_customer_action.isEnabled = true
        button_next_customer_action.setOnClickListener {
            arrive(task)
        }
    }

    private fun showArrivedUI(task: Task) {
        showActiveOrderUI()
        current_state_text.text = "Arrived to destination"
        current_state.text = "Online, arrived at current destination"
        button_next_customer_action.text = "Order Collected"
        button_next_customer_action.isEnabled = true
        button_next_customer_action.setOnClickListener {
            leave(task)
        }
    }

    private fun showLeftDestinationUI(task: Task) {
        showActiveOrderUI()
        current_state_text.text = "Left destination, order is done=${task.isDone}"
        current_state.text = "Online, left current destination"
        if (task.isDone || task.currentWayPoint == null) {
            button_next_customer_action.setOnClickListener(null)
            button_next_customer_action.isEnabled = false
            button_next_customer_action.text = "Order Done"
        } else {
            showOrderStartedUI(task)
        }
    }

    private fun showOrderDoneUI(task: Task) {
        showActiveOrderUI()
        text_flipper.visibility = View.VISIBLE
        text_flipper.displayedChild = 2
        current_state_text.text = "Order is done"
        current_state.text = "Online, active order is done"
        button_next_customer_action.text = "Order Done"
        button_next_customer_action.isEnabled = false
        button_next_customer_action.setOnClickListener(null)
        Snackbar.make(button_next_customer_action, "Order #${task.externalId} is done", Snackbar.LENGTH_LONG).show()
    }

    private fun showActiveOrderUI() {
        text_flipper.visibility = View.GONE
        active_order_group.visibility = View.VISIBLE
        refreshMenuItemsVisibility()
    }

    private fun showNoOrderUI() {
        text_flipper.visibility = View.VISIBLE
        active_order_group.visibility = View.GONE
        refreshMenuItemsVisibility()
    }
    // endregion UI changes

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.customer_activity, menu)
        menuBecomeOffline = menu?.findItem(R.id.menu_item_become_offline)
        menuLogout = menu?.findItem(R.id.menu_item_logout)
        refreshMenuItemsVisibility()
        menuBecomeOffline?.setOnMenuItemClickListener {
            becomeOffline()
            true
        }
        menuLogout?.setOnMenuItemClickListener {
            customerActionsSDK.logout()
            showNoActiveOrder()
            true
        }
        return true
    }

    private fun refreshMenuItemsVisibility() {
        menuBecomeOffline?.isVisible = customerActionsSDK.isOnline()
        menuLogout?.isVisible = customerActionsSDK.isLoggedIn()
    }
}
