package com.fossdown.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fossdown.FossdownApp
import com.fossdown.data.ProgressEvent
import com.fossdown.ui.components.EventProgressCard
import com.fossdown.ui.theme.FossdownTheme

class CountdownWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as FossdownApp
        enableEdgeToEdge()
        setContent {
            FossdownTheme {
                var events by remember { mutableStateOf<List<ProgressEvent>>(emptyList()) }
                LaunchedEffect(Unit) {
                    events = app.repository.getActive()
                }
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Choose an event") })
                    }
                ) { padding ->
                    ConfigureList(
                        events = events,
                        padding = padding,
                        onSelect = { event ->
                            WidgetPrefs.saveEventId(this, appWidgetId, event.id)
                            CountdownWidgetProvider.updateAppWidget(
                                this,
                                AppWidgetManager.getInstance(this),
                                appWidgetId
                            )
                            val result = Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                            )
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigureList(
    events: List<ProgressEvent>,
    padding: PaddingValues,
    onSelect: (ProgressEvent) -> Unit
) {
    if (events.isEmpty()) {
        Text(
            text = "Create an event in the app first, then add the widget again.",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { event ->
            EventProgressCard(
                event = event,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(event) }
            )
        }
    }
}
