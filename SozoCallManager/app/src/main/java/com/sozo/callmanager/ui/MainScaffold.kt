@file:OptIn(ExperimentalMaterial3Api::class)

package com.sozo.callmanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.sozo.callmanager.data.DemoSession
import com.sozo.callmanager.dialer.CallHolder

private enum class MainTab(val label: String, val icon: ImageVector) {
    DIALPAD("Dialpad", Icons.Filled.Dialpad),
    RECENTS("Recents", Icons.Filled.History),
    CONTACTS("Contacts", Icons.Filled.Contacts),
    REPORTS("Reports", Icons.Filled.BarChart)
}

@Composable
fun MainScaffold(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(MainTab.RECENTS) }
    val refreshTick = rememberCallLogRefreshTrigger()

    // Right after a call ends, InCallActivity sets this flag so we land on
    // Recents automatically and the user can see the call they just made.
    LaunchedEffect(CallHolder.shouldFocusRecentsOnReturn.value) {
        if (CallHolder.shouldFocusRecentsOnReturn.value) {
            selectedTab = MainTab.RECENTS
            CallHolder.shouldFocusRecentsOnReturn.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sozo Call Manager", fontWeight = FontWeight.Bold)
                        Text(
                            "${DemoSession.employeeName} \u00B7 ${DemoSession.employeeRole}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { DemoSession.logout(); onLogout() }) { Text("Log out") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                MainTab.DIALPAD -> DialpadScreen()
                MainTab.RECENTS -> RecentsScreen(refreshTick)
                MainTab.CONTACTS -> ContactsScreen()
                MainTab.REPORTS -> ReportsScreen(refreshTick)
            }
        }
    }
}
