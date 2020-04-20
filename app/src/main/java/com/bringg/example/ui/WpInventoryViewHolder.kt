package com.bringg.example.ui

import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bringg.example.R
import driver_sdk.models.InventoryItem

class WpInventoryViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    private val root: View = v.findViewById(R.id.root)
    private val remainingQuantity: TextView = v.findViewById(R.id.tv_remaining_quantity)
    private val name: TextView = v.findViewById(R.id.tv_name)
    private val totalQuantity: TextView = v.findViewById(R.id.tv_total_quantity)
    private val rejectedQuantity: TextView = v.findViewById(R.id.tv_rejected_quantity)
    private val price: TextView = v.findViewById(R.id.tv_price)
    private val comment: TextView = v.findViewById(R.id.inventory_comment)
    private val resources = itemView.resources

    fun bindHeader(totalCount: Int) {
        name.text = "ITEM"
        price.text = "PRICE"
        totalQuantity.text = "TOTAL ITEMS: $totalCount"
        name.setTypeface(null, Typeface.BOLD)
        price.setTypeface(null, Typeface.BOLD)
        totalQuantity.setTypeface(null, Typeface.BOLD)
    }

    fun bind(inventory: InventoryItem, taskTitle: String?) {

        val color = if (adapterPosition % 2 == 0) R.color.pricing_summary_table_cell_background_1 else R.color.pricing_summary_table_cell_background_2
        root.setBackgroundColor(ContextCompat.getColor(itemView.context, color))

        remainingQuantity.text = inventory.remainingQuantity.toString()
        rejectedQuantity.text = "${inventory.rejectedQuantity}\nrejected"

        name.text = if (TextUtils.isEmpty(inventory.title)) taskTitle else inventory.title
        name.setTypeface(null, Typeface.NORMAL)
        val padding = resources.getDimensionPixelOffset(R.dimen.sub_inventory_pricing_padding)
        name.setPadding(padding * inventory.level, name.paddingTop, name.paddingRight, name.paddingBottom)

        totalQuantity.text = inventory.originalQuantity.toString()
        totalQuantity.setTypeface(null, Typeface.NORMAL)
        price.text = "%.2f".format(inventory.cost)
        price.setTypeface(null, Typeface.NORMAL)
        comment.text = inventory.note
    }
}
