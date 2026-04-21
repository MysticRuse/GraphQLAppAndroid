package com.android.mr.githubexplorer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.internal.composableLambdaN
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mr.githubexplorer.ui.profile.ProfileScreen
import com.android.mr.githubexplorer.ui.search.SearchScreen

// All route strings in one place
object Routes {
    const val SEARCH = "search"
    const val PROFILE = "profile/{login}"

    fun profile(login: String) = "profile/$login"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SEARCH
    ) {
        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateToProfile = { login ->
                    navController.navigate(Routes.profile(login))
                }
            )
        }

        // {login} in the route becomes a new nav argument
        // ProfileViewModel reads it from SavedStateHandle["login"]
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}