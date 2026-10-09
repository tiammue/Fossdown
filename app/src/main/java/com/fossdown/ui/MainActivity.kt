package com.fossdown.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fossdown.FossdownApp
import com.fossdown.ui.edit.EditEventScreen
import com.fossdown.ui.edit.EditEventViewModel
import com.fossdown.ui.home.HomeScreen
import com.fossdown.ui.home.HomeViewModel
import com.fossdown.ui.theme.FossdownTheme
import com.fossdown.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FossdownApp
        setContent {
            FossdownTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val vm: HomeViewModel = viewModel(
                                factory = HomeViewModel.factory(app.repository)
                            )
                            HomeScreen(
                                viewModel = vm,
                                onAdd = { navController.navigate("edit/-1") },
                                onOpen = { id -> navController.navigate("edit/$id") }
                            )
                        }
                        composable(
                            route = "edit/{eventId}",
                            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
                        ) { entry ->
                            val eventId = entry.arguments?.getLong("eventId") ?: -1L
                            val vm: EditEventViewModel = viewModel(
                                factory = EditEventViewModel.factory(app.repository, eventId)
                            )
                            EditEventScreen(
                                viewModel = vm,
                                onDone = {
                                    WidgetUpdater.refreshAll(this@MainActivity)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WidgetUpdater.refreshAll(this)
    }
}
