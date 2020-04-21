package com.bringg.example.ui

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bringg.example.R
import com.bumptech.glide.Glide
import driver_sdk.models.Task
import driver_sdk.models.Waypoint
import kotlinx.android.synthetic.main.fragment_waypoint_list_header.view.*
import kotlinx.android.synthetic.main.layout_way_point_title.view.*
import kotlinx.android.synthetic.main.waypoint_time_window_layout.view.*

/**
 * TODO: document your custom view class.
 */
class WaypointView(context: Context, private val task: Task, waypointId: Long) : CardView(context) {
    private var inventoryLayout: InventoryPricingLayout
    private var waypoint = task.getWayPointById(waypointId)
    private var addressText: TextView
    private var secondLineAddress: TextView
    private var customerAddressType: TextView
    private var customerAddressName: TextView
     private var headerView: View

    init {
        useCompatPadding = true
        val inflater = LayoutInflater.from(context)
        val rootView: View = inflater.inflate(R.layout.fragment_way_point, this)
        headerView = inflater.inflate(R.layout.fragment_waypoint_list_header, this)
        addressText = headerView.findViewById(R.id.way_point_address)
        secondLineAddress = headerView.findViewById(R.id.way_point_second_address)
        customerAddressType = headerView.findViewById(R.id.customer_address_type)
        customerAddressName = headerView.findViewById(R.id.customer_address_name)
        val waypointDescriptionLayout = headerView.findViewById<LinearLayout>(R.id.waypoint_description_layout)
        val waypointDescriptionText = waypointDescriptionLayout.findViewById<TextView>(R.id.way_point_description_text)
        waypointDescriptionText.text = task.title
        inventoryLayout = InventoryPricingLayout(context)
        inventoryLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        inventory_list.addView(inventoryLayout)
        refresh()
    }

    private fun updateCustomer(waypoint: Waypoint) {
        val customer = waypoint.customer
        customer_name.text = customer.getName()
        Glide.with(this).load(customer.imageUrl).into(customer_image);
    }

    private fun updateTimeWindowDetails() {
        scheduled_for_text.text = waypoint.scheduledAt
        time_window_start.text = waypoint.noEarlierThan
        time_window_end.text = waypoint.noLaterThan
        eta_text.text = waypoint.eta
    }

    private fun refresh() {
        updateTimeWindowDetails()
        updateInventoryList()
        updateAddress()
        waypoint?.let { updateCustomer(it) }
    }

    private fun updateAddress() {
        val sb = getFormattedAddressText()
        addressText.text = sb.toString()
        secondLineAddress.text = waypoint.getSecondLineAddress()
        customerAddressType.text = waypoint.getAddressType().name
        customerAddressName.text = waypoint.getLocationName()
    }

    private fun getFormattedAddressText(): StringBuilder {
        val sb = StringBuilder()
        sb.append(waypoint.houseNumber).append(" ")
        sb.append(waypoint.address)

        if (!TextUtils.isEmpty(waypoint.borough)) {
            sb.append(", ").append(waypoint.borough)
        }
        if (!TextUtils.isEmpty(waypoint.city)) {
            sb.append(", ").append(waypoint.city)
        }
        if (!TextUtils.isEmpty(waypoint.state)) {
            sb.append(", ").append(waypoint.state)
        }
        if (!TextUtils.isEmpty(waypoint.zipCode)) {
            sb.append(", ").append(waypoint.zipCode)
        }
        return sb
    }

    private fun updateInventoryList() {
        inventoryLayout.setData(task, waypoint)
        inventoryLayout.setDeliveryFee(task.deliveryPrice)
        inventoryLayout.setTotal(task.totalPrice)
        inventoryLayout.setToBePaid(task.leftToBePaid)
    }

    companion object {
        const val TAG = "WayPointFragment"
        fun newInstance(context: Context, task: Task, way_point_id: Long): WaypointView {
            return WaypointView(context, task, way_point_id)
        }
    }
}
