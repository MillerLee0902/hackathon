const express = require('express');
const { pool } = require('../database');
const auth = require('../middleware/auth');
const { staffOnly } = require('../middleware/auth');

const router = express.Router();

// 所有 staff 路由都需先通過 JWT 驗證 + 店員權限
router.use(auth);
router.use(staffOnly);

// POST /api/staff/return
// 店員代替用戶歸還餐具：先掃用戶 QR（USER-{id}），再掃餐具 QR
router.post('/return', async (req, res) => {
  const { userQrCode, utensilQrCode } = req.body;
  if (!userQrCode || !utensilQrCode) {
    return res.status(400).json({ success: false, message: '請提供用戶 QR 與餐具 QR' });
  }

  const match = userQrCode.match(/^USER-(\d+)$/);
  if (!match) {
    return res.status(400).json({ success: false, message: '用戶 QR 格式錯誤，應為 USER-{id}' });
  }
  const borrowerUserId = parseInt(match[1]);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 確認餐具存在
    const ur = await client.query(
      'SELECT * FROM utensils WHERE qr_code = $1 FOR UPDATE',
      [utensilQrCode]
    );
    if (ur.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到此餐具' });
    }
    const utensil = ur.rows[0];

    if (utensil.status === 'available') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此餐具目前未被借出' });
    }
    if (utensil.current_borrower_id !== borrowerUserId) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此餐具並非由該用戶借出' });
    }

    // 歸還餐具
    await client.query(
      "UPDATE utensils SET status = 'available', current_borrower_id = NULL, borrowed_at = NULL WHERE id = $1",
      [utensil.id]
    );

    // 退押金 + 加點數給用戶
    const updatedUser = await client.query(
      'UPDATE users SET wallet_balance = wallet_balance + $1, points = points + 1 WHERE id = $2 RETURNING username, wallet_balance, points',
      [utensil.deposit_amount, borrowerUserId]
    );

    // 記錄 transaction，note 標記由店員操作
    await client.query(
      "INSERT INTO transactions (user_id, utensil_id, action, points_earned, deposit_change, note) VALUES ($1, $2, 'return', 1, $3, $4)",
      [borrowerUserId, utensil.id, parseFloat(utensil.deposit_amount), '[店員回收] ' + utensil.type]
    );

    await client.query('COMMIT');

    const b = updatedUser.rows[0];
    res.json({
      success: true,
      message: b.username + ' 的 ' + utensil.type + ' 已回收',
      borrowerName: b.username,
      utensilType: utensil.type,
      pointsEarned: 1,
      depositReturned: parseFloat(utensil.deposit_amount),
      newPoints: b.points,
      newWalletBalance: parseFloat(b.wallet_balance),
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    client.release();
  }
});

// GET /api/staff/transactions
// 查詢所有借還記錄（店員盤點用）
router.get('/transactions', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT
        t.id,
        t.action,
        t.points_earned,
        t.deposit_change,
        t.note,
        t.created_at,
        u.username,
        u.email,
        ut.qr_code AS utensil_qr,
        ut.type AS utensil_type,
        ut.status AS utensil_status
      FROM transactions t
      JOIN users u ON t.user_id = u.id
      JOIN utensils ut ON t.utensil_id = ut.id
      ORDER BY t.created_at DESC
      LIMIT 200
    `);

    res.json({
      success: true,
      transactions: result.rows.map(row => ({
        id: row.id,
        action: row.action,
        pointsEarned: row.points_earned,
        depositChange: parseFloat(row.deposit_change),
        note: row.note,
        createdAt: row.created_at,
        username: row.username,
        email: row.email,
        utensilQr: row.utensil_qr,
        utensilType: row.utensil_type,
        utensilStatus: row.utensil_status,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// GET /api/staff/utensils
// 查詢所有餐具現況（含借出者資訊）
router.get('/utensils', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT
        ut.id,
        ut.qr_code,
        ut.type,
        ut.status,
        ut.borrowed_at,
        u.username AS borrower_name,
        u.email AS borrower_email
      FROM utensils ut
      LEFT JOIN users u ON ut.current_borrower_id = u.id
      ORDER BY ut.qr_code
    `);

    res.json({
      success: true,
      utensils: result.rows.map(row => ({
        id: row.id,
        qrCode: row.qr_code,
        type: row.type,
        status: row.status,
        borrowedAt: row.borrowed_at,
        borrowerName: row.borrower_name || null,
        borrowerEmail: row.borrower_email || null,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

module.exports = router;
