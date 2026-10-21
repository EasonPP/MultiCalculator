package com.example.multicalc.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multicalc.ui.AppMode
import com.example.multicalc.ui.CalcButton
import com.example.multicalc.ui.CalcColors
import com.example.multicalc.ui.ModeButton
import com.example.multicalc.ui.autoFontSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrencyScreen(
    vm: CurrencyViewModel,
    onModeChange: (AppMode) -> Unit,
) {
    /** 正在挑選哪一列的貨幣：true = 上方, false = 下方, null = 未開啟 */
    var picking by remember { mutableStateOf<Boolean?>(null) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val gap = 12.dp
        val landscape = maxWidth > maxHeight
        val rowHeight = if (landscape) (maxHeight * 0.5f - gap * 3) / 4
        else minOf((maxWidth - gap * 3) / 4, (maxHeight * 0.55f - gap * 3) / 4)

        Column(Modifier.fillMaxSize()) {
            // ---- 標題列 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("匯率換算", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (vm.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = CalcColors.Operator)
                } else {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, "更新匯率", tint = CalcColors.Operator)
                    }
                }
            }

            // ---- 兩列貨幣 ----
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                val (fromText, toText) = vm.displayPair
                CurrencyRow(
                    code = vm.fromCode, amount = fromText, active = vm.inputIsFrom,
                    onPickCurrency = { picking = true },
                    onSelect = { vm.selectInputRow(true) },
                )
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    IconButton(
                        onClick = vm::swap,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CalcColors.Digit),
                    ) {
                        Icon(Icons.Filled.SwapVert, "交換貨幣", tint = CalcColors.Operator)
                    }
                }
                CurrencyRow(
                    code = vm.toCode, amount = toText, active = !vm.inputIsFrom,
                    onPickCurrency = { picking = false },
                    onSelect = { vm.selectInputRow(false) },
                )
                Spacer(Modifier.height(12.dp))
                StatusLine(vm)
            }

            // ---- 數字鍵 ----
            val keys = listOf(
                listOf("7", "8", "9", "⌫"),
                listOf("4", "5", "6", "AC"),
                listOf("1", "2", "3", "⇅"),
                listOf("MODE", "0", ".", "00"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                keys.forEach { row ->
                    Row(Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { k ->
                            val m = Modifier.weight(1f)
                            when (k) {
                                "MODE" -> ModeButton(AppMode.CURRENCY, onModeChange, m)
                                "⌫", "AC" -> CalcButton(k, m, CalcColors.Function, Color.Black, 30.sp) { vm.onKey(k) }
                                "⇅" -> CalcButton(k, m, CalcColors.Operator, Color.White, 30.sp) { vm.swap() }
                                else -> CalcButton(k, m, fontSize = 32.sp) { vm.onKey(k) }
                            }
                        }
                    }
                }
            }
        }
    }

    picking?.let { isFrom ->
        CurrencyPicker(
            codes = vm.availableCodes,
            selected = if (isFrom) vm.fromCode else vm.toCode,
            onDismiss = { picking = null },
            onPick = { code ->
                if (isFrom) vm.setFrom(code) else vm.setTo(code)
                picking = null
            },
        )
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    amount: String,
    active: Boolean,
    onPickCurrency: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (active) Modifier.border(1.5.dp, CalcColors.Operator, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 點左側選貨幣
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .clickable(onClick = onPickCurrency)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(CurrencyViewModel.flag(code), fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("$code ▾", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    CurrencyViewModel.currencyName(code),
                    fontSize = 12.sp, color = CalcColors.Secondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(96.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            amount,
            fontSize = autoFontSize(amount, max = 44f, min = 20f, fitChars = 9),
            fontWeight = FontWeight.Light,
            color = if (active) Color.White else Color(0xFFD1D1D6),
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatusLine(vm: CurrencyViewModel) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (vm.rateLine.isNotEmpty()) {
            Text(vm.rateLine, color = CalcColors.Operator, fontSize = 16.sp)
        }
        vm.snapshot?.let { s ->
            val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)
            Text(
                "匯率更新：${fmt.format(Date(s.updatedAt))}（${s.source}）",
                color = CalcColors.Secondary, fontSize = 12.sp,
            )
        }
        vm.errorMessage?.let { Text(it, color = Color(0xFFFF453A), fontSize = 13.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPicker(
    codes: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, codes) {
        val q = query.trim()
        if (q.isEmpty()) codes
        else codes.filter {
            it.contains(q, ignoreCase = true) || CurrencyViewModel.currencyName(it).contains(q, ignoreCase = true)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜尋代碼或名稱，例如 JPY、日圓") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.padding(top = 8.dp, bottom = 24.dp)) {
            items(filtered, key = { it }) { code ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(code) }
                        .background(if (code == selected) Color(0xFF2C2C2E) else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(CurrencyViewModel.flag(code), fontSize = 24.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(code, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                    Text(CurrencyViewModel.currencyName(code), color = CalcColors.Secondary, fontSize = 15.sp)
                    if (code in CurrencyViewModel.POPULAR) {
                        Spacer(Modifier.weight(1f))
                        Text("常用", color = CalcColors.Operator, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
