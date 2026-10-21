package com.example.multicalc.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multicalc.ui.AppMode
import com.example.multicalc.ui.CalcButton
import com.example.multicalc.ui.CalcColors
import com.example.multicalc.ui.ModeButton
import com.example.multicalc.ui.autoFontSize

/** 一顆按鍵的定義；kind = MODE 代表「切換模式」鍵 */
private data class Key(
    val label: String,
    val kind: Kind,
    val highlighted: Boolean = false,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
) {
    enum class Kind { DIGIT, FUNCTION, OPERATOR, SCIENTIFIC, MODE }
}

@Composable
fun CalculatorScreen(
    vm: CalculatorViewModel,
    mode: AppMode,
    onModeChange: (AppMode) -> Unit,
) {
    var showHistory by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val landscape = maxWidth > maxHeight
        // 橫放時自動顯示工程計算機（與 iPhone 相同）
        val scientific = mode == AppMode.SCIENTIFIC || landscape
        val gap = if (scientific) 8.dp else 12.dp

        val basic = basicKeys(vm)
        val sci = if (scientific) scientificKeys(vm) else emptyList()

        val rows: List<List<Key>>
        val rowHeight: Dp
        if (landscape) {
            rows = sci.zip(basic) { s, b -> s + b }       // 每列 6 + 4 = 10 顆
            rowHeight = (maxHeight * 0.74f - gap * 4) / 5
        } else if (scientific) {
            rows = sci + basic
            val cellW = (maxWidth - gap * 5) / 6
            rowHeight = minOf(cellW, (maxHeight * 0.70f - gap * 9) / 10)
        } else {
            rows = basic
            val cellW = (maxWidth - gap * 3) / 4
            rowHeight = minOf(cellW, (maxHeight * 0.72f - gap * 4) / 5)
        }

        Column(Modifier.fillMaxSize()) {
            // ---- 顯示區 ----
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Row(
                    Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Filled.History, "歷史紀錄", tint = CalcColors.Operator)
                    }
                    if (scientific) {
                        Text(
                            if (vm.isRadians) "Rad" else "Deg",
                            color = CalcColors.Secondary, fontSize = 14.sp,
                        )
                    }
                    if (vm.memory != null) {
                        Text("  M", color = CalcColors.Secondary, fontSize = 14.sp)
                    }
                }
                Display(
                    vm = vm,
                    maxSize = if (scientific) 54f else 84f,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            Spacer(Modifier.height(8.dp))

            // ---- 按鍵區 ----
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                rows.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().height(rowHeight),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        row.forEach { key ->
                            KeyView(key, scientific, mode, onModeChange, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showHistory) HistorySheet(vm) { showHistory = false }
}

@Composable
private fun Display(vm: CalculatorViewModel, maxSize: Float, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        val preview = vm.previewText
        Text(
            text = preview,
            color = CalcColors.Secondary,
            fontSize = 26.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState(), reverseScrolling = true),
        )
        val main = vm.mainText
        Text(
            text = main,
            color = Color.White,
            fontSize = autoFontSize(main, max = maxSize, min = 30f, fitChars = if (maxSize > 60) 7 else 14),
            fontWeight = FontWeight.Light,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState(), reverseScrolling = true),
        )
    }
}

@Composable
private fun KeyView(
    key: Key,
    scientific: Boolean,
    mode: AppMode,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier,
) {
    if (key.kind == Key.Kind.MODE) {
        ModeButton(current = mode, onModeChange = onModeChange, modifier = modifier)
        return
    }
    val (bg, fg) = when (key.kind) {
        Key.Kind.DIGIT -> CalcColors.Digit to Color.White
        Key.Kind.FUNCTION -> CalcColors.Function to Color.Black
        Key.Kind.OPERATOR -> CalcColors.Operator to Color.White
        else -> (if (key.highlighted) CalcColors.ScientificActive else CalcColors.Scientific) to Color.White
    }
    val fontSize = when {
        key.kind == Key.Kind.SCIENTIFIC -> 16.sp
        scientific -> 24.sp
        else -> 36.sp
    }
    CalcButton(
        label = key.label,
        background = bg,
        contentColor = fg,
        fontSize = fontSize,
        onLongClick = key.onLongClick,
        onClick = key.onClick,
        modifier = modifier,
    )
}

