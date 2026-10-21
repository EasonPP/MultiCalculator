package com.example.multicalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multicalc.calculator.CalculatorScreen
import com.example.multicalc.calculator.CalculatorViewModel
import com.example.multicalc.currency.CurrencyScreen
import com.example.multicalc.currency.CurrencyViewModel
import com.example.multicalc.ui.AppMode
import com.example.multicalc.ui.CalcColors
import com.example.multicalc.ui.MultiCalcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiCalcTheme {
                var mode by rememberSaveable { mutableStateOf(AppMode.BASIC) }
                val calcVm: CalculatorViewModel = viewModel()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(CalcColors.Background)
                        .safeDrawingPadding()
                ) {
                    when (mode) {
                        AppMode.BASIC, AppMode.SCIENTIFIC ->
                            CalculatorScreen(calcVm, mode) { mode = it }
                        AppMode.CURRENCY ->
                            CurrencyScreen(viewModel<CurrencyViewModel>()) { mode = it }
                    }
                }
            }
        }
    }
}
