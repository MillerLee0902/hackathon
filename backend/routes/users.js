const express = require('express');
const { pool } = require('../database');
const auth = require('../middleware/auth');
const crypto = require('crypto');

const router = express.Router();
router.use(auth);

router.get('/me', async (req, res) => {
  try {
    const userResult = await pool.query(
      'SELECT id, username, email, points, wallet_balance, role FROM users WHERE id = $1',
      [req.user.id]
    );
    if (userResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }
    const user = userResult.rows[0];
    const borrowResult = await pool.query(
      "SELECT COUNT(*) FROM utensils WHERE status = 'borrowed' AND current_borrower_id = $1",
      [req.user.id]
    );
    res.json({
      id: user.id,
      username: user.username,
      email: user.email,
      points: user.points,
      walletBalance: parseFloat(user.wallet_balance),
      borrowedCount: parseInt(borrowResult.rows[0].count),
      role: user.role || req.user.role || 'user',
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

router.get('/qrcode', (req, res) => {
  res.json({ qrData: 'USER-' + req.user.id });
});

router.get('/transactions', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT t.id, t.action, t.points_earned, t.deposit_change, t.note, t.created_at,
              u.type AS utensil_type
       FROM transactions t
       LEFT JOIN utensils u ON t.utensil_id = u.id
       WHERE t.user_id = $1
       ORDER BY t.created_at DESC
       LIMIT 50`,
      [req.user.id]
    );
    res.json(result.rows.map(t => ({
      id: t.id,
      action: t.action,
      utensilType: t.utensil_type || 'Unknown',
      pointsEarned: t.points_earned,
      depositChange: parseFloat(t.deposit_change),
      note: t.note,
      createdAt: t.created_at,
    })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// GET /api/users/lottery-numbers
// 查詢本人持有的抽獎號碼
router.get('/lottery-numbers', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT ln.id, ln.ticket_number, ln.created_at,
              u.type AS utensil_type, u.qr_code AS utensil_qr
       FROM lottery_numbers ln
       LEFT JOIN utensils u ON ln.utensil_id = u.id
       WHERE ln.user_id = $1
       ORDER BY ln.created_at DESC`,
      [req.user.id]
    );
    res.json({
      success: true,
      total: result.rows.length,
      tickets: result.rows.map(r => ({
        id: r.id,
        ticketNumber: r.ticket_number,
        utensilType: r.utensil_type || '未知',
        utensilQr: r.utensil_qr || '',
        createdAt: r.created_at,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// POST /api/users/redeem-qr
// 用戶輸入欲兌換點數，取得兌換 QR Code 資料
router.post('/redeem-qr', async (req, res) => {
  const points = parseInt(req.body.points);
  if (!Number.isInteger(points) || points <= 0) {
    return res.status(400).json({ success: false, message: '請輸入有效的兌換點數（正整數）' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 確認點數足夠
    const userRes = await client.query(
      'SELECT points FROM users WHERE id = $1 FOR UPDATE',
      [req.user.id]
    );
    if (userRes.rows[0].points < points) {
      await client.query('ROLLBACK');
      return res.status(400).json({
        success: false,
        message: `點數不足（目前 ${userRes.rows[0].points} 點）`,
      });
    }

    // 產生唯一 token（32 位 hex）
    const token = crypto.randomBytes(16).toString('hex');
    await client.query(
      `INSERT INTO redemption_tokens (user_id, points, token) VALUES ($1, $2, $3)`,
      [req.user.id, points, token]
    );

    await client.query('COMMIT');

    // QR 資料格式：REDEEM-{userId}-{points}-{token}
    const qrData = `REDEEM-${req.user.id}-${points}-${token}`;
    res.json({
      success: true,
      qrData,
      points,
      message: `請在 10 分鐘內出示此 QR Code 給店員掃描`,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    client.release();
  }
});

module.exports = router;
