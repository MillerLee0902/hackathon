const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { pool } = require('../database');
const { JWT_SECRET } = require('../middleware/auth');

const router = express.Router();

// POST /api/auth/register
router.post('/register', async (req, res) => {
  const { username, email, password } = req.body;
  if (!username || !email || !password) {
    return res.status(400).json({ success: false, message: '請填寫所有欄位' });
  }

  try {
    const existing = await pool.query(
      'SELECT id FROM users WHERE email = $1 OR username = $2',
      [email, username]
    );
    if (existing.rows.length > 0) {
      return res.status(409).json({ success: false, message: '帳號或電子郵件已被使用' });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    await pool.query(
      `INSERT INTO users
         (username, email, password_hash, email_verified, verification_token, verification_token_expires)
       VALUES ($1, $2, $3, TRUE, NULL, NULL)`,
      [username, email, passwordHash]
    );

    res.status(201).json({
      success: true,
      message: '註冊成功！請直接登入。',
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ success: false, message: '請填寫帳號和密碼' });
  }

  try {
    const result = await pool.query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );
    if (result.rows.length === 0) {
      return res.status(401).json({ success: false, message: '帳號或密碼錯誤' });
    }

    const user = result.rows[0];
    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) {
      return res.status(401).json({ success: false, message: '帳號或密碼錯誤' });
    }

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '7d' });

    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        points: user.points,
        walletBalance: parseFloat(user.wallet_balance),
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  }
});

// GET /api/auth/verify-email?token=xxx
router.get('/verify-email', async (req, res) => {
  const { token } = req.query;
  if (!token) {
    return res.status(400).send(htmlPage('驗證失敗', '缺少驗證 token。', false));
  }

  try {
    const result = await pool.query(
      `SELECT id, username, email_verified, verification_token_expires
       FROM users
       WHERE verification_token = $1`,
      [token]
    );

    if (result.rows.length === 0) {
      return res.status(400).send(htmlPage('驗證失敗', '驗證連結無效或已被使用。', false));
    }

    const user = result.rows[0];

    if (user.email_verified) {
      return res.send(htmlPage('已完成驗證', '您的帳號已完成驗證，請開啟 App 登入。', true));
    }

    if (new Date() > new Date(user.verification_token_expires)) {
      return res.status(400).send(htmlPage('連結已過期', '驗證連結已超過 24 小時。', false));
    }

    await pool.query(
      `UPDATE users
       SET email_verified = TRUE,
           verification_token = NULL,
           verification_token_expires = NULL
       WHERE id = $1`,
      [user.id]
    );

    res.send(htmlPage('驗證成功！', user.username + '，您的電子郵件已驗證完成。請開啟 App 並登入。', true));
  } catch (err) {
    console.error(err);
    res.status(500).send(htmlPage('伺服器錯誤', '請稍後再試。', false));
  }
});

// POST /api/auth/resend-verification
router.post('/resend-verification', async (req, res) => {
  return res.json({ success: true, message: 'Email 驗證功能暫時停用，請直接登入。' });
});

function htmlPage(title, body, success) {
  const color = success ? '#4CAF50' : '#f44336';
  const icon = success ? '✅' : '❌';
  return `<!DOCTYPE html>
<html lang="zh-TW">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${title}</title>
  <style>
    body { font-family: Arial, sans-serif; display: flex; align-items: center;
           justify-content: center; min-height: 100vh; margin: 0; background: #f5f5f5; }
    .card { background: white; border-radius: 12px; padding: 40px 32px;
            max-width: 400px; text-align: center; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
    h1 { color: ${color}; font-size: 22px; }
    p { color: #555; line-height: 1.6; white-space: pre-line; }
    .icon { font-size: 48px; margin-bottom: 16px; }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">${icon}</div>
    <h1>${title}</h1>
    <p>${body}</p>
  </div>
</body>
</html>`;
}

module.exports = router;
