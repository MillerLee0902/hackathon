const express = require('express');
const { pool } = require('../database');
const authMiddleware = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

router.get('/me', async (req, res) => {
  try {
    const userResult = await pool.query(
      'SELECT id, username, email, points, wallet_balance FROM users WHERE id = $1',
      [req.user.id]
    );
    if (userResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: '找不到用戶' });
    }
    const user = userResult.rows[0];

    const borrowResult = await pool.query(
      `SELECT COUNT(*) FROM utensils WHERE status = 'borrowed' AND current_borrower_id = $1`,
      [req.user.id]
    );

    res.json({
      id: user.id,
      username: user.username,
      email: user.email,
      points: user.points,
      walletBalance: parseFloat(user.wallet_balance),
      borrowedCount: parseInt(borrowResult.rows[0].count),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  }
});

router.get('/qrcode', (req, res) => {
  res.json({ qrData: `USER-${req.user.id}` });
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
      utensilType: t.utensil_type || '未知餐具',
      pointsEarned: t.points_earned,
      depositChange: parseFloat(t.deposit_change),
      note: t.note,
      createdAt: t.created_at,
    })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  }
});

module.exports = router;
