package com.bringg.example.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bringg.example.R
import driver_sdk.models.InventoryItem

class InventoryPricingAdapter(
    private val inventoryItems: List<InventoryItem>,
    private val taskTitle: String?
) : RecyclerView.Adapter<WpInventoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WpInventoryViewHolder {
        val listItemInventoryWithPrice = if (PRICING_VIEW_TYPE_HEADER == viewType) R.layout.list_item_inventory_title else R.layout.list_item_inventory_with_price
        return WpInventoryViewHolder(LayoutInflater.from(parent.context).inflate(listItemInventoryWithPrice, parent, false))
    }

    override fun getItemCount() = inventoryItems.size + 1 // + header

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return PRICING_VIEW_TYPE_HEADER else return PRICING_VIEW_TYPE_ROW
    }

    override fun onBindViewHolder(holder: WpInventoryViewHolder, position: Int) {
        if (PRICING_VIEW_TYPE_ROW == holder.itemViewType)
            holder.bind(inventoryItems[position - 1], taskTitle)
    }

    companion object {
        private const val PRICING_VIEW_TYPE_HEADER = 0
        private const val PRICING_VIEW_TYPE_ROW = 1
    }
}