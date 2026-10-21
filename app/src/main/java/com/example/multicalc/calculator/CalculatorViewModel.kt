package com.example.multicalc.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.random.Random

data class HistoryEntry(val expression: String, val result: String, val value: Double)

class CalculatorViewModel : ViewModel() {

    /** 運算式以「按鍵單位」儲存，退格時 sin( 這類函數會整個刪除 */
    private val tokens = mutableStateListOf<String>()

    var isRadians by mutableStateOf(false); private set
    var isSecond by mutableStateOf(false); private set
    var memory by mutableStateOf<Double?>(null); private set
    var justEvaluated by mutableStateOf(false); private set
    /** 按下 = 之後，上方顯示的原運算式 */
    var lastExpression by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null); private set
    val history = mutableStateListOf<HistoryEntry>()

    private var lastValue = 0.0

    val expression: String get() = tokens.joinToString("")

    val showAllClear: Boolean get() = tokens.isEmpty() || justEvaluated || error != null

    /** 主顯示行 */
    val mainText: String
        get() = when {
            error != null -> error!!
            justEvaluated -> Evaluator.format(lastValue)
            tokens.isEmpty() -> "0"
            else -> expression
        }

    /** 輸入中即時預覽的結果（灰色小字） */
    val previewText: String
        get() {
            if (justEvaluated) return lastExpression
            if (error != null || tokens.size < 2) return ""
            if (tokens.all { it.isNumberPart() }) return ""
            return runCatching { Evaluator.format(evaluate(expression)) }.getOrDefault("")
        }

    private fun evaluate(expr: String) = Evaluator(isRadians).evaluate(expr)

    // ---------- 按鍵 ----------

    fun onDigit(d: String) {
        startFreshIfNeeded()
        val num = currentNumber()
        if (num == "0" && d != ".") tokens.removeAt(tokens.lastIndex)
        if (d == ".") {
            if (num.contains('.')) return
            if (num.isEmpty()) tokens += "0"
        }
        tokens += d
    }

    /** 常數、函數、左括號等會開始新數值的按鍵 */
    fun onOperand(token: String) {
        startFreshIfNeeded()
        // 5 接 10ˣ 會變成 510^(，因此數字開頭的 token 前面要補乘號
        if (token.first().isDigit() && tokens.lastOrNull()?.isNumberPart() == true) tokens += "×"
        tokens += token
    }

    fun onRandom() = onOperand(raw(Random.nextDouble()))

    fun onMemoryRecall() {
        memory?.let { onOperand(raw(it)) }
    }

    /** + − × ÷ */
    fun onOperator(op: String) {
        continueFromResult()
        closeNegativeNumber()
        val last = tokens.lastOrNull()
        when {
            last == null -> if (op == "−") tokens += op else tokens += listOf("0", op)
            last in BINARY_OPS -> {
                if (op == "−" && (last == "×" || last == "÷")) tokens += op
                else tokens[tokens.lastIndex] = op
            }
            last == "(−" -> Unit
            last.endsWith("(") -> if (op == "−") tokens += op
            else -> tokens += op
        }
    }

    /** 接在數值後面的運算：² ³ ! % ^( ^(−1) 等 */
    fun onPostfix(token: String) {
        continueFromResult()
        if (tokens.isEmpty()) tokens += "0"
        closeNegativeNumber()
        if (tokens.last() in BINARY_OPS) tokens.removeAt(tokens.lastIndex)
        tokens += token
    }

    fun onCloseParen() {
        if (justEvaluated) return
        val open = tokens.sumOf { t -> t.count { it == '(' } }
        val close = tokens.sumOf { t -> t.count { it == ')' } }
        if (open > close && tokens.lastOrNull() !in BINARY_OPS) tokens += ")"
    }

    /** EE：科學記號，例如 1.5 EE 3 = 1500 */
    fun onExponent() {
        startFreshIfNeeded()
        val num = currentNumber()
        if (num.isEmpty() || num.contains('E')) return
        tokens += "E"
    }

    fun onToggleSign() {
        if (justEvaluated || error != null) {
            val v = -lastValue
            clearAll()
            if (v != 0.0) tokens += raw(v)
            return
        }
        // 已經是 (−5) 這種完整的負數 → 變回正數
        val last = tokens.lastOrNull()
        if (last != null && last.startsWith("(−") && last.endsWith(")")) {
            tokens[tokens.lastIndex] = last.removePrefix("(−").removeSuffix(")")
            return
        }
        // 找到最後一個數字的起點
        var i = tokens.size
        while (i > 0 && tokens[i - 1].isNumberPart()) i--
        if (i == tokens.size) {
            tokens += "(−"
        } else if (i > 0 && tokens[i - 1] == "(−") {
            tokens.removeAt(i - 1)
        } else {
            tokens.add(i, "(−")
        }
    }

    fun onBackspace() {
        when {
            error != null || justEvaluated -> clearAll()
            tokens.isNotEmpty() -> tokens.removeAt(tokens.lastIndex)
        }
    }

    fun clearAll() {
        tokens.clear()
        justEvaluated = false
        lastExpression = ""
        error = null
    }

    fun onEquals() {
        if (tokens.isEmpty() || justEvaluated) return
        val expr = expression
        try {
            val v = evaluate(expr)
            lastValue = v
            lastExpression = expr
            justEvaluated = true
            error = null
            history.add(0, HistoryEntry(expr, Evaluator.format(v), v))
            if (history.size > 50) history.removeAt(history.lastIndex)
        } catch (e: CalcException) {
            error = e.message
            lastExpression = expr
        }
    }

    fun toggleRadians() { isRadians = !isRadians }
    fun toggleSecond() { isSecond = !isSecond }

    fun memoryClear() { memory = null }
    fun memoryAdd() = updateMemory(1.0)
    fun memorySubtract() = updateMemory(-1.0)

    /** 與 iPhone 相同：m+ / m− 會結束目前輸入，下一個數字重新開始 */
    private fun updateMemory(sign: Double) {
        val v = currentValue() ?: return
        memory = (memory ?: 0.0) + sign * v
        if (!justEvaluated) {
            lastExpression = expression
            lastValue = v
            justEvaluated = true
        }
    }

    /** 點歷史紀錄：把該筆結果帶入目前運算式 */
    fun useHistory(entry: HistoryEntry) = onOperand(raw(entry.value))

    // ---------- 內部 ----------

    private fun currentValue(): Double? = when {
        justEvaluated -> lastValue
        tokens.isEmpty() -> null
        else -> runCatching { evaluate(expression) }.getOrNull()
    }

    private fun startFreshIfNeeded() {
        if (justEvaluated || error != null) clearAll()
    }

    /** 按運算子時，從上一次結果繼續算 */
    private fun continueFromResult() {
        if (error != null) clearAll()
        if (justEvaluated) {
            val v = lastValue
            clearAll()
            tokens += raw(v)
        }
    }

    /** ± 產生的「(−數字」在接運算子前補上右括號，讓 (−2)² = 4 */
    private fun closeNegativeNumber() {
        var i = tokens.size
        while (i > 0 && tokens[i - 1].isNumberPart()) i--
        if (i < tokens.size && i > 0 && tokens[i - 1] == "(−") tokens += ")"
    }

    private fun currentNumber(): String {
        var i = tokens.size
        while (i > 0 && tokens[i - 1].isNumberPart()) i--
        return tokens.subList(i, tokens.size).joinToString("")
    }

    /** 把結果轉成可再次解析的文字（負數加括號，科學記號用 E） */
    private fun raw(v: Double): String {
        val s = Evaluator.format(v, grouping = false).replace('e', 'E').replace('-', '−')
        return if (v < 0) "(${s})" else s
    }

    private fun String.isNumberPart() =
        all { it.isDigit() || it == '.' || it == 'E' }

    companion object {
        val BINARY_OPS = setOf("+", "−", "×", "÷")
    }
}
