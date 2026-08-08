package com.example.crossmediatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.crossmediatracker.ui.MediaViewModel
import com.example.crossmediatracker.ui.components.DetailEditDialog
import com.example.crossmediatracker.ui.screens.HomeScreen
import com.example.crossmediatracker.ui.theme.MediaTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge‑to‑edge rendering
        enableEdgeToEdge()

        setContent {
            MediaTrackerTheme {
                val viewModel: MediaViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onAddClick = { navController.navigate("edit?itemId=null") },
                                onItemClick = { item ->
                                    navController.navigate("edit?itemId=${item.id}")
                                },
                                onFilterSelected = viewModel::setFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onBackup = viewModel::backupToLocalZip,
                                onRestore = viewModel::restoreFromLocalZip,
                                onClearMessage = viewModel::clearSyncMessage
                            )
                        }
                        composable("edit?itemId={itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId")
                            val item = if (itemId == null || itemId == "null") {
                                null
                            } else {
                                uiState.items.find { it.id == itemId }
                            }

                            DetailEditDialog(
                                item = item,
                                onDismiss = { navController.popBackStack() },
                                onSave = viewModel::addOrUpdate,
                                onDelete = viewModel::deleteItem
                            )
                        }
                    }
                }
            }
        }
    }
}