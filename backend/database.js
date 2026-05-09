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
      role VARCHAR(20) DEFAULT 'user',
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  // 若已存在舊資料表，補上 role 欄位（Migration）
  await pool.query(`
    ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'user'
  `);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS utensils (
      id SERIAL PRIMARY KEY,
      qr_code VARCHAR(255) UNIQUE NOT NULL,
      type VARCHAR(255) NOT NULL,
      status VARCHAR(50) DEFAULT 'available',
      current_borrower_id INTEGER REFERENCES users(id),
      deposit_amount DECIMAL(10,2) DEFAULT 20.00,
      add_quantity INTEGER DEFAULT 0,
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

  await pool.query(`
    CREATE TABLE IF NOT EXISTS lottery_numbers (
      id SERIAL PRIMARY KEY,
      ticket_number VARCHAR(10) UNIQUE NOT NULL,
      utensil_id INTEGER REFERENCES utensils(id),
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  const utensils = [
    ['UTENSIL-001', 'Chopsticks'],
    ['UTENSIL-002', 'Spoon'],
    ['UTENSIL-003', 'Fork'],
    ['UTENSIL-004', 'Lunchbox'],
    ['UTENSIL-005', 'Cup'],
    ['UTENSIL-006', 'Straw'],
    ['UTENSIL-007', 'Chopsticks'],
    ['UTENSIL-008', 'Spoon'],
    ['tool 1', 'tool 1'],
    ['tool 2', 'tool 2'],
  ];
  for (const [qrCode, type] of utensils) {
    await pool.query(
      'INSERT INTO utensils (qr_code, type) VALUES ($1, $2) ON CONFLICT (qr_code) DO NOTHING',
      [qrCode, type]
    );
  }
  console.log('DB init complete');
}

module.exports = { pool, initDb };
