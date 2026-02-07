package com.workguard.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

internal data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

internal val bottomNavItems = listOf(
    BottomNavItem(Routes.Home, "Home", Icons.Outlined.Home),
    BottomNavItem(Routes.Chat, "Chat", Icons.Outlined.ChatBubble),
    BottomNavItem(Routes.Scan, "Scan", Icons.Outlined.QrCodeScanner),
    BottomNavItem(Routes.News, "News", Icons.Outlined.Article),
    BottomNavItem(Routes.Settings, "Settings", Icons.Outlined.Settings)
)

@Composable
fun BottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val accent = Color(0xFF16B3A8)
    val muted = Color(0xFF9AA4A7)
    val isChatRoute = currentRoute == Routes.Chat || currentRoute == Routes.ChatThread

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFFE2E6E8), thickness = 1.dp)
        NavigationBar(
            containerColor = Color(0xFFFFFFFF),
            tonalElevation = 0.dp
        ) {
            bottomNavItems.forEach { item ->
                val selected = if (item.route == Routes.Chat) {
                    isChatRoute
                } else {
                    currentRoute == item.route
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(Routes.Home) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        unselectedIconColor = muted,
                        unselectedTextColor = muted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
