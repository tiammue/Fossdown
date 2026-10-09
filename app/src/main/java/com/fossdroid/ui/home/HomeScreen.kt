package com.fossdroid.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fossdroid.R
import com.fossdroid.ui.components.EventProgressCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    val availableUpdate = (updateState as? UpdateUiState.Available)?.update

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.check_for_updates)) },
                            onClick = {
                                menuOpen = false
                                viewModel.checkForUpdates(manual = true)
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.version_label,
                                        viewModel.currentVersion
                                    )
                                )
                            },
                            onClick = { menuOpen = false },
                            enabled = false
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                shape = CircleShape,
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_event))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.background,
                            scheme.surfaceVariant.copy(alpha = 0.35f),
                            scheme.background
                        )
                    )
                )
                .padding(padding)
        ) {
            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_events),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.dashboard),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.widget_help),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (availableUpdate != null) {
                        item {
                            UpdateBanner(
                                version = availableUpdate.versionName,
                                onOpen = {
                                    val url = availableUpdate.apkDownloadUrl
                                        ?: availableUpdate.htmlUrl
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                }
                            )
                        }
                    }
                    items(events, key = { it.id }) { event ->
                        EventProgressCard(
                            event = event,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(event.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }

            if (events.isEmpty() && availableUpdate != null) {
                UpdateBanner(
                    version = availableUpdate.versionName,
                    onOpen = {
                        val url = availableUpdate.apkDownloadUrl ?: availableUpdate.htmlUrl
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    when (val state = updateState) {
        is UpdateUiState.Checking -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.check_for_updates)) },
                text = { Text(stringResource(R.string.update_checking)) },
                confirmButton = {}
            )
        }
        is UpdateUiState.UpToDate -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateMessage,
                title = { Text(stringResource(R.string.update_up_to_date_title)) },
                text = {
                    Text(
                        stringResource(R.string.update_up_to_date_body, state.version)
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdateMessage) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateMessage,
                title = { Text(stringResource(R.string.update_failed_title)) },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdateMessage) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissUpdateMessage()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.releasesUrl))
                            )
                        }
                    ) {
                        Text(stringResource(R.string.open_releases))
                    }
                }
            )
        }
        is UpdateUiState.Available -> {
            if (state.showDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissUpdateMessage,
                    title = {
                        Text(
                            stringResource(
                                R.string.update_available_title,
                                state.update.versionName
                            )
                        )
                    },
                    text = {
                        Text(
                            state.update.body?.take(280)
                                ?: stringResource(R.string.update_available_body)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val url = state.update.apkDownloadUrl ?: state.update.htmlUrl
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                viewModel.dismissUpdateMessage()
                            }
                        ) {
                            Text(stringResource(R.string.download_update))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(state.update.htmlUrl))
                                )
                                viewModel.dismissUpdateMessage()
                            }
                        ) {
                            Text(stringResource(R.string.view_on_github))
                        }
                    }
                )
            }
        }
        UpdateUiState.Idle -> Unit
    }
}

@Composable
private fun UpdateBanner(
    version: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.primary.copy(alpha = 0.16f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.SystemUpdateAlt,
            contentDescription = null,
            tint = scheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.update_available_title, version),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface
            )
            Text(
                text = stringResource(R.string.update_banner_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}
