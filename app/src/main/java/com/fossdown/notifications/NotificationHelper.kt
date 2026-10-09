package com.fossdown.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fossdown.FossdownApp
import com.fossdown.R
import com.fossdown.domain.ProgressCalculator
import com.fossdown.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_EVENTS = "event_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_EVENTS,
            "Event reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for countdowns and habit streaks"
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyUpcoming(context: Context) {
        val app = context.applicationContext as? FossdownApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val events = app.repository.getActive()
            val now = System.currentTimeMillis()
            events.forEach { event ->
                if (!event.notifyDayBefore && !event.notifyOnComplete) return@forEach
                val snap = ProgressCalculator.snapshot(event, now)
                val dayMs = TimeUnit.DAYS.toMillis(1)
                when {
                    event.notifyOnComplete && snap.isComplete -> {
                        show(
                            context,
                            event.id.toInt(),
                            event.title,
                            "Complete — nice work!"
                        )
                    }
                    event.notifyDayBefore &&
                        snap.remainingMillis in 1 until dayMs &&
                        event.mode.name != "COUNT_UP" -> {
                        show(
                            context,
                            (event.id + 10_000).toInt(),
                            event.title,
                            "Less than a day left"
                        )
                    }
                }
            }
        }
    }

    private fun show(context: Context, id: Int, title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_flag)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Notification permission may be denied on Android 13+.
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            WidgetUpdater.refreshAll(context)
            NotificationHelper.notifyUpcoming(context)
        }
    }
}
