package com.example.multicalc.currency

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset

/** 以 USD 為基準的匯率表；任兩種貨幣用交叉匯率換算 */
data class RatesSnapshot(
    val rates: Map<String, Double>,
    /** 資料來源公布的更新時間（epoch 毫秒） */
    val updatedAt: Long,
    /** 本機下載的時間（epoch 毫秒） */
    val fetchedAt: Long,
    val source: String,
) {
    fun convert(amount: Double, from: String, to: String): Double? {
        val f = rates[from] ?: return null
        val t = rates[to] ?: return null
        return amount / f * t
    }
}

class CurrencyRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rates_cache", Context.MODE_PRIVATE)

    fun cached(): RatesSnapshot? {
        val json = prefs.getString(KEY_JSON, null) ?: return null
        return runCatching {
            parseErApi(JSONObject(json), prefs.getLong(KEY_FETCHED, 0L))
        }.getOrNull()
    }

    /** 先用 open.er-api.com（160+ 種貨幣），失敗再用 frankfurter（歐洲央行資料） */
    suspend fun fetch(): RatesSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        try {
            val body = httpGet("https://open.er-api.com/v6/latest/USD")
            val json = JSONObject(body)
            if (json.optString("result") != "success") error("API 回傳失敗")
            val snapshot = parseErApi(json, now)
            prefs.edit().putString(KEY_JSON, body).putLong(KEY_FETCHED, now).apply()
            snapshot
        } catch (first: Exception) {
            try {
                fetchFrankfurter(now)
            } catch (second: Exception) {
                throw first
            }
        }
    }

    private fun parseErApi(json: JSONObject, fetchedAt: Long): RatesSnapshot {
        val ratesJson = json.getJSONObject("rates")
        val rates = buildMap {
            ratesJson.keys().forEach { put(it, ratesJson.getDouble(it)) }
        }
        return RatesSnapshot(
            rates = rates,
            updatedAt = json.optLong("time_last_update_unix", fetchedAt / 1000) * 1000,
            fetchedAt = fetchedAt,
            source = "ExchangeRate-API",
        )
    }

    private fun fetchFrankfurter(now: Long): RatesSnapshot {
        val json = JSONObject(httpGet("https://api.frankfurter.app/latest?from=USD"))
        val ratesJson = json.getJSONObject("rates")
        val rates = buildMap {
            put("USD", 1.0)
            ratesJson.keys().forEach { put(it, ratesJson.getDouble(it)) }
        }
        val date = runCatching {
            LocalDate.parse(json.getString("date")).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrDefault(now)
        // frankfurter 的格式不同，不寫入快取，只在本次使用
        return RatesSnapshot(rates, date, now, "Frankfurter (ECB)")
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/json")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val KEY_JSON = "json"
        const val KEY_FETCHED = "fetched_at"
    }
}
