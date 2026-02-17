package com.example.palmfinger.ui.navigation

sealed class Screen(val route: String) {

    object Palm : Screen("palm")

    object Finger : Screen("finger/{handSide}") {
        fun createRoute(handSide: String): String {
            return "finger/$handSide"
        }
    }

    object Result : Screen("result")
}