private fun basicKeys(vm: CalculatorViewModel): List<List<Key>> {
    fun d(n: String) = Key(n, Key.Kind.DIGIT) { vm.onDigit(n) }
    fun op(o: String) = Key(o, Key.Kind.OPERATOR) { vm.onOperator(o) }
    val clear = if (vm.showAllClear) Key("AC", Key.Kind.FUNCTION) { vm.clearAll() }
    else Key("⌫", Key.Kind.FUNCTION, onLongClick = vm::clearAll) { vm.onBackspace() }
    return listOf(
        listOf(clear, Key("±", Key.Kind.FUNCTION) { vm.onToggleSign() }, Key("%", Key.Kind.FUNCTION) { vm.onPostfix("%") }, op("÷")),
        listOf(d("7"), d("8"), d("9"), op("×")),
        listOf(d("4"), d("5"), d("6"), op("−")),
        listOf(d("1"), d("2"), d("3"), op("+")),
        listOf(Key("", Key.Kind.MODE) {}, d("0"), d("."), Key("=", Key.Kind.OPERATOR) { vm.onEquals() }),
    )
}

private fun scientificKeys(vm: CalculatorViewModel): List<List<Key>> {
    val second = vm.isSecond
    fun k(label: String, onClick: () -> Unit) = Key(label, Key.Kind.SCIENTIFIC, onClick = onClick)
    fun fn(label: String, token: String) = k(label) { vm.onOperand(token) }
    fun trig(name: String) = if (second) fn("$name⁻¹", "$name⁻¹(") else fn(name, "$name(")
    return listOf(
        listOf(
            k("(") { vm.onOperand("(") }, k(")") { vm.onCloseParen() },
            k("mc") { vm.memoryClear() }, k("m+") { vm.memoryAdd() },
            k("m−") { vm.memorySubtract() }, k("mr") { vm.onMemoryRecall() },
        ),
        listOf(
            Key("2nd", Key.Kind.SCIENTIFIC, highlighted = second) { vm.toggleSecond() },
            k("x²") { vm.onPostfix("²") }, k("x³") { vm.onPostfix("³") },
            k("xʸ") { vm.onPostfix("^(") }, fn("eˣ", "e^("),
            if (second) fn("2ˣ", "2^(") else fn("10ˣ", "10^("),
        ),
        listOf(
            k("1/x") { vm.onPostfix("^(−1)") }, fn("²√x", "√("), fn("³√x", "∛("),
            k("ʸ√x") { vm.onPostfix("^(1÷") }, fn("ln", "ln("),
            if (second) fn("log₂", "log₂(") else fn("log₁₀", "log("),
        ),
        listOf(
            k("x!") { vm.onPostfix("!") }, trig("sin"), trig("cos"), trig("tan"),
            fn("e", "e"), k("EE") { vm.onExponent() },
        ),
        listOf(
            k(if (vm.isRadians) "Deg" else "Rad") { vm.toggleRadians() },
            trig("sinh"), trig("cosh"), trig("tanh"),
            fn("π", "π"), k("Rand") { vm.onRandom() },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(vm: CalculatorViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("歷史紀錄", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.history.clear() }) { Text("清除") }
        }
        if (vm.history.isEmpty()) {
            Text(
                "尚無紀錄",
                color = CalcColors.Secondary,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(vm.history) { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.useHistory(entry)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(entry.expression, color = CalcColors.Secondary, fontSize = 16.sp)
                    Text(entry.result, fontSize = 26.sp)
                }
                HorizontalDivider(color = Color(0xFF2C2C2E))
            }
        }
    }
}
