package com.github.foxeiz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val channelId = "upload_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "File Upload",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of file uploads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUploadProgress(notificationId: Int, fileName: String?, progress: Int, max: Int) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Uploading $fileName")
            .setContentText("$progress/$max files uploaded")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun dismissNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}