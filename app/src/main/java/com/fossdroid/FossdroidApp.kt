package com.fossdroid

import android.app.Application
import com.fossdroid.data.AppDatabase
import com.fossdroid.data.EventRepository
import com.fossdroid.notifications.NotificationHelper
import com.fossdroid.widget.WidgetUpdater

class FossdroidApp : Application() {
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
