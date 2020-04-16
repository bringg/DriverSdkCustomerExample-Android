package com.bringg.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils

class ExampleToast(private val context: Context) {
    private var message: String? = null
    private var icon: Drawable? = null
    private var duration = Toast.LENGTH_SHORT

    fun setMessage(message: String?): ExampleToast {
        this.message = message
        return this
    }

    fun setMessage(@StringRes messageRes: Int): ExampleToast {
        setMessage(context.resources.getString(messageRes))
        return this
    }

    fun setIcon(icon: Drawable?): ExampleToast {
        this.icon = icon
        return this
    }

    fun setIcon(@DrawableRes iconRes: Int): ExampleToast {
        setIcon(context.resources.getDrawable(iconRes))
        return this
    }

    fun setDuration(duration: Int): ExampleToast {
        this.duration = duration
        return this
    }

    fun setBackgroundColor(@ColorInt bgColor: Int): ExampleToast {
        Companion.bgColor = bgColor
        return this
    }

    fun show() {
        getToast(context, icon).show()
    }

    private fun getToast(context: Context, icon: Drawable?): Toast {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val toast = Toast(context)
        val view: View = inflater.inflate(R.layout.toast, null)
        val cardView = view.findViewById<CardView>(R.id.card_view)
        cardView.setCardBackgroundColor(bgColor)
        val textView = view.findViewById<TextView>(R.id.message)
        if (message != null) textView.text = message
        if (isColorDark(bgColor)) textView.setTextColor(
            Color.WHITE
        )
        toast.view = view
        if (icon != null) {
            val iconIV = view.findViewById<ImageView>(R.id.icon)
            iconIV.visibility = View.VISIBLE
            iconIV.setImageDrawable(icon)
        }
        toast.duration = duration
        return toast
    }

    companion object {
        private var bgColor: Int = Color.WHITE
        fun makeText(
            context: Context,
            message: String?,
            icon: Drawable?,
            duration: Int
        ): ExampleToast {
            return ExampleToast(context)
                .setMessage(message)
                .setIcon(icon)
                .setBackgroundColor(Color.WHITE)
                .setDuration(duration)
        }

        fun makeText(context: Context, message: String?, duration: Int): ExampleToast {
            return makeText(context, message, null, duration)
        }

        fun makeText(context: Context, @StringRes messageId: Int, duration: Int): ExampleToast {
            return makeText(context, context.resources.getString(messageId), null, duration)
        }

        fun makeText(
            context: Context,
            @StringRes messageId: Int,
            icon: Drawable?,
            duration: Int
        ): ExampleToast {
            return makeText(context, context.resources.getString(messageId), icon, duration)
        }

        fun makeText(
            context: Context,
            message: String?,
            @DrawableRes iconId: Int,
            duration: Int
        ): ExampleToast {
            return makeText(context, message, context.resources.getDrawable(iconId), duration)
        }

        fun makeText(context: Context, messageId: Int, iconId: Int, duration: Int): ExampleToast {
            return makeText(
                context, context.resources.getString(messageId),
                context.resources.getDrawable(iconId), duration
            )
        }

        private fun isColorDark(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) < 0.5
        }
    }
}