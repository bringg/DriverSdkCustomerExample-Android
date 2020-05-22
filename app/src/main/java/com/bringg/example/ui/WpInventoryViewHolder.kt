package com.bringg.example.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bringg.example.R
import com.bumptech.glide.Glide
import driver_sdk.models.Inventory

class WpInventoryViewHolder(v: View) : RecyclerView.ViewHolder(v) {

    fun bind(inventory: Inventory, taskTitle: String?) {
        val image: ImageView = itemView.findViewById(R.id.img_inventory_item)
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val totalQuantity: TextView = itemView.findViewById(R.id.tv_total_quantity)
        val rejectedQuantity: TextView = itemView.findViewById(R.id.tv_rejected_quantity)
        val price: TextView = itemView.findViewById(R.id.tv_price)
        val comment: TextView = itemView.findViewById(R.id.inventory_comment)
        val resources = itemView.resources

        val color = if (adapterPosition % 2 == 0) R.color.pricing_summary_table_cell_background_1 else R.color.pricing_summary_table_cell_background_2
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, color))

        name.setTextColor(Color.GRAY)
        price.setTextColor(Color.GRAY)
        totalQuantity.setTextColor(Color.GRAY)

        rejectedQuantity.text = "${inventory.rejectedQuantity} rejected"

        name.text = if (TextUtils.isEmpty(inventory.title)) taskTitle else inventory.title
        name.setTypeface(null, Typeface.NORMAL)
        val padding = resources.getDimensionPixelOffset(R.dimen.sub_inventory_pricing_padding)
        name.setPadding(padding * inventory.level, name.paddingTop, name.paddingRight, name.paddingBottom)

        totalQuantity.text = inventory.originalQuantity.toString()
        totalQuantity.setTypeface(null, Typeface.NORMAL)
        price.text = "%.2f".format(inventory.cost)
        price.setTypeface(null, Typeface.NORMAL)
        comment.text = inventory.note

        val imageUrl = inventory.image
        if (TextUtils.isEmpty(imageUrl) || !imageUrl!!.startsWith("http")) {
            image.setImageDrawable(null)
            image.visibility = View.GONE
        } else {
            image.visibility = View.VISIBLE
            Glide.with(image).load(imageUrl).into(image)
        }
    }
}
