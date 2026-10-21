package com.example.multicalc.currency

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

class CurrencyViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CurrencyRepository(app)
    private val prefs = app.getSharedPreferences("currency_settings", Context.MODE_PRIVATE)

    var snapshot by mutableStateOf<RatesSnapshot?>(repo.cached()); private set
    var loading by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    var fromCode by mutableStateOf(prefs.getString("from", "USD")!!); private set
    var toCode by mutableStateOf(prefs.getString("to", "TWD")!!); private set

    /** 目前正在輸入的是上面（來源）還是下面（目標）那一列 */
    var inputIsFrom by mutableStateOf(true); private set
    var input by mutableStateOf("1"); private set

    private var refreshJob: Job? = null

    init {
        val s = snapshot
        if (s == null || System.currentTimeMillis() - s.fetchedAt > STALE_MS) refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                snapshot = repo.fetch()
            } catch (e: Exception) {
                errorMessage = if (snapshot == null) "無法取得匯率，請檢查網路後重試"
                else "更新失敗，目前顯示離線匯率"
            } finally {
                loading = false
            }
        }
    }

    val availableCodes: List<String>
        get() {
            val all = snapshot?.rates?.keys ?: POPULAR.toSet()
            val popular = POPULAR.filter { it in all }
            return popular + all.filter { it !in POPULAR }.sorted()
        }

    private val inputValue: Double get() = input.toDoubleOrNull() ?: 0.0

    /** 回傳兩列要顯示的文字 (來源, 目標) */
    val displayPair: Pair<String, String>
        get() {
            val converted = snapshot?.let { s ->
                if (inputIsFrom) s.convert(inputValue, fromCode, toCode)
                else s.convert(inputValue, toCode, fromCode)
            }
            val typed = groupTyped(input)
            val other = converted?.let { formatMoney(it) } ?: "—"
            return if (inputIsFrom) typed to other else other to typed
        }

    /** 例如「1 USD = 32.4512 TWD」 */
    val rateLine: String
        get() {
            val r = snapshot?.convert(1.0, fromCode, toCode) ?: return ""
            return "1 $fromCode = ${formatRate(r)} $toCode"
        }

    fun setFrom(code: String) {
        if (code == toCode) toCode = fromCode
        fromCode = code
        save()
    }

    fun setTo(code: String) {
        if (code == fromCode) fromCode = toCode
        toCode = code
        save()
    }

    fun swap() {
        val f = fromCode
        fromCode = toCode
        toCode = f
        inputIsFrom = !inputIsFrom   // 保留剛剛輸入的數字在同一個貨幣上
        save()
    }

    /** 點某一列，改由該列輸入；把該列目前顯示的數值帶過來 */
    fun selectInputRow(isFrom: Boolean) {
        if (isFrom == inputIsFrom) return
        val shown = (if (isFrom) displayPair.first else displayPair.second).replace(",", "")
        inputIsFrom = isFrom
        input = shown.toDoubleOrNull()?.let { plain(it) } ?: "0"
    }

    fun onKey(key: String) {
        input = when (key) {
            "⌫" -> input.dropLast(1).ifEmpty { "0" }
            "AC" -> "0"
            "." -> if (input.contains('.')) input else "$input."
            else -> when {
                input.replace(".", "").length >= 15 -> input
                input == "0" -> if (key == "00") "0" else key
                else -> input + key
            }
        }
    }

    private fun save() {
        prefs.edit().putString("from", fromCode).putString("to", toCode).apply()
    }

    companion object {
        private const val STALE_MS = 30 * 60 * 1000L

        val POPULAR = listOf(
            "TWD", "USD", "JPY", "EUR", "CNY", "HKD", "KRW", "GBP",
            "AUD", "CAD", "SGD", "THB", "MYR", "VND", "PHP", "CHF", "NZD",
        )

        private val symbols = DecimalFormatSymbols(Locale.US)

        fun formatMoney(v: Double): String {
            val pattern = if (v != 0.0 && kotlin.math.abs(v) < 1) "#,##0.######" else "#,##0.##"
            return DecimalFormat(pattern, symbols).format(v)
        }

        fun formatRate(v: Double): String =
            DecimalFormat(if (v < 1) "#,##0.######" else "#,##0.####", symbols).format(v)

        private fun plain(v: Double): String =
            BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

        /** 輸入中的數字加上千分位，但保留尾端的小數點與 0 */
        private fun groupTyped(s: String): String {
            val intPart = s.substringBefore('.')
            val grouped = DecimalFormat("#,##0", symbols).format(intPart.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            return if (s.contains('.')) grouped + "." + s.substringAfter('.') else grouped
        }

        fun currencyName(code: String): String =
            runCatching { Currency.getInstance(code).getDisplayName(Locale.TRADITIONAL_CHINESE) }
                .getOrDefault(code)

        /** 由貨幣代碼前兩碼組出國旗 emoji（EUR → 🇪🇺） */
        fun flag(code: String): String {
            if (code.length < 2 || code.startsWith("X")) return "🌐"
            val base = 0x1F1E6 - 'A'.code
            return String(Character.toChars(base + code[0].code)) +
                String(Character.toChars(base + code[1].code))
        }
    }
}
