package ru.familybudget.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Необязательная отправка операций в Google-таблицу через Apps Script (scripts/Code.gs).
 * Каждая операция имеет постоянный uid, поэтому повторная отправка не создаёт дубль.
 */
object SheetsSync {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private class Result(val ok: Boolean, val error: String = "")

    suspend fun push(dao: BudgetDao, settings: AppSettings): String = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank() || settings.token.isBlank()) {
            return@withContext "Синхронизация не настроена: укажите URL и токен."
        }
        val pending = dao.unsynced()
        if (pending.isEmpty()) return@withContext "Все операции уже отправлены."
        var sent = 0
        for (op in pending) {
            val result = try {
                post(op, settings)
            } catch (e: Exception) {
                return@withContext "Отправлено $sent из ${pending.size}. Нет связи: ${e.localizedMessage ?: "ошибка сети"}"
            }
            if (result.ok) {
                dao.markSynced(op.id)
                sent++
            } else {
                return@withContext "Отправлено $sent из ${pending.size}. ${result.error}"
            }
        }
        "Готово: отправлено $sent из ${pending.size}."
    }

    suspend fun test(settings: AppSettings): String = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank() || settings.token.isBlank()) {
            return@withContext "Укажите URL и токен."
        }
        try {
            val sep = if (settings.apiUrl.contains("?")) "&" else "?"
            val url = settings.apiUrl + sep + "token=" + URLEncoder.encode(settings.token, "UTF-8")
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext "Ошибка HTTP ${resp.code}."
                val json = try { JSONObject(body) } catch (e: Exception) {
                    return@withContext "Ответ не похож на API. Проверьте, что в развёртывании доступ «Все»."
                }
                if (json.optBoolean("ok")) "Подключение работает." else describeError(json)
            }
        } catch (e: Exception) {
            "Нет связи: ${e.localizedMessage ?: "ошибка сети"}"
        }
    }

    private fun post(op: Operation, settings: AppSettings): Result {
        val transfer = op.type == OpType.TRANSFER
        val payload = JSONObject().apply {
            put("token", settings.token)
            put("action", "addOperation")
            put("requestId", op.uid)
            put("date", LocalDate.ofEpochDay(op.date).toString())
            put("amount", op.amountCents / 100.0)
            put("type", op.type)
            put("category", if (transfer) "Перевод" else op.category)
            put("comment", op.comment)
            put("person", op.person)
            put("paymentMethod", if (transfer) "" else op.account)
            put("sourceAccount", if (transfer) op.account else "")
            put("destinationAccount", if (transfer) op.destAccount else "")
        }
        val request = Request.Builder().url(settings.apiUrl)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return Result(false, "Ошибка HTTP ${resp.code}. Проверьте развёртывание Apps Script.")
            val json = try { JSONObject(body) } catch (e: Exception) {
                return Result(false, "Ответ не похож на API. Проверьте, что в развёртывании доступ «Все».")
            }
            return if (json.optBoolean("ok")) Result(true) else Result(false, describeError(json))
        }
    }

    private fun describeError(json: JSONObject): String = when (json.optString("error")) {
        "unauthorized" -> "Ошибка доступа: токен в приложении и в Apps Script должен совпадать."
        "transfer_accounts_invalid" -> "Для перевода нужны два разных счёта."
        else -> "Таблица отклонила операцию: ${json.optString("message", json.optString("error"))}"
    }
}
