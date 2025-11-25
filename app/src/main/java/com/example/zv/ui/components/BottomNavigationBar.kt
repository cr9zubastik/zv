package com.example.zv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.example.zv.navigation.Screen

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    isGuest: Boolean
) {
    val items = listOf(
        BottomNavItem(Screen.Scanner.route, "Сканер", Icons.Default.Search),
        BottomNavItem(Screen.ThreatHistory.route, "История", Icons.Default.List),
        BottomNavItem(Screen.DeviceInfo.route, "Устройство", Icons.Default.Phone),
        BottomNavItem(Screen.Settings.route, "Настройки", Icons.Default.Settings)
    )
    
    NavigationBar(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (item.route == Screen.ThreatHistory.route && isGuest) {
                        // Не навигируем, если гость
                        return@NavigationBarItem
                    }
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                enabled = !(item.route == Screen.ThreatHistory.route && isGuest)
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

