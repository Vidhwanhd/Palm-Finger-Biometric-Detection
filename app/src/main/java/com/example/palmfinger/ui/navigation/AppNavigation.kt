package com.example.palmfinger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.palmfinger.ui.screens.PalmScreen
import com.example.palmfinger.ui.screens.FingerScreen
import com.example.palmfinger.ui.screens.ResultScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Palm.route
    ) {

        // ================= PALM SCREEN =================
        composable(Screen.Palm.route) {
            PalmScreen(navController)
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
                storedHand = handSide
            )
        }

        // ================= RESULT SCREEN =================
        composable(Screen.Result.route) {
            ResultScreen(navController)
        }
    }
}
