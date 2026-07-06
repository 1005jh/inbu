package com.inbu.ledger.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.inbu.ledger.BuildConfig
import com.inbu.ledger.ui.payment.PaymentAllocation
import com.inbu.ledger.ui.payment.WorkerPayment
import com.inbu.ledger.ui.sites.SiteStatus
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.work.WorkRecordSummary
import com.inbu.ledger.ui.work.WorkerWorkAmount
import com.inbu.ledger.ui.workers.WorkerSummary
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

data class LedgerSnapshot(
    val sites: List<SiteSummary>,
    val deletedSites: List<SiteSummary>,
    val workers: List<WorkerSummary>,
    val workRecords: List<WorkRecordSummary>,
    val deletedWorkRecords: List<WorkRecordSummary>,
    val payments: List<WorkerPayment>,
)

class LedgerApiClient(context: Context) {
    private val preferences = context.getSharedPreferences("inbu_session", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    val hasSession: Boolean get() = preferences.getString(TOKEN_KEY, null) != null

    fun clearSession() {
        preferences.edit().remove(TOKEN_KEY).apply()
    }

    fun exchangeKakaoToken(accessToken: String, callback: (Result<LedgerSnapshot>) -> Unit) {
        execute(callback) {
            val response = request(
                method = "POST",
                path = "/api/v1/auth/kakao",
                body = JSONObject().put("accessToken", accessToken),
                authenticated = false,
            )
            preferences.edit().putString(TOKEN_KEY, response.getString("token")).apply()
            request("GET", "/api/v1/ledger").toLedgerSnapshot()
        }
    }

    fun loginForDevelopment(callback: (Result<LedgerSnapshot>) -> Unit) {
        execute(callback) {
            check(BuildConfig.DEBUG && BuildConfig.API_DEV_AUTH) {
                "개발 로그인은 디버그 테스트 설정에서만 사용할 수 있어요."
            }
            val response = request(
                method = "POST",
                path = "/api/v1/auth/dev",
                body = JSONObject(),
                authenticated = false,
            )
            preferences.edit().putString(TOKEN_KEY, response.getString("token")).apply()
            request("GET", "/api/v1/ledger").toLedgerSnapshot()
        }
    }

    fun loadLedger(callback: (Result<LedgerSnapshot>) -> Unit) = execute(callback) {
        request("GET", "/api/v1/ledger").toLedgerSnapshot()
    }

    fun createSite(name: String, memo: String, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "POST", "/api/v1/sites", JSONObject().put("name", name).put("memo", memo).put("status", "Active"), callback,
    )

    fun updateSite(site: SiteSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "PUT", "/api/v1/sites/${site.id}", JSONObject().put("name", site.name).put("memo", site.memo).put("status", site.status.name), callback,
    )

    fun trashSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("DELETE", "/api/v1/sites/$id", null, callback)
    fun restoreSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("POST", "/api/v1/sites/$id/restore", JSONObject(), callback)
    fun deleteSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("DELETE", "/api/v1/sites/$id/permanent", null, callback)

    fun createWorker(worker: WorkerSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "POST", "/api/v1/workers", worker.toRequestJson(), callback,
    )

    fun updateWorker(worker: WorkerSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "PUT", "/api/v1/workers/${worker.id}", worker.toRequestJson(), callback,
    )

    fun createRecord(record: WorkRecordSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "POST", "/api/v1/records", record.toRequestJson(), callback,
    )

    fun updateRecord(record: WorkRecordSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "PUT", "/api/v1/records/${record.id}", record.toRequestJson(), callback,
    )

    fun trashRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("DELETE", "/api/v1/records/$id", null, callback)
    fun restoreRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("POST", "/api/v1/records/$id/restore", JSONObject(), callback)
    fun deleteRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("DELETE", "/api/v1/records/$id/permanent", null, callback)

    fun settlePayment(workerId: Long, siteId: Long, cutoffEpochDay: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(
        "POST", "/api/v1/payments/settle",
        JSONObject().put("workerId", workerId).put("siteId", siteId).put("cutoffEpochDay", cutoffEpochDay), callback,
    )

    fun cancelPayment(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate("DELETE", "/api/v1/payments/$id", null, callback)

    private fun mutate(method: String, path: String, body: JSONObject?, callback: (Result<LedgerSnapshot>) -> Unit) = execute(callback) {
        request(method, path, body).toLedgerSnapshot()
    }

    private fun <T> execute(callback: (Result<T>) -> Unit, block: () -> T) {
        executor.execute {
            val result = runCatching(block)
            mainHandler.post { callback(result) }
        }
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
    ): JSONObject {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (authenticated) {
                val token = preferences.getString(TOKEN_KEY, null) ?: throw ApiClientException("로그인이 필요해요.")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) {
                if (status == 401) clearSession()
                throw ApiClientException(json.optString("message", "서버 요청에 실패했어요."))
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toLedgerSnapshot() = LedgerSnapshot(
        sites = getJSONArray("sites").mapObjects { toSite() },
        deletedSites = getJSONArray("deletedSites").mapObjects { toSite() },
        workers = getJSONArray("workers").mapObjects { toWorker() },
        workRecords = getJSONArray("workRecords").mapObjects { toRecord() },
        deletedWorkRecords = getJSONArray("deletedWorkRecords").mapObjects { toRecord() },
        payments = getJSONArray("payments").mapObjects { toPayment() },
    )

    private fun JSONObject.toSite() = SiteSummary(
        id = getLong("id"), name = getString("name"), memo = getString("memo"),
        status = SiteStatus.valueOf(getString("status")), totalWorkUnits = getDouble("totalWorkUnits"), unpaidAmount = getLong("unpaidAmount"),
    )

    private fun JSONObject.toWorker(): WorkerSummary {
        val wageJson = getJSONObject("siteDailyWages")
        val wages = wageJson.keys().asSequence().associate { it.toLong() to wageJson.getLong(it) }
        return WorkerSummary(
            id = getLong("id"), name = getString("name"), dailyWage = getLong("dailyWage"), phone = getString("phone"), memo = getString("memo"),
            totalWorkUnits = getDouble("totalWorkUnits"), unpaidAmount = getLong("unpaidAmount"), isActive = getBoolean("isActive"), siteDailyWages = wages,
        )
    }

    private fun JSONObject.toRecord() = WorkRecordSummary(
        id = getLong("id"), dateEpochDay = getLong("dateEpochDay"), siteId = getLong("siteId"),
        workers = getJSONArray("workers").mapObjects { WorkerWorkAmount(getLong("workerId"), getDouble("workUnits"), getLong("dailyWage")) },
        memo = getString("memo"), fuelCost = getLong("fuelCost"), mealCost = getLong("mealCost"),
    )

    private fun JSONObject.toPayment() = WorkerPayment(
        id = getLong("id"), workerId = getLong("workerId"), siteId = getLong("siteId"), settledThroughEpochDay = getLong("settledThroughEpochDay"),
        paidDateEpochDay = getLong("paidDateEpochDay"), amount = getLong("amount"),
        allocations = getJSONArray("allocations").mapObjects { PaymentAllocation(getLong("recordId"), getLong("workerId"), getLong("amount")) },
    )

    private fun WorkerSummary.toRequestJson(): JSONObject {
        val wages = JSONObject().also { json -> siteDailyWages.forEach { (siteId, wage) -> json.put(siteId.toString(), wage) } }
        return JSONObject().put("name", name).put("dailyWage", dailyWage).put("phone", phone).put("memo", memo)
            .put("isActive", isActive).put("siteDailyWages", wages)
    }

    private fun WorkRecordSummary.toRequestJson(): JSONObject = JSONObject()
        .put("dateEpochDay", dateEpochDay).put("siteId", siteId)
        .put("workers", JSONArray().also { array -> workers.forEach { array.put(JSONObject().put("workerId", it.workerId).put("workUnits", it.workUnits).put("dailyWage", it.dailyWage)) } })
        .put("memo", memo).put("fuelCost", fuelCost).put("mealCost", mealCost)

    private inline fun <T> JSONArray.mapObjects(transform: JSONObject.() -> T): List<T> =
        List(length()) { index -> getJSONObject(index).transform() }

    private companion object { const val TOKEN_KEY = "session_token" }
}

class ApiClientException(message: String) : RuntimeException(message)
