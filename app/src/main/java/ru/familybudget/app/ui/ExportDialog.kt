package ru.familybudget.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.familybudget.app.BudgetViewModel

/** Окно выгрузки отчёта в Google Таблицу: ожидание → ссылка (открыть / поделиться) или ошибка. */
@Composable
fun ExportDialog(vm: BudgetViewModel) {
    val context = LocalContext.current
    when (vm.exportState) {
        1 -> AlertDialog(
            onDismissRequest = {},
            title = { Text("📤 Готовлю отчёт…") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Строю таблицы и графики. Это может занять до минуты.")
                }
            },
            confirmButton = {}
        )
        2 -> AlertDialog(
            onDismissRequest = { vm.closeExport() },
            title = { Text("🎉 Отчёт готов") },
            text = { Text("Он в вашей Google Таблице. Откройте или отправьте ссылку близкому человеку.") },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.exportUrl)))
                    vm.closeExport()
                }) { Text("Открыть") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, vm.exportUrl)
                        }
                        context.startActivity(Intent.createChooser(send, "Поделиться отчётом"))
                    }) { Text("Поделиться") }
                    TextButton(onClick = { vm.closeExport() }) { Text("Закрыть") }
                }
            }
        )
        3 -> AlertDialog(
            onDismissRequest = { vm.closeExport() },
            title = { Text("Не получилось") },
            text = { Text(vm.exportMessage) },
            confirmButton = { TextButton(onClick = { vm.closeExport() }) { Text("Понятно") } }
        )
        else -> {}
    }
}
