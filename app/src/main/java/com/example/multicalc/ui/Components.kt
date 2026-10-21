package com.example.multicalc.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** iPhone 計算機配色 */
object CalcColors {
    val Background = Color.Black
    val Digit = Color(0xFF333333)
    val Function = Color(0xFFA5A5A5)
    val Operator = Color(0xFFFF9F0A)
    val Scientific = Color(0xFF212121)
    val ScientificActive = Color(0xFF5C5C5C)
    val Secondary = Color(0xFF8E8E93)
}

enum class AppMode(val label: String, val icon: ImageVector) {
    BASIC("基本", Icons.Filled.Calculate),
    SCIENTIFIC("工程", Icons.Filled.Functions),
    CURRENCY("匯率換算", Icons.Filled.CurrencyExchange),
}

@Composable
fun MultiCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CalcColors.Operator,
            background = CalcColors.Background,
            surface = Color(0xFF1C1C1E),
        ),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalcButton(
    label: String,
    modifier: Modifier = Modifier,
    background: Color = CalcColors.Digit,
    contentColor: Color = Color.White,
    fontSize: TextUnit = 32.sp,
    icon: ImageVector? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let { lc ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lc()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = contentColor)
        } else {
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** 左下角的切換鍵：點擊後選擇 基本 / 工程 / 匯率換算 */
@Composable
fun ModeButton(
    current: AppMode,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        CalcButton(
            label = "切換模式",
            modifier = Modifier.fillMaxWidth(),
            icon = current.icon,
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    leadingIcon = { Icon(mode.icon, contentDescription = null) },
                    trailingIcon = {
                        if (mode == current) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onModeChange(mode)
                    },
                )
            }
        }
    }
}

/** 依字數縮小字體，讓長數字也能放得下 */
fun autoFontSize(text: String, max: Float, min: Float, fitChars: Int): TextUnit {
    val len = text.length.coerceAtLeast(1)
    val size = if (len <= fitChars) max else (max * fitChars / len).coerceAtLeast(min)
    return size.sp
}
