package com.bringg.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import driver_sdk.providers.NotificationProvider

class ExampleNotificationProvider(
    private val context: Context
) : NotificationProvider {
    val TAG = "ExampleNotificationProvider"

    override fun getBackgroundLongProcessNotification(): Notification {
        Log.d(TAG, "getBackgroundLongProcessNotification() called")
        return getShiftNotification()
    }

    override fun getShiftNotification(): Notification {
        Log.d(TAG, "getShiftNotification() called")
        val channelId = "channel-01"
        val channelName = "Channel Name"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.resources.getString(R.string.notification_in_shift_title))
            .setContentText(context.resources.getString(R.string.notification_in_shift_message))

        return builder.build()
    }
}