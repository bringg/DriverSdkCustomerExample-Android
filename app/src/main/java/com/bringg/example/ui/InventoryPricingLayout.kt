package com.bringg.example.ui

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.bringg.example.R
import driver_sdk.models.InventoryItem
import driver_sdk.models.Task
import driver_sdk.models.Waypoint
import driver_sdk.models.enums.PaymentMethod
import driver_sdk.util.annotations.Mockable
import kotlinx.android.synthetic.main.layout_inventory_pricing.view.*

@Mockable
class InventoryPricingLayout : ConstraintLayout {

    private var fallbackTitle: String? = null
    private val pricingFormat = "%.2f"

    var inventoryItems: List<InventoryItem> = emptyList()

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        inflate(context, R.layout.layout_inventory_pricing, this)
    }

    fun setDeliveryFee(deliveryFee: Double) {
        tv_delivery_fee_value.text = pricingFormat.format(deliveryFee)
    }

    fun setTotal(total: Double) {
        tv_total_value.text = pricingFormat.format(total)
    }

    fun setToBePaid(toBePaid: Double) {
        tv_total_to_be_paid_value.text = pricingFormat.format(toBePaid)
    }

    fun setData(task: Task, waypoint: Waypoint) {
        this.fallbackTitle = task.title
        this.inventoryItems = waypoint.flattenedInventoryList.toMutableList()
        list_inventory_items.adapter = InventoryPricingAdapter(inventoryItems, fallbackTitle)
    }

    fun setAmountPaid(paidAmount: Double, paymentMethod: PaymentMethod) {
        tv_amount_paid_label.text = "Amount paid (${paymentMethod.name})"
        tv_amount_paid_value.text = pricingFormat.format(paidAmount)
    }
}