package com.bringg.example.ui

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
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

class WaypointView : CardView {
    private var inventoryLayout: InventoryPricingLayout
    private var addressText: TextView
    private var secondLineAddress: TextView
    private var customerAddressType: TextView
    private var customerAddressName: TextView
    private var headerView: View

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    init {
        useCompatPadding = true
        val inflater = LayoutInflater.from(context)
        headerView = inflater.inflate(R.layout.fragment_waypoint_list_header, this)
        addressText = headerView.findViewById(R.id.way_point_address)
        secondLineAddress = headerView.findViewById(R.id.way_point_second_address)
        customerAddressType = headerView.findViewById(R.id.customer_address_type)
        customerAddressName = headerView.findViewById(R.id.customer_address_name)
        inventoryLayout = InventoryPricingLayout(context)
        inventoryLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        inventory_list.addView(inventoryLayout)
    }

    private fun updateCustomer(waypoint: Waypoint) {
        val customer = waypoint.customer
        if (customer == null) {
            customer_name.text = "No Customer"
            customer_image.setImageDrawable(null)
            return
        }
        customer_name.text = customer.getName()
        val imageUrl =
            if (customer.imageUrl.isBlank() || !customer.imageUrl.startsWith("http")) "https://pngimage.net/wp-content/uploads/2018/06/happy-client-png-7.png" else customer.imageUrl
        Glide.with(this).load(imageUrl).into(customer_image)
    }

    private fun updateTimeWindowDetails(waypoint: Waypoint) {
        if (waypoint.scheduledAt.isNullOrBlank()) {
            scheduled_for_text.visibility = View.GONE
            scheduled_for_title.visibility = View.GONE
        } else {
            scheduled_for_text.visibility = View.VISIBLE
            scheduled_for_title.visibility = View.VISIBLE
            scheduled_for_text.text = waypoint.scheduledAt
        }

        if (waypoint.noEarlierThan.isNullOrBlank() && waypoint.noLaterThan.isNullOrBlank()) {
            time_window_title.visibility = View.GONE
        } else {
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

    fun refresh(task: Task?, waypoint: Waypoint?) {
        if (task == null || waypoint == null) {
            waypoint_root_container.visibility = View.GONE
        } else {
            waypoint_root_container.visibility = View.VISIBLE
            updateUiData(waypoint)
            updateStatus(task, waypoint)
            updateTimeWindowDetails(waypoint)
            updateInventoryList(task, waypoint)
            updateAddress(waypoint)
            updateCustomer(waypoint)
        }
    }

    private fun updateUiData(waypoint: Waypoint) {
        val color = waypoint.uiData.color
        if (TextUtils.isEmpty(color)) {
            order_color_and_number.visibility = View.GONE
        } else {
            order_color_and_number.visibility = View.VISIBLE
            order_color_and_number.text = waypoint.uiData.number.toString()
            order_color_and_number.setBackgroundColor(Color.parseColor(color))
        }
    }

    private fun updateStatus(task: Task, waypoint: Waypoint) {
        findViewById<TextView>(R.id.way_point_description_text).text = task.title
        way_point_status_label_text.text = "${TaskStatusMap.getUserStatus(waypoint.status).toUpperCase(Locale.US)} (${task.status})"
        waypoint_is_current_label.visibility = if (waypoint.id == task.currentWayPointId) View.VISIBLE else GONE
    }

    private fun updateAddress(waypoint: Waypoint) {
        val sb = getFormattedAddressText(waypoint)
        addressText.text = sb.toString()
        secondLineAddress.text = waypoint.secondLineAddress
        customerAddressType.text = waypoint.addressType.name.substring(waypoint.addressType.name.lastIndexOf("_") + 1)
        customerAddressName.text = waypoint.locationName
    }

    private fun getFormattedAddressText(waypoint: Waypoint): StringBuilder {
        val sb = StringBuilder()
        appendValue(waypoint.houseNumber, sb)
        appendValue(waypoint.address, sb)
        appendValue(waypoint.borough, sb)
        appendValue(waypoint.city, sb)
        appendValue(waypoint.state, sb)
        appendValue(waypoint.zipCode, sb)
        return sb
    }

    private fun appendValue(value: String?, sb: StringBuilder) {
        if (!value.isNullOrBlank()) {
            sb.append(value).append(" ")
        }
    }

    private fun updateInventoryList(task: Task, waypoint: Waypoint) {
        inventoryLayout.setData(task, waypoint)
        inventoryLayout.setDeliveryFee(task.deliveryPrice)
        inventoryLayout.setTotal(task.totalPrice)
        inventoryLayout.setToBePaid(task.leftToBePaid)
        inventoryLayout.setAmountPaid(task.paidAmount, task.paymentMethod)
    }

    companion object {
        const val TAG = "WayPointFragment"
        fun newInstance(context: Context): WaypointView {
            return WaypointView(context)
        }
    }
}
