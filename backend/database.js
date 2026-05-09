const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
});

async function initDb() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id SERIAL PRIMARY KEY,
      username VARCHAR(255) UNIQUE NOT NULL,
      email VARCHAR(255) UNIQUE NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      points INTEGER DEFAULT 0,
      wallet_balance DECIMAL(10,2) DEFAULT 100.00,
      email_verified BOOLEAN DEFAULT FALSE,
      verification_token VARCHAR(255),
      verification_token_expires TIMESTAMP WITH TIME ZONE,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  // 若資料表已存在（舊版本），補上新欄位
  await pool.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE,
      ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255),
      ADD COLUMN IF NOT EXISTS verification_token_expires TIMESTAMP WITH TIME ZONE
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS utensils (
      id SERIAL PRIMARY KEY,
      qr_code VARCHAR(255) UNIQUE NOT NULL,
      type VARCHAR(255) NOT NULL,
      status VARCHAR(50) DEFAULT 'available',
      current_borrower_id INTEGER REFERENCES users(id),
      deposit_amount DECIMAL(10,2) DEFAULT 20.00,
      borrowed_at TIMESTAMP WITH TIME ZONE
    )
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS transactions (
      id SERIAL PRIMARY KEY,
      user_id INTEGER NOT NULL REFERENCES users(id),
      utensil_id INTEGER NOT NULL REFERENCES utensils(id),
      action VARCHAR(50) NOT NULL,
      points_earned INTEGER DEFAULT 0,
      deposit_change DECIMAL(10,2) DEFAULT 0,
      note TEXT,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  // 初始餐具資料（已存在則跳過）
  const utensils = [
    ['UTENSIL-001', '環保筷子'],
    ['UTENSIL-002', '環保湯匙'],
    ['UTENSIL-003', '環保叉子'],
    ['UTENSIL-004', '環保餐盒'],
    ['UTENSIL-005', '環保杯子'],
    ['UTENSIL-006', '環保吸管'],
    ['UTENSIL-007', '環保筷子'],
    ['UTENSIL-008', '環保湯匙'],
  ];

  for (const [qrCode, type] of utensils) {
    await pool.query(
      `INSERT INTO utensils (qr_code, type) VALUES ($1, $2) ON CONFLICT (qr_code) DO NOTHING`,
      [qrCode, type]
    );
  }

  console.log('資料庫初始化完成');
}

module.exports = { pool, initDb };
