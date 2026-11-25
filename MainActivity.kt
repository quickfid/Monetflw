package ru.familybudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.familybudget.app.ui.BudgetTheme
import ru.familybudget.app.ui.OperationEditor
import ru.familybudget.app.ui.OperationsScreen
import ru.familybudget.app.ui.OverviewScreen
import ru.familybudget.app.ui.ReportsScreen
import ru.familybudget.app.ui.SettingsScreen
import ru.familybudget.app.ui.YearScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTheme { BudgetApp() }
        }
    }
}

private class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Обзор", Icons.Default.Home),
    Tab("Операции", Icons.Default.List),
    Tab("Отчёты", Icons.Default.Info),
    Tab("Год", Icons.Default.DateRange),
    Tab("Ещё", Icons.Default.Settings)
)

@Composable
private fun BudgetApp() {
    val vm: BudgetViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val editing = vm.editing

    if (editing != null) {
        OperationEditor(vm, editing)
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = t.title) },
                            label = { Text(t.title) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab <= 1) {
                    FloatingActionButton(onClick = { vm.startNew() }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить операцию")
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> OverviewScreen(vm, padding)
                1 -> OperationsScreen(vm, padding)
                2 -> ReportsScreen(vm, padding)
                3 -> YearScreen(vm, padding)
                else -> SettingsScreen(vm, padding)
            }
        }
    }
}
