package com.callmind.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.callmind.app.ui.calldetail.CallDetailScreen
import com.callmind.app.ui.contact.ContactScreen
import com.callmind.app.ui.main.MainScreen
import com.callmind.app.ui.permissions.PermissionScreen
import com.callmind.app.ui.settings.SettingsScreen

object Routes {
    const val PERMISSIONS = "permissions"
    const val MAIN = "main"
    const val CALL_DETAIL = "call/{callId}"
    const val CONTACT = "contact/{contactName}"
    const val SETTINGS = "settings"

    fun callDetail(callId: Long) = "call/$callId"
    fun contact(contactName: String) = "contact/${Uri.encode(contactName)}"
}

@Composable
fun CallMindNavGraph(
    navController: NavHostController,
    hasPermissions: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = if (hasPermissions) Routes.MAIN else Routes.PERMISSIONS
    ) {

        composable(Routes.PERMISSIONS) {
            PermissionScreen(
                onAllGranted = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onCallClick = { callId -> navController.navigate(Routes.callDetail(callId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onContactClick = { name -> navController.navigate(Routes.contact(name)) }
            )
        }

        composable(
            route = Routes.CALL_DETAIL,
            arguments = listOf(navArgument("callId") { type = NavType.LongType })
        ) {
            CallDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CONTACT,
            arguments = listOf(navArgument("contactName") { type = NavType.StringType })
        ) {
            ContactScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
