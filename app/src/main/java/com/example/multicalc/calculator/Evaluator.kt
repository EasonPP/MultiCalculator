package com.example.multicalc.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

class CalcException(message: String) : Exception(message)

/**
 * 運算式解析器（遞迴下降）。
 *
 * 支援：+ − × ÷ ^ ( ) % ! ² ³、隱含乘法（2π、3(4)、2sin(30)）、
 * 科學記號 E（1.5E3）、常數 π e、未關閉的括號會自動補上。
 *
 * 文法：
 *   expr    := term (('+' | '−') term)*
 *   term    := unary (('×' | '÷') unary | <隱含乘法> unary)*
 *   unary   := ('−' | '+') unary | power
 *   power   := postfix ('^' unary)?          // 右結合，且 −2^2 = −4
 *   postfix := primary ('!' | '%' | '²' | '³')*
 *   primary := number | 'π' | 'e' | func '(' expr ')' | '(' expr ')'
 */
class Evaluator(private val isRadians: Boolean) {

    private lateinit var s: String
    private var pos = 0

    fun evaluate(input: String): Double {
        s = input.replace(" ", "").replace(",", "")
        pos = 0
        if (s.isEmpty()) throw CalcException("空白")
        val v = parseExpr().value
        if (pos < s.length) throw CalcException("語法錯誤")
        if (v.isNaN() || v.isInfinite()) throw CalcException("錯誤")
        return v
    }

    /** isPercent：此項為單獨的百分比，讓 a + b% 依 iPhone 規則算成 a + a×b/100 */
    private data class Term(val value: Double, val isPercent: Boolean = false)

    private fun peek(): Char? = if (pos < s.length) s[pos] else null

    private fun parseExpr(): Term {
        var left = parseTerm()
        while (true) {
            val c = peek()
            if (c != '+' && c != '−' && c != '-') break
            pos++
            val right = parseTerm()
            val r = if (right.isPercent) left.value * right.value else right.value
            left = Term(if (c == '+') left.value + r else left.value - r)
        }
        return left
    }

    private fun parseTerm(): Term {
        var left = parseUnary()
        while (true) {
            val c = peek() ?: break
            when {
                c == '×' || c == '*' -> { pos++; left = Term(left.value * parseUnary().value) }
                c == '÷' || c == '/' -> {
                    pos++
                    val d = parseUnary().value
                    if (d == 0.0) throw CalcException("無法除以 0")
                    left = Term(left.value / d)
                }
                startsPrimary(c) -> left = Term(left.value * parseUnary().value)
                else -> break
            }
        }
        return left
    }

    private fun startsPrimary(c: Char) =
        c.isDigit() || c == '.' || c == '(' || c == 'π' || c.isLetter() || c == '√' || c == '∛'

    private fun parseUnary(): Term {
        return when (peek()) {
            '−', '-' -> { pos++; val t = parseUnary(); Term(-t.value, t.isPercent) }
            '+' -> { pos++; parseUnary() }
            else -> parsePower()
        }
    }

    private fun parsePower(): Term {
        val base = parsePostfix()
        if (peek() == '^') {
            pos++
            val exp = parseUnary().value
            return Term(pow(base.value, exp))
        }
        return base
    }

    private fun pow(b: Double, e: Double): Double {
        // 讓 (−8)^(1/3) 這類奇數次方根得到實數解
        if (b < 0 && e != floor(e)) {
            val inv = 1.0 / e
            val n = inv.roundToLong()
            if (abs(inv - n) < 1e-9 && n % 2L != 0L) return -(-b).pow(e)
        }
        return b.pow(e)
    }

    private fun parsePostfix(): Term {
        var t = parsePrimary()
        loop@ while (true) {
            t = when (peek()) {
                '!' -> { pos++; Term(factorial(t.value)) }
                '%' -> { pos++; Term(t.value / 100.0, isPercent = true) }
                '²' -> { pos++; Term(t.value * t.value) }
                '³' -> { pos++; Term(t.value * t.value * t.value) }
                else -> break@loop
            }
        }
        return t
    }

    private fun parsePrimary(): Term {
        val c = peek() ?: throw CalcException("運算式不完整")
        return when {
            c.isDigit() || c == '.' -> Term(parseNumber())
            c == '(' -> { pos++; Term(parseGroup()) }
            c == 'π' -> { pos++; Term(PI) }
            c == '√' -> { pos++; Term(sqrtChecked(parseFuncArg())) }
            c == '∛' -> { pos++; Term(cbrt(parseFuncArg())) }
            c.isLetter() -> parseFunctionOrConstant()
            else -> throw CalcException("語法錯誤")
        }
    }

