package com.bringg.example

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.bringg.example.databinding.ActivityCustomerBinding
import com.bringg.example.ui.MapFragment
import com.bringg.example.ui.VehicleDetailsFragment
import com.bringg.example.ui.WaypointView
import com.google.android.material.snackbar.Snackbar
import driver_sdk.ActiveCustomerSdkFactory
import driver_sdk.content.ResultCallback
import driver_sdk.customer.ActiveCustomerActions
import driver_sdk.customer.CustomerVehicle
import driver_sdk.customer.SdkSettings
import driver_sdk.logging.BringgLog
import driver_sdk.models.Task
import driver_sdk.models.TaskState
import driver_sdk.models.TaskStates
import driver_sdk.models.enums.LoginResult
import driver_sdk.models.enums.LogoutResult
import driver_sdk.tasks.ArriveWaypointResult
import driver_sdk.tasks.LeaveWaypointResult
import driver_sdk.tasks.start.StartTaskResult
import java.util.*

class CustomerActivity : AppCompatActivity() {

    private val TAG = "CustomerActivity"

    private lateinit var binding: ActivityCustomerBinding

    // sdk instance
    private lateinit var customerActionsSDK: ActiveCustomerActions

    // state live data:
    private lateinit var loggedInLiveData: LiveData<Boolean>
    private lateinit var onlineLiveData: LiveData<Boolean>
    private lateinit var activeTaskLiveData: LiveData<TaskState>

    // option menu items
    private var menuLogout: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        PermissionVerifier.requestLocationPermissions(this)

        if (BuildConfig.DEBUG) BringgLog.enableLogcatLog()

        // configure your sdk implementation
        val builder = SdkSettings.Builder()
            .autoArriveByLocation(SdkSettings.FeatureMode.ENABLED)
            .autoLeaveByLocation(SdkSettings.FeatureMode.ENABLED)

        if (BuildConfig.IS_AUTOMATION) {
            val url = intent.extras?.getString("url")
            val realtime = intent.extras?.getString("realtime")
            if (url != null && realtime != null)
                builder.urls(url, realtime)
        }

        // initialize the sdk
        val sdkInstance = ActiveCustomerSdkFactory.init(this, ExampleNotificationProvider(this), builder.build())
        customerActionsSDK = sdkInstance.getActiveCustomerActions()

        // take reference to state live data objects:
        loggedInLiveData = sdkInstance.isLoggedIn()
        onlineLiveData = sdkInstance.isOnline()
        activeTaskLiveData = sdkInstance.activeTask()

        // observe login state
        loggedInLiveData.observe(this, { isLoggedIn -> onLoginStateChanged(isLoggedIn) })
        // observe online state
        onlineLiveData.observe(this, { isOnline -> onOnlineStateChanged(isOnline) })
        // observe active user order, display order UI and perform manual actions (start/arrive/left)
        activeTaskLiveData.observe(this, { onActiveOrderChanged(it) })

