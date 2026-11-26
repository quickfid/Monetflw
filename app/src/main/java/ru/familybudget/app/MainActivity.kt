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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.YearMonth
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.familybudget.app.ui.BudgetTheme
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.ui.ExportDialog
import ru.familybudget.app.ui.FixedPaymentsDialog
import ru.familybudget.app.ui.HomeScreen
import ru.familybudget.app.ui.OnboardingScreen
import ru.familybudget.app.ui.PlanEditor
import ru.familybudget.app.ui.PlanScreen
import ru.familybudget.app.ui.ReportsHost
import ru.familybudget.app.ui.OperationEditor
import ru.familybudget.app.ui.OperationsScreen
import ru.familybudget.app.ui.SettingsScreen

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
    Tab("Главная", Icons.Default.Home),
    Tab("План", Icons.Default.DateRange),
    Tab("Траты", Icons.Default.List),
    Tab("Отчёты", Icons.Default.Info),
    Tab("Ещё", Icons.Default.Settings)
)

@Composable
private fun BudgetApp() {
    val vm: BudgetViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val editing = vm.editing
    val settings by vm.settings.collectAsState()

    val planMonth = vm.planEditing
    val categoriesNow by vm.categories.collectAsState()
    val nowMonth = remember { YearMonth.now() }
    val askFixed = settings.onboarded && settings.fixedPromptYm != nowMonth.toString() &&
        categoriesNow.any { !it.isIncome && Fmt.isBill(it.name) }
    ExportDialog(vm)

    if (!settings.onboarded) {
        OnboardingScreen(vm)
    } else if (planMonth != null) {
        PlanEditor(vm, planMonth)
    } else if (editing != null) {
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
                if (tab == 0 || tab == 2) {
                    FloatingActionButton(onClick = { vm.startNew() }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить операцию")
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(vm, padding)
                1 -> PlanScreen(vm, padding)
                2 -> OperationsScreen(vm, padding)
                3 -> ReportsHost(vm, padding)
                else -> SettingsScreen(vm, padding)
            }
        }
        if (askFixed) FixedPaymentsDialog(vm, nowMonth)
    }
}
