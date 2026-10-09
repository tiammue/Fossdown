package com.fossdroid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.fossdroid.FossdroidApp
import com.fossdroid.R
import com.fossdroid.data.ProgressEvent
import com.fossdroid.data.ProgressStyle
import com.fossdroid.domain.DotGrid
import com.fossdroid.domain.ProgressCalculator
import com.fossdroid.domain.ProgressSnapshot
import com.fossdroid.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CountdownWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.delete(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            WidgetUpdater.refreshAll(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.fossdroid.ACTION_WIDGET_REFRESH"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val eventId = WidgetPrefs.getEventId(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            val openApp = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            if (eventId <= 0L) {
                bindPlaceholder(context, appWidgetManager, appWidgetId, views)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val app = context.applicationContext as? FossdroidApp
            val event = if (app != null) {
                runBlocking { app.repository.getById(eventId) }
            } else null

            if (event == null) {
                views.setTextViewText(R.id.widget_title, "Missing event")
                bindRemainingLines(
                    views,
                    listOf("Open app"),
                    context.getColor(R.color.widget_muted_text)
                )
                views.setViewVisibility(R.id.widget_icon, View.GONE)
                setProgressVisual(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    views,
                    style = ProgressStyle.BAR,
                    fraction = 0f,
                    accentColor = context.getColor(R.color.widget_default_accent),
                    textColor = context.getColor(R.color.widget_default_text),
                    rounded = true,
                    event = null,
                    snapshot = null,
                    centerLabel = "",
                    remainingLineCount = 1
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val snap = ProgressCalculator.snapshot(event)
            bindEvent(context, appWidgetManager, appWidgetId, views, event, snap)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun bindPlaceholder(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
            bindRemainingLines(
                views,
                listOf(context.getString(R.string.widget_tap_configure)),
                context.getColor(R.color.widget_muted_text)
            )
            views.setViewVisibility(R.id.widget_icon, View.GONE)
            setProgressVisual(
                context,
                appWidgetManager,
                appWidgetId,
                views,
                style = ProgressStyle.BAR,
                fraction = 0f,
                accentColor = context.getColor(R.color.widget_default_accent),
                textColor = context.getColor(R.color.widget_default_text),
                rounded = true,
                event = null,
                snapshot = null,
                centerLabel = "",
                remainingLineCount = 1
            )
        }

        private fun bindEvent(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews,
            event: ProgressEvent,
            snap: ProgressSnapshot
        ) {
            val text = event.textColor.toInt()
            val muted = Color.argb(
                200,
                Color.red(text),
                Color.green(text),
                Color.blue(text)
            )
            views.setInt(R.id.widget_root, "setBackgroundColor", event.backgroundColor.toInt())
            views.setTextViewText(R.id.widget_title, event.title)
            views.setTextColor(R.id.widget_title, text)
            bindRemainingLines(views, snap.remainingLabels, muted)
            views.setViewVisibility(
                R.id.widget_icon,
                if (event.showIcon) View.VISIBLE else View.GONE
            )

            setProgressVisual(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                views = views,
                style = event.progressStyle,
                fraction = snap.progressFraction,
                accentColor = event.accentColor.toInt(),
                textColor = text,
                rounded = event.barRounded,
                event = event,
                snapshot = snap,
                centerLabel = DotGrid.remainingUnits(event, snap).toString(),
                remainingLineCount = snap.remainingLabels.size.coerceIn(1, 3)
            )
        }

        private fun bindRemainingLines(views: RemoteViews, labels: List<String>, color: Int) {
            val lineIds = intArrayOf(R.id.widget_line1, R.id.widget_line2, R.id.widget_line3)
            lineIds.forEachIndexed { index, id ->
                val label = labels.getOrNull(index)
                if (label.isNullOrBlank()) {
                    views.setViewVisibility(id, View.GONE)
                    views.setTextViewText(id, "")
                } else {
                    views.setViewVisibility(id, View.VISIBLE)
                    views.setTextViewText(id, label)
                    views.setTextColor(id, color)
                }
            }
        }

        private fun setProgressVisual(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews,
            style: ProgressStyle,
            fraction: Float,
            accentColor: Int,
            textColor: Int,
            rounded: Boolean,
            event: ProgressEvent?,
            snapshot: ProgressSnapshot?,
            centerLabel: String,
            remainingLineCount: Int
        ) {
            val (widthDp, heightDp) = widgetSizeDp(appWidgetManager, appWidgetId)
            val contentWidth = (widthDp - 24f).coerceAtLeast(80f)
            // Header shares a row with remaining lines (up to 3); reserve ~14dp per line.
            val headerReserve = (18f + remainingLineCount * 14f).coerceAtLeast(32f)
            val contentHeight = (heightDp - 24f - headerReserve).coerceAtLeast(40f)
            val bitmap = WidgetProgressRenderer.render(
                context = context,
                style = style,
                fraction = fraction,
                accentColor = accentColor,
                textColor = textColor,
                rounded = rounded,
                widthDp = contentWidth,
                heightDp = contentHeight,
                event = event,
                snapshot = snapshot,
                centerLabel = centerLabel
            )
            views.setImageViewBitmap(R.id.widget_progress_visual, bitmap)
        }

        private fun widgetSizeDp(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): Pair<Float, Float> {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val widths = listOf(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            ).filter { it > 0 }
            val heights = listOf(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            ).filter { it > 0 }
            // Prefer the larger reported size so the bitmap fills the real cell.
            val width = widths.maxOrNull()?.toFloat() ?: 180f
            val height = heights.maxOrNull()?.toFloat() ?: 110f
            return width to height
        }
    }
}

object WidgetUpdater {
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        CoroutineScope(Dispatchers.Default).launch {
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CountdownWidgetProvider::class.java)
            )
            ids.forEach { id ->
                CountdownWidgetProvider.updateAppWidget(context, manager, id)
            }
        }
    }
}

object WidgetPrefs {
    private const val PREFS = "fossdroid_widgets"
    private fun key(id: Int) = "event_$id"

    fun saveEventId(context: Context, appWidgetId: Int, eventId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(key(appWidgetId), eventId)
            .apply()
    }

    fun getEventId(context: Context, appWidgetId: Int): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(key(appWidgetId), -1L)
    }

    fun delete(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(appWidgetId))
            .apply()
    }
}
