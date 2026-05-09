# 環保餐具借還系統 — 專案指示

## 專案概述
技職黑客松專案。掃描 QR Code 實現環保餐具借還系統，含點數獎勵（類似 FamiPoint）。

## 技術棧
- **Android 前端**: Kotlin + Jetpack Compose（Android Studio）
- **後端**: Node.js + Express，部署於 Railway
- **資料庫**: PostgreSQL（Railway 內建 Postgres 服務），使用 `pg` 套件連線
- **QR 掃描**: zxing-android-embedded 4.3.0
- **HTTP**: Retrofit2 + OkHttp
- **導航**: Navigation Compose 2.7.7

## 目錄結構
```
hackathon/
├── backend/                   # Node.js 後端（部署至 Railway）
│   ├── server.js
│   ├── database.js            # PostgreSQL 初始化（pg Pool）
│   ├── mailer.js              # Gmail SMTP 寄信（nodemailer）
│   ├── .env.example           # 環境變數範本
│   ├── middleware/auth.js     # JWT 驗證
│   └── routes/
│       ├── auth.js            # POST /api/auth/register, /login
│       │                      # GET  /api/auth/verify-email?token=xxx
│       │                      # POST /api/auth/resend-verification
│       ├── users.js           # GET /api/users/me, /qrcode, /transactions
│       ├── utensils.js        # GET /api/utensils
│       └── borrow.js          # POST /api/borrow/borrow, /return
└── app/src/main/java/com/example/hackathon/
    ├── MainActivity.kt        # NavHost，路由: login/register/dashboard/borrow/return/myqrcode/transactions
    ├── data/
    │   ├── RetrofitClient.kt  # BASE_URL: Railway 部署後改為 https://<your-app>.railway.app
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
# 本機開發後端（需先有 PostgreSQL，或用 Railway 的 DATABASE_URL）
cd backend
cp .env.example .env      # 填入 DATABASE_URL、JWT_SECRET、GMAIL_USER、GMAIL_APP_PASSWORD
npm start                 # port 3000

# Android
# Android Studio 中 Sync Gradle，然後 Run
# RetrofitClient.kt 的 BASE_URL 填入 Railway 部署後的 https URL
```

## Railway 部署說明
1. 在 Railway 建立新專案，加入 **PostgreSQL** 服務
2. 再加入 **Node.js** 服務，指向 `backend/` 目錄（或設定 Root Directory 為 `backend`）
3. 在 Node.js 服務的 Variables 填入：
   - `DATABASE_URL`：Railway 自動注入（連結 Postgres 服務後可用 `${{Postgres.DATABASE_URL}}`）
   - `JWT_SECRET`：自訂隨機字串
   - `GMAIL_USER`：Gmail 帳號
   - `GMAIL_APP_PASSWORD`：Gmail 應用程式密碼（16 碼）
   - `BASE_URL`：Railway 部署後的 `https://<your-app>.railway.app`
   - `NODE_ENV`：`production`
4. 部署後把 Railway URL 填入 Android `RetrofitClient.kt` 的 `BASE_URL`

## 商業邏輯
- 新用戶：錢包 $100，點數 0
- 借用餐具：掃餐具 QR（格式 `UTENSIL-001`）→ 扣押金 $20
- 歸還餐具：先掃用戶 QR（格式 `USER-{id}`）→ 再掃餐具 QR → 退 $20 + 加 1 點
- 初始餐具：UTENSIL-001 到 UTENSIL-008

## 開發注意事項
- `weight()` 是 RowScope/ColumnScope 擴充函式，不能直接在一般 Composable 裡用，需透過 `modifier` 參數傳入
- 圖示用 `Icons.Default.*` 時，非標準圖示（QrCodeScanner、AccountBalanceWallet 等）需要 `material-icons-extended` 依賴
- 資料庫使用 PostgreSQL（`pg` 套件），透過 `DATABASE_URL` 環境變數連線，不要改回 SQLite 或 JSON 檔
- Railway 部署時後端為 HTTPS，Android `RetrofitClient.kt` 的 BASE_URL 必須用 `https://` 開頭；`AndroidManifest.xml` 的 `usesCleartextTraffic` 僅本機開發時需要
- Email 驗證：註冊後寄 Gmail 驗證信，未點擊連結前登入會收到 403；登入畫面有「重新寄送驗證信」按鈕
- Gmail 寄信需使用「應用程式密碼」（16 碼），不能用 Google 帳號原始密碼