    /** 已讀過 '('，讀內容並吃掉 ')'；結尾缺少的括號自動補上 */
    private fun parseGroup(): Double {
        val v = parseExpr().value
        if (peek() == ')') pos++
        else if (pos < s.length) throw CalcException("括號不對稱")
        return v
    }

    private fun parseFuncArg(): Double =
        if (peek() == '(') { pos++; parseGroup() } else parseUnary().value

    private fun parseNumber(): Double {
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (peek() == 'E') {
            pos++
            if (peek() == '−' || peek() == '-' || peek() == '+') pos++
            while (pos < s.length && s[pos].isDigit()) pos++
        }
        val text = s.substring(start, pos).replace('−', '-')
        return text.toDoubleOrNull() ?: throw CalcException("數字格式錯誤")
    }

    private fun parseFunctionOrConstant(): Term {
        // 沒有函數以 e 開頭，所以 e 一律是常數（esin(30) = e × sin(30)）
        if (s[pos] == 'e') { pos++; return Term(E) }
        val start = pos
        while (pos < s.length && (s[pos].isLetter() || s[pos] in "⁻¹₂₁₀")) pos++
        val name = s.substring(start, pos)
        val f: (Double) -> Double = when (name) {
            "sin" -> { x -> sin(toRad(x)) }
            "cos" -> { x -> cos(toRad(x)) }
            "tan" -> { x -> tanChecked(x) }
            "sin⁻¹" -> { x -> fromRad(asin(x)) }
            "cos⁻¹" -> { x -> fromRad(acos(x)) }
            "tan⁻¹" -> { x -> fromRad(atan(x)) }
            "sinh" -> ::sinh
            "cosh" -> ::cosh
            "tanh" -> ::tanh
            "sinh⁻¹" -> ::asinh
            "cosh⁻¹" -> ::acosh
            "tanh⁻¹" -> ::atanh
            "ln" -> { x -> logChecked(x) { ln(it) } }
            "log", "log₁₀" -> { x -> logChecked(x) { log10(it) } }
            "log₂" -> { x -> logChecked(x) { log2(it) } }
            else -> throw CalcException("未知函數 $name")
        }
        return Term(cleanTrig(f(parseFuncArg())))
    }

    private fun toRad(x: Double) = if (isRadians) x else Math.toRadians(x)
    private fun fromRad(x: Double) = if (isRadians) x else Math.toDegrees(x)

    private fun tanChecked(x: Double): Double {
        // 角度模式下 tan(90)、tan(270)… 無定義
        if (!isRadians && abs(((x % 180) + 180) % 180 - 90) < 1e-12) throw CalcException("錯誤")
        return tan(toRad(x))
    }

    private fun cleanTrig(v: Double) = if (abs(v) < 1e-14) 0.0 else v

    private fun sqrtChecked(x: Double): Double {
        if (x < 0) throw CalcException("錯誤")
        return sqrt(x)
    }

    private inline fun logChecked(x: Double, f: (Double) -> Double): Double {
        if (x <= 0) throw CalcException("錯誤")
        return f(x)
    }

    private fun factorial(x: Double): Double {
        if (x < 0 || x != floor(x)) throw CalcException("錯誤")
        if (x > 170) throw CalcException("數值過大")
        var r = 1.0
        for (i in 2..x.toInt()) r *= i
        return r
    }

    companion object {
        /** iPhone 風格輸出：最多 10 位有效數字、千分位；過大或過小時用科學記號 */
        fun format(value: Double, grouping: Boolean = true): String {
            if (value == 0.0 || abs(value) < 1e-100) return "0"
            val bd = BigDecimal(value).round(MathContext(10, RoundingMode.HALF_UP))
            val absBd = bd.abs()
            if (absBd >= BigDecimal("1e15") || absBd < BigDecimal("1e-9")) {
                val str = bd.stripTrailingZeros().toString()  // 例如 1.2345E+20
                val mantissa = str.substringBefore('E')
                val exponent = str.substringAfter('E', "0").removePrefix("+")
                return "${mantissa}e$exponent"
            }
            val plain = bd.stripTrailingZeros().toPlainString()
            if (!grouping) return plain
            val df = DecimalFormat("#,##0.##########", DecimalFormatSymbols(Locale.US))
            return df.format(BigDecimal(plain))
        }
    }
}
