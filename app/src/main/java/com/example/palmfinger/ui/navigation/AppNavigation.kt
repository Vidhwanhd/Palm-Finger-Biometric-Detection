package com.example.palmfinger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.palmfinger.ui.screens.PalmScreen
import com.example.palmfinger.ui.screens.FingerScreen
import com.example.palmfinger.ui.screens.ResultScreen
import com.example.palmfinger.viewmodel.MainViewModel

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    // 🔥 Create ONE shared ViewModel
    val mainViewModel: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Palm.route
    ) {

        // ================= PALM SCREEN =================
        composable(Screen.Palm.route) {
            PalmScreen(
                navController = navController,
                viewModel = mainViewModel
            )
        }

        // ================= FINGER SCREEN =================
        composable(
            route = Screen.Finger.route,
            arguments = listOf(
                navArgument("handSide") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val handSide =
                backStackEntry.arguments?.getString("handSide")

            if (handSide == null) {
                navController.popBackStack()
                return@composable
            }

            FingerScreen(
                navController = navController,
                storedHand = handSide,
                viewModel = mainViewModel
            )
        }

        // ================= RESULT SCREEN =================
        composable(Screen.Result.route) {
            ResultScreen(
                navController = navController,
                viewModel = mainViewModel
            )
        }
    }
}
