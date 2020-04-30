package com.bringg.example.ui

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bringg.example.R
import com.bringg.example.TaskStatusMap
import com.bumptech.glide.Glide
import driver_sdk.models.Task
import driver_sdk.models.Waypoint
import kotlinx.android.synthetic.main.fragment_waypoint_list_header.view.*
import kotlinx.android.synthetic.main.layout_way_point_title.view.*
import kotlinx.android.synthetic.main.waypoint_time_window_layout.view.*
import java.util.*

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
        elevation = 3 * context.resources.displayMetrics.density
        val inflater = LayoutInflater.from(context)
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
        if (customer == null) {
            customer_name.text = "No Customer"
            customer_image.setImageDrawable(null)
            return
        }
        customer_name.text = customer.getName()
        val imageUrl = if (customer.imageUrl.isBlank() || !customer.imageUrl.startsWith("http")) "https://pngimage.net/wp-content/uploads/2018/06/happy-client-png-7.png" else customer.imageUrl
        Glide.with(this).load(imageUrl).into(customer_image)
    }

    private fun updateTimeWindowDetails() {
        if (waypoint.scheduledAt.isNullOrBlank()) {
            scheduled_for_text.visibility = View.GONE
            scheduled_for_title.visibility = View.GONE
        } else {
            scheduled_for_text.visibility = View.VISIBLE
            scheduled_for_title.visibility = View.VISIBLE
            scheduled_for_text.text = waypoint.scheduledAt
        }

        if (waypoint.noEarlierThan.isNullOrBlank() && waypoint.noLaterThan.isNullOrBlank()) {
            time_window_container.visibility = View.GONE
            time_window_title.visibility = View.GONE
        } else {
            time_window_container.visibility = View.VISIBLE
            time_window_title.visibility = View.VISIBLE
            if (waypoint.noEarlierThan.isNullOrBlank()) {
                time_window_start.visibility = View.GONE
            } else {
                time_window_start.visibility = View.VISIBLE
                time_window_start.text = waypoint.noEarlierThan
            }
            if (waypoint.noLaterThan.isNullOrBlank()) {
                time_window_end.visibility = View.GONE
            } else {
                time_window_end.visibility = View.VISIBLE
                time_window_end.text = waypoint.noLaterThan
            }
        }

        eta_text.text = if (waypoint.eta.isNullOrBlank()) "Start the order to calculate ETA" else waypoint.eta
    }

    private fun refresh() {
        updateStatus()
        updateTimeWindowDetails()
        updateInventoryList()
        updateAddress()
        waypoint?.let { updateCustomer(it) }
    }

    private fun updateStatus() {
        way_point_status_label_text.text = "Waypoint Status: ${TaskStatusMap.getUserStatus(waypoint.status).toUpperCase(Locale.US)} (${task.status})"
        waypoint_is_current_label.visibility = if (waypoint.id == task.currentWayPointId) View.VISIBLE else GONE
    }

    private fun updateAddress() {
        val sb = getFormattedAddressText()
        addressText.text = sb.toString()
        secondLineAddress.text = waypoint.secondLineAddress
        customerAddressType.text = waypoint.addressType.name.substring(waypoint.addressType.name.lastIndexOf("_") + 1)
        customerAddressName.text = waypoint.locationName
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
        inventoryLayout.setAmountPaid(task.paidAmount, task.paymentMethod)
    }

    companion object {
        const val TAG = "WayPointFragment"
        fun newInstance(context: Context, task: Task, way_point_id: Long): WaypointView {
            return WaypointView(context, task, way_point_id)
        }
    }
}
