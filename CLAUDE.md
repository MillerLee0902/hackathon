# 環保餐具借還系統 — 專案指示

## 專案概述
技職黑客松專案。掃描 QR Code 實現環保餐具借還系統，含點數獎勵（類似 FamiPoint）。

## 技術棧
- **Android 前端**: Kotlin + Jetpack Compose（Android Studio）
- **後端**: Node.js + Express
- **資料庫**: JSON 檔（`backend/data.json`）— 因 better-sqlite3 在 Node.js 24 無法編譯
- **QR 掃描**: zxing-android-embedded 4.3.0
- **HTTP**: Retrofit2 + OkHttp
- **導航**: Navigation Compose 2.7.7

## 目錄結構
```
hackathon/
├── backend/                   # Node.js 後端
│   ├── server.js
│   ├── database.js            # JSON 讀寫工具
│   ├── data.json              # 資料儲存（自動產生）
│   ├── middleware/auth.js     # JWT 驗證
│   └── routes/
│       ├── auth.js            # POST /api/auth/register, /login
│       ├── users.js           # GET /api/users/me, /qrcode, /transactions
│       ├── utensils.js        # GET /api/utensils
│       └── borrow.js         # POST /api/borrow/borrow, /return
└── app/src/main/java/com/example/hackathon/
    ├── MainActivity.kt        # NavHost，路由: login/register/dashboard/borrow/return/myqrcode/transactions
    ├── data/
    │   ├── RetrofitClient.kt  # BASE_URL: 模擬器用 10.0.2.2:3000，實機改區網 IP
    │   ├── ApiService.kt
    │   └── SessionManager.kt  # JWT 存 SharedPreferences
    ├── model/Models.kt
    └── ui/
        ├── LoginScreen.kt
        ├── RegisterScreen.kt
        ├── DashboardScreen.kt
        ├── BorrowScreen.kt
        ├── ReturnScreen.kt
        ├── MyQrCodeScreen.kt
        └── TransactionScreen.kt
```

## 啟動方式
```bash
# 後端
cd backend
npm start        # port 3000

# Android
# Android Studio 中 Sync Gradle，然後 Run
```

## 商業邏輯
- 新用戶：錢包 $100，點數 0
- 借用餐具：掃餐具 QR（格式 `UTENSIL-001`）→ 扣押金 $20
- 歸還餐具：先掃用戶 QR（格式 `USER-{id}`）→ 再掃餐具 QR → 退 $20 + 加 1 點
- 初始餐具：UTENSIL-001 到 UTENSIL-008

## 開發注意事項
- `weight()` 是 RowScope/ColumnScope 擴充函式，不能直接在一般 Composable 裡用，需透過 `modifier` 參數傳入
- 圖示用 `Icons.Default.*` 時，非標準圖示（QrCodeScanner、AccountBalanceWallet 等）需要 `material-icons-extended` 依賴
- 後端改用純 JSON 檔避免原生模組編譯問題，不要換回 better-sqlite3
- Android 清文字流量（http）已在 AndroidManifest.xml 設定 `usesCleartextTraffic="true"`
