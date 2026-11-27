package ru.familybudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.familybudget.app.ui.AppIcons
import ru.familybudget.app.ui.BudgetTheme
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.ui.ExportDialog
import ru.familybudget.app.ui.FixedPaymentsDialog
import ru.familybudget.app.ui.HomeScreen
import ru.familybudget.app.ui.OnboardingScreen
import ru.familybudget.app.ui.PlanEditor
import ru.familybudget.app.ui.PlanScreen
import ru.familybudget.app.ui.OperationEditor
import ru.familybudget.app.ui.OperationsScreen
import ru.familybudget.app.ui.SettingsScreen
import ru.familybudget.app.ui.DepositsPage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTheme { BudgetApp() }
        }
    }

    override fun onStop() {
        super.onStop()
        ViewModelProvider(this)[BudgetViewModel::class.java].syncOnBackground()
    }
}

private class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Главная", AppIcons.Home),
    Tab("План", AppIcons.Plan),
    Tab("Траты", AppIcons.Expenses),
    Tab("Накопления", AppIcons.Savings),
    Tab("Ещё", AppIcons.More)
)

@Composable
private fun BudgetApp() {
    val vm: BudgetViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val editing = vm.editing
    val settings by vm.settings.collectAsState()

    LaunchedEffect(vm.message) {
        if (vm.message.isNotBlank()) {
            snackbarHostState.showSnackbar(vm.message)
            vm.clearMessage()
        }
    }

    val planMonth = vm.planEditing
    val categoriesNow by vm.categories.collectAsState()
    val plansNow by vm.plans.collectAsState()
    val nowMonth = remember { YearMonth.now() }
    val askMonthlyGoals = settings.onboarded && settings.fixedPromptYm != nowMonth.toString() &&
        !ru.familybudget.app.logic.Plans.hasPlan(plansNow, nowMonth)
    val askFixed = settings.onboarded && !askMonthlyGoals && settings.fixedPromptYm != nowMonth.toString() &&
        categoriesNow.any { !it.isIncome && Fmt.isBill(it.name) }
    ExportDialog(vm)

    val screenKey = when {
        !settings.onboarded -> 0
        planMonth != null -> 1
        editing != null -> 2
        else -> 3
    }

    AnimatedContent(
        targetState = screenKey,
        transitionSpec = {
            (fadeIn(tween(280, easing = FastOutSlowInEasing)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 18 }) togetherWith
                (fadeOut(tween(220, easing = FastOutSlowInEasing)) + slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { -it / 24 })
        },
        label = "rootScreen"
    ) { screen ->
        when (screen) {
            0 -> OnboardingScreen(vm)
            1 -> planMonth?.let { PlanEditor(vm, it) }
            2 -> editing?.let { OperationEditor(vm, it) }
            else -> {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { i, t ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(t.icon, contentDescription = t.title) },
                                    label = {
                                        Text(
                                            t.title,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = tab == 0 || tab == 2,
                            enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                            exit = scaleOut(tween(160)) + fadeOut(tween(160))
                        ) {
                            FloatingActionButton(onClick = { vm.startNew() }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Добавить операцию")
                            }
                        }
                    }
                ) { padding ->
                    Crossfade(
                        targetState = tab,
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        label = "mainTab"
                    ) { currentTab ->
                        when (currentTab) {
                            0 -> HomeScreen(vm, padding)
                            1 -> PlanScreen(vm, padding)
                            2 -> OperationsScreen(vm, padding)
                            3 -> DepositsPage(vm, padding)
                            else -> SettingsScreen(vm, padding)
                        }
                    }
                }
                if (askMonthlyGoals) {
                    AlertDialog(
                        onDismissRequest = { vm.dismissFixed(nowMonth) },
                        title = { Text("🗓️ Цели на ${Fmt.monthTitle(nowMonth)}") },
                        text = { Text("Начался новый месяц, а цели расходов ещё не заполнены. Хотите запланировать бюджет сейчас?") },
                        confirmButton = {
                            TextButton(onClick = { vm.dismissFixed(nowMonth); vm.openPlan(nowMonth) }) {
                                Text("Заполнить цели")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { vm.dismissFixed(nowMonth) }) { Text("Позже") }
                        }
                    )
                } else if (askFixed) {
                    FixedPaymentsDialog(vm, nowMonth)
                }
            }
        }
    }
}
