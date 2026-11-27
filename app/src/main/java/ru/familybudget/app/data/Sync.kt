package ru.familybudget.app.data

import kotlinx.coroutines.CancellationException
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

    private val longClient = client.newBuilder()
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private class Result(val ok: Boolean, val error: String = "")

    class ReportResult(val url: String, val message: String)

    /** Отправляет данные месяца; скрипт строит в таблице красивый отчёт с графиками и возвращает ссылку. */
    suspend fun exportReport(settings: AppSettings, payload: String): ReportResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(settings.apiUrl)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            longClient.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext ReportResult("", "Ошибка HTTP ${resp.code}.")
                val json = try { JSONObject(body) } catch (e: Exception) {
                    return@withContext ReportResult("", "Ответ не похож на API. Обновите скрипт в таблице до новой версии (scripts/Code.gs) и сделайте новое развёртывание.")
                }
                if (json.optBoolean("ok")) {
                    ReportResult(json.optString("url"), "Готово")
                } else if (json.optString("error") == "unknown_action") {
                    ReportResult("", "Скрипт в таблице старый. Вставьте новый Code.gs и обновите развёртывание.")
                } else {
                    ReportResult("", describeError(json))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReportResult("", "Нет связи: ${e.localizedMessage ?: "ошибка сети"}")
        }
    }

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
            } catch (e: CancellationException) {
                throw e
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

    /** Загружает операции из Google Таблицы: обновляет изменённые и добавляет новые строки. */
    suspend fun pull(dao: BudgetDao, settings: AppSettings): String = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank() || settings.token.isBlank()) {
            return@withContext "Синхронизация не настроена: укажите URL и токен."
        }
        try {
            val payload = JSONObject().apply {
                put("token", settings.token)
                put("action", "getOperations")
            }
            val request = Request.Builder().url(settings.apiUrl)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext "Ошибка HTTP ${resp.code}."
                val json = try { JSONObject(body) } catch (e: Exception) {
                    return@withContext "Ответ не похож на API. Обновите Code.gs и сделайте новое развёртывание."
                }
                if (!json.optBoolean("ok")) {
                    return@withContext if (json.optString("error") == "unknown_action") {
                        "Скрипт в таблице не поддерживает загрузку. Обновите Code.gs."
                    } else {
                        describeError(json)
                    }
                }
                val arr = json.optJSONArray("operations") ?: return@withContext "В таблице пока нет операций."
                val existingByUid = dao.operationsOnce().associateBy { it.uid }
                var updated = 0
                var added = 0
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val uid = item.optString("uid").trim()
                    val dateStr = item.optString("date").trim()
                    val type = item.optString("type").trim()
                    val amountCents = Math.round(item.optDouble("amount", 0.0) * 100.0)
                    if (uid.isEmpty() || dateStr.isEmpty() || amountCents <= 0L) continue
                    val epochDay = try { LocalDate.parse(dateStr).toEpochDay() } catch (e: Exception) { continue }
                    val transfer = type == OpType.TRANSFER
                    val category = item.optString("category").trim()
                    val comment = item.optString("comment").trim()
                    val person = item.optString("person").trim()
                    val account = if (transfer) item.optString("sourceAccount").trim() else item.optString("paymentMethod").trim()
                    val destAccount = if (transfer) item.optString("destinationAccount").trim() else ""

                    val existing = existingByUid[uid]
                    if (existing != null) {
                        val next = existing.copy(
                            date = epochDay,
                            amountCents = amountCents,
                            type = type,
                            category = category,
                            comment = comment,
                            account = account,
                            destAccount = destAccount,
                            person = person,
                            synced = true
                        )
                        if (next != existing) {
                            dao.updateOperation(next)
                            updated++
                        }
                    } else {
                        dao.insertOperation(
                            Operation(
                                uid = uid,
                                date = epochDay,
                                amountCents = amountCents,
                                type = type,
                                category = category,
                                comment = comment,
                                account = account,
                                destAccount = destAccount,
                                person = person,
                                synced = true
                            )
                        )
                        added++
                    }
                }
                "Загружено из таблицы: новых $added, обновлено $updated."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Нет связи: ${e.localizedMessage ?: "ошибка сети"}"
        }
    }

    /** Двусторонняя синхронизация: сначала отправляет локальные, затем загружает правки из таблицы. */
    suspend fun syncBoth(dao: BudgetDao, settings: AppSettings): String {
        if (settings.apiUrl.isBlank() || settings.token.isBlank()) {
            return "Синхронизация не настроена: укажите URL и токен."
        }
        val pushMsg = push(dao, settings)
        val pullMsg = pull(dao, settings)
        return "$pushMsg $pullMsg"
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
        } catch (e: CancellationException) {
            throw e
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
