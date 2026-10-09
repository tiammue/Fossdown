package com.fossdown

import android.app.Application
import com.fossdown.data.AppDatabase
import com.fossdown.data.EventRepository
import com.fossdown.notifications.NotificationHelper
import com.fossdown.widget.WidgetUpdater

class FossdownApp : Application() {
    lateinit var repository: EventRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = EventRepository(db.eventDao())
        NotificationHelper.createChannels(this)
        WidgetUpdater.refreshAll(this)
    }
}