        binding.loginLayout.buttonLogin.setOnClickListener { login() }
        binding.loggedInLayout.buttonStartTask.setOnClickListener {
            val taskId = binding.loggedInLayout.inputStartTaskId.editText?.text.toString().toLongOrNull()
            if (taskId == null) {
                binding.loggedInLayout.inputStartTaskId.error = "Not a valid task_id"
                return@setOnClickListener
            }
            startOrderById(taskId)
        }
        binding.orderLayout.buttonOpenMap.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.activity_root_container, MapFragment::class.java, null, "map_fragment")
                .addToBackStack("map").commit()
        }

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
        val token = binding.loginLayout.textInputToken.editText?.text.toString()
        val secret = binding.loginLayout.textInputSecret.editText?.text.toString()
        var region = binding.loginLayout.textInputRegion.editText?.text.toString()

        if (hasMissingData(token, secret, region)) return

        if (BuildConfig.IS_AUTOMATION) region = "local"

        binding.currentStateText.text = "Logging in to Bringg..."
        binding.loginLayout.loginProgress.visibility = View.VISIBLE
        customerActionsSDK.login(token, secret, region, object : ResultCallback<LoginResult> {
            override fun onResult(result: LoginResult) {
                binding.loginLayout.loginProgress.visibility = View.GONE
                Log.i(TAG, "login result=$result, success=${result.success()}")
                if (!result.success())
                    binding.currentStateText.text = "Login error - login failed, check logs for reason"
            }
        })
    }

    private fun startOrder(task: Task) {
        startOrderById(task.getId())
    }

    /**
     * Start an order
     * After a new order is created and assigned to the user first step on the order lifecycle is start order.
     * This call will notify that the user has started his journey to the first destination of this order.
     * The SDK will fetch the order data from Bringg platform and start tracking the order progress.
     * After the order is fetched successfully active task live data events will start being fired with the current order state and data
     * Once the order is successfully started the order state will be ON_THE_WAY_TO_FIRST_DESTINATION
     */
    private fun startOrderById(taskId: Long) {
        customerActionsSDK.startTask(taskId, object : ResultCallback<StartTaskResult> {
            override fun onResult(result: StartTaskResult) {
                Log.i(TAG, "order start result=$result, success=${result.success()}")
                if (result.success()) {
                    clearTaskIdEditText()
                    binding.currentStateText.text = "Started order, result=${result.result.name()}"
                } else {
                    binding.loggedInLayout.inputStartTaskId.error = "Error starting order, start result is ${result.result.name()}"
                    binding.currentStateText.text = "Error trying to start order, check logs for errors, result=${result.result.name()}"
                }
            }
        })
    }

    private fun showVehicleDetailsView(task: Task) {
        val dialog = VehicleDetailsFragment()
        dialog.onResult = { vehicle -> arrive(task, vehicle) }
        dialog.show(supportFragmentManager, VehicleDetailsFragment.TAG)
    }

    /**
     * Arrived to destination
     * After successfully starting the order (see #startOrder) you can manually notify your arrival to the destination
     * When SdkSetting is configured with autoArriveByLocation = true arriving to the destination will be done
     * automatically by the SDK monitoring and tracking services.
     * Manual action could be used otherwise or as a fallback.
     * After successfully arriving to a destination active live data event will be fired with ON_THE_WAY_TO_FIRST_DESTINATION state
     */
    private fun arrive(task: Task, vehicle: CustomerVehicle? = null) {
        Log.d(TAG, "arrive() called with: task = [${task.getId()}], vehicle = [$vehicle]")
        if (vehicle == null) {
            customerActionsSDK.arriveToWaypoint(object : ResultCallback<ArriveWaypointResult> {
                override fun onResult(result: ArriveWaypointResult) {
                    onArriveToWaypointResult(task, result)
                }
            })
        } else {
            customerActionsSDK.arriveToWaypoint(vehicle, object : ResultCallback<ArriveWaypointResult> {
                override fun onResult(result: ArriveWaypointResult) {
                    onArriveToWaypointResult(task, result)
                }
            })
        }
    }

    private fun onArriveToWaypointResult(task: Task, result: ArriveWaypointResult) {
        Log.i(TAG, "arrive result=$result, success=${result.success()}")
        if (result.success()) {
            showArrivedUI(task)
        } else {
            binding.currentStateText.text =
                "Error trying to arrive to waypoint ${task.currentWayPointId}, check logs for reason, result=${result}"
        }
    }

    /**
     * Left current destination:
     * After the user has arrived to the destination, leaving the destination will complete the current journey.
     * Order with a single destination will now be done, orders with multiple destinations will proceed to the next destination.
     * When SdkSetting is configured with autoLeaveByLocation = true leaving to the destination will be done
     * automatically by the SDK monitoring and tracking services.
     * Manual action could be used otherwise or as a fallback.
     * After successfully leaving a destination active live data event will be fired with ON_THE_WAY_TO_NEXT_DESTINATION state when there are more
     * destinations for this order or DONE state followed by NO_TASK state
     */
    private fun leave(task: Task) {
        customerActionsSDK.leaveWaypoint(object : ResultCallback<LeaveWaypointResult> {
            override fun onResult(result: LeaveWaypointResult) {
                Log.i(TAG, "leave result=$result, success=${result.success()}")
                if (!result.success()) {
                    binding.currentStateText.text =
                        "Error trying to leave waypoint ${task.currentWayPointId}, check logs for reason, result=${result}"
                }
            }
        })
    }

    // region state observing
    private fun onLoginStateChanged(loggedIn: Boolean) {
        if (loggedIn) {
            onOnlineStateChanged(onlineLiveData.value!!)
        } else {
            showLoginUI()
        }
    }

    private fun onOnlineStateChanged(online: Boolean) {
        if (online) {
            onActiveOrderChanged(activeTaskLiveData.value!!)
        } else {
            if (true == loggedInLiveData.value)
                showLoggedInUI()
            else
                showLoginUI()
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
        Log.i(TAG, "order updated, state=${taskState.currentState} order=$task")
        onOrderStateChangedEvent(task)
        when (taskState.currentState) {
            TaskStates.NO_TASK -> showNoActiveOrder()
            TaskStates.NEW_TASK -> showStartOrderUI(task!!)
            TaskStates.ON_THE_WAY_TO_FIRST_DESTINATION -> showOrderStartedUI(task!!)
            TaskStates.ARRIVED_AT_CURRENT_DESTINATION -> showArrivedUI(task!!)
            TaskStates.ON_THE_WAY_TO_NEXT_DESTINATION -> showLeftDestinationUI(task!!) // only happens on multiple destination orders
            TaskStates.DONE -> showOrderDoneUI(task!!)
            TaskStates.CANCELED -> {
                showOnlineUI()
                Snackbar.make(
                    binding.viewFlipper,
                    "Order #${task!!.externalId} was canceled",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    // endregion state observing

    // region UI changes
    private fun onOrderStateChangedEvent(task: Task?) {
        binding.orderLayout.taskState.text =
            if (task == null) "Waiting for active task..." else "Order Status: ${
                TaskStatusMap.getUserStatus(
                    task.status
                ).toUpperCase(Locale.US)
            } (${task.status})\nactive waypoint Id=${task.currentWayPointId}"

        binding.orderLayout.waypointsContainer.removeAllViews()
        task?.wayPoints?.forEach { waypoint ->
            val waypointView = WaypointView.newInstance(this)
            waypointView.refresh(task, waypoint)
            binding.orderLayout.waypointsContainer.addView(waypointView)
        }
    }

    private fun showNoActiveOrder() {
        if (false == loggedInLiveData.value) {
            showLoginUI()
        } else if (false == onlineLiveData.value) {
            showLoggedInUI()
        } else {
            showOnlineUI()
        }
    }

    private fun showLoginUI() {
        binding.viewFlipper.displayedChild = 0
        showNoOrderUI()
        binding.currentStateText.text = "Logged out from Bringg"
    }

    private fun showLoggedInUI() {
        binding.viewFlipper.displayedChild = 1
        showNoOrderUI()
        binding.currentStateText.text = "Logged in to Bringg"
    }

    private fun showOnlineUI() {
        binding.viewFlipper.displayedChild = 1
        showNoOrderUI()
        if (true == onlineLiveData.value)
            binding.currentStateText.text = "all monitoring services are on, waiting for active order"
        else
            binding.currentStateText.text = "Logged in to Bringg"

        binding.orderLayout.currentState.text = "no active order"
    }

    private fun showStartOrderUI(task: Task) {
        showActiveOrderUI()
        binding.currentStateText.text = "Got active order"
        binding.orderLayout.currentState.text = "Online, active order not started"
        binding.orderLayout.buttonNextOrderAction.text = "Start Order"
        binding.orderLayout.buttonNextOrderAction.setOnClickListener {
            startOrder(task)
        }
    }

    private fun showOrderStartedUI(task: Task) {
        showActiveOrderUI()
        binding.currentStateText.text = "Started active waypoint"
        binding.orderLayout.currentState.text = "Online, active order is started"
        binding.orderLayout.buttonNextOrderAction.text = "Arrived Destination"
        binding.orderLayout.buttonNextOrderAction.setOnClickListener {
            showVehicleDetailsView(task)
        }
    }

    private fun showArrivedUI(task: Task) {
        showActiveOrderUI()
        binding.currentStateText.text = "Arrived to destination"
        binding.orderLayout.currentState.text = "Online, arrived at current destination"
        binding.orderLayout.buttonNextOrderAction.text = "Order Collected"
        binding.orderLayout.buttonNextOrderAction.setOnClickListener {
            leave(task)
        }
    }

    private fun showLeftDestinationUI(task: Task) {
        showActiveOrderUI()
        binding.currentStateText.text = "Left destination, order is done=${task.isDone}"
        binding.orderLayout.currentState.text = "Online, left current destination"
        if (task.isDone || task.currentWayPoint == null) {
            showOrderDoneUI(task)
        } else {
            showOrderStartedUI(task)
        }
    }

    private fun showOrderDoneUI(task: Task) {
        showActiveOrderUI()
        binding.viewFlipper.displayedChild = 1
        binding.currentStateText.text = "Order is done"
        binding.orderLayout.currentState.text = "Online, active order is done"
        Snackbar.make(binding.viewFlipper, "Order #${task.externalId} is done", Snackbar.LENGTH_LONG).show()
    }

    private fun showActiveOrderUI() {
        binding.viewFlipper.displayedChild = 2
        refreshMenuItemsVisibility()
    }

    private fun showNoOrderUI() {
        refreshMenuItemsVisibility()
    }

    private fun clearTaskIdEditText() {
        binding.loggedInLayout.inputStartTaskId.error = null
        binding.loggedInLayout.inputStartTaskId.editText?.text = null
    }

    private fun hasMissingData(token: String, secret: String, region: String): Boolean {
        var hasError = false
        if (token.isBlank()) {
            binding.loginLayout.textInputToken.error = "Token is mandatory"
            hasError = true
        } else {
            binding.loginLayout.textInputToken.error = null
        }
        if (secret.isBlank()) {
            binding.loginLayout.textInputSecret.error = "Secret is mandatory"
            hasError = true
        } else {
            binding.loginLayout.textInputSecret.error = null
        }
        if (region.isBlank()) {
            binding.loginLayout.textInputRegion.error = "Region is mandatory"
            hasError = true
        } else {
            binding.loginLayout.textInputRegion.error = null
        }
        if (hasError)
            return true
        return false
    }
    // endregion UI changes

    // region options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.customer_activity, menu)
        menuLogout = menu?.findItem(R.id.menu_item_logout)
        menuLogout?.setOnMenuItemClickListener {
            customerActionsSDK.logout(object : ResultCallback<LogoutResult> {
                override fun onResult(result: LogoutResult) {
                    Log.i(TAG, "logout result=$result, success=${result.success()}")
                    if (!result.success()) {
                        binding.currentStateText.text = "Error trying to logout, check logs for reason, result=${result.name()}"
                    }
                }
            })
            showNoActiveOrder()
            true
        }
        refreshMenuItemsVisibility()
        return true
    }

    private fun refreshMenuItemsVisibility() {
        menuLogout?.isVisible = true == loggedInLiveData.value
    }
    // endregion options menu
}
