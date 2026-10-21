# 多功能計算機（Android）

仿 iPhone（iOS 18）計算機的 Android App，使用 Kotlin + Jetpack Compose 開發。
點左下角的 🧮 鍵可以切換三種模式：**基本／工程／匯率換算**。

## 功能

### 基本計算機
- iPhone 配色與圓形按鍵（灰色功能鍵、橘色運算鍵）
- 輸入時直接顯示完整算式，下方灰字即時預覽結果
- `⌫` 退格（長按可全部清除）、`AC`、`±`、`%`（`50 + 10%` = 55，與 iPhone 相同）
- 運算子優先順序、千分位、超大或超小的數字自動改用科學記號
- 歷史紀錄（左上角時鐘圖示），點一筆紀錄即可把結果帶回目前算式

### 工程計算機（手機橫放時自動切換）
- `( )`、`mc m+ m− mr` 記憶鍵
- `2nd` 切換反函數：sin⁻¹ cos⁻¹ tan⁻¹、sinh⁻¹…、2ˣ、log₂
- x² x³ xʸ eˣ 10ˣ、1/x ²√x ³√x ʸ√x、ln log₁₀、x!、π e、EE、Rand
- Rad / Deg 切換
- 隱含乘法（`2π`、`3(4)`、`2sin(30)`），結尾沒關的括號會自動補上

### 匯率換算
- 從網路抓取最新匯率，涵蓋 160 多種貨幣；主要來源 open.er-api.com，失敗時改用 Frankfurter（歐洲央行資料）
- 點貨幣代碼即可換成任何貨幣，可用代碼或中文名稱搜尋（例如「日圓」）
- `⇅` 交換兩種貨幣；點任一列就改由那一列輸入金額
- 匯率會存在手機上，沒有網路時也能用；超過 30 分鐘會自動更新，也可以按右上角手動更新
- 會記住你上次選的兩種貨幣

> 註：免費的匯率 API 約每天更新一次，與銀行的即時牌告匯率可能有些差距。

## 如何編譯與安裝

### 方法一：Android Studio（推薦）
1. 安裝 [Android Studio](https://developer.android.com/studio)
2. 選 `File → Open`，開啟 `MultiCalculator` 資料夾，等 Gradle 同步完成
3. 手機開啟「開發人員選項 → USB 偵錯」，接上電腦後按 ▶ Run

### 方法二：命令列
```bash
./gradlew assembleDebug          # 產生 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 跑計算引擎的單元測試
```

### 方法三：GitHub 自動編譯
把專案推到 GitHub 後，`.github/workflows/build-apk.yml` 會自動編譯，
到 Actions 頁面下載 `MultiCalculator-apk`，再把 APK 傳到手機安裝。

## 專案結構
```
app/src/main/java/com/example/multicalc/
├── MainActivity.kt                 # 進入點，切換三種模式
├── ui/Components.kt                # iPhone 風格按鍵、配色、模式選單
├── calculator/
│   ├── Evaluator.kt                # 算式解析與計算引擎
│   ├── CalculatorViewModel.kt      # 按鍵邏輯、記憶、歷史紀錄
│   └── CalculatorScreen.kt         # 基本／工程計算機畫面
└── currency/
    ├── CurrencyRepository.kt       # 下載與快取匯率
    ├── CurrencyViewModel.kt        # 換算邏輯
    └── CurrencyScreen.kt           # 匯率換算畫面與貨幣選擇器
```

需求：Android 8.0（API 26）以上。
