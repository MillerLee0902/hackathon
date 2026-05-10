const express = require('express');
const { pool } = require('../database');
const auth = require('../middleware/auth');
const { staffOnly } = require('../middleware/auth');
const { generateSequentialTickets } = require('../utils/lottery');

const router = express.Router();

// 所有 staff 路由都需先通過 JWT 驗證 + 店員權限
router.use(auth);
router.use(staffOnly);

// POST /api/staff/borrow
// 店員代替用戶借用餐具：先掃餐具 QR → 再掃用戶 QR（USER-{id}）
router.post('/borrow', async (req, res) => {
  const { utensilQrCode, userQrCode } = req.body;
  if (!utensilQrCode || !userQrCode) {
    return res.status(400).json({ success: false, message: '請提供餐具 QR 與用戶 QR' });
  }

  const match = userQrCode.match(/^USER-(\d+)$/);
  if (!match) {
    return res.status(400).json({ success: false, message: '用戶 QR 格式錯誤，應為 USER-{id}' });
  }
  const borrowerUserId = parseInt(match[1]);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 確認餐具存在且可借
    const ur = await client.query('SELECT * FROM utensils WHERE qr_code = $1 FOR UPDATE', [utensilQrCode]);
    if (ur.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到此餐具' });
    }
    const utensil = ur.rows[0];
    if (utensil.status === 'borrowed') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此餐具已被借走' });
    }

    // 確認用戶存在且餘額足夠
    const userR = await client.query('SELECT * FROM users WHERE id = $1 FOR UPDATE', [borrowerUserId]);
    if (userR.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到此用戶，請確認用戶 QR Code' });
    }
    const user = userR.rows[0];
    if (parseFloat(user.wallet_balance) < parseFloat(utensil.deposit_amount)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '用戶錢包不足，無法扣押金 $' + utensil.deposit_amount });
    }

    // 標記餐具為借出中
    await client.query(
      "UPDATE utensils SET status = 'borrowed', current_borrower_id = $1, borrowed_at = NOW() WHERE id = $2",
      [borrowerUserId, utensil.id]
    );

    // 扣押金
    const updated = await client.query(
      'UPDATE users SET wallet_balance = wallet_balance - $1 WHERE id = $2 RETURNING wallet_balance',
      [utensil.deposit_amount, borrowerUserId]
    );

    // 記錄 transaction
    await client.query(
      "INSERT INTO transactions (user_id, utensil_id, action, points_earned, deposit_change, note) VALUES ($1, $2, 'borrow', 0, $3, $4)",
      [borrowerUserId, utensil.id, -parseFloat(utensil.deposit_amount), '[店員借出] ' + utensil.type]
    );

    await client.query('COMMIT');

    res.json({
      success: true,
      message: user.username + ' 借用 ' + utensil.type + '，押金 $' + utensil.deposit_amount + ' 已扣除',
      borrowerName: user.username,
      utensilType: utensil.type,
      depositCharged: parseFloat(utensil.deposit_amount),
      newWalletBalance: parseFloat(updated.rows[0].wallet_balance),
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    client.release();
  }
});

// POST /api/staff/return
// 店員代替用戶歸還餐具：先掃餐具 QR → 再掃用戶 QR（USER-{id}）
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
      "UPDATE utensils SET status = 'available', current_borrower_id = NULL, borrowed_at = NULL, return_quantity = return_quantity + 1 WHERE id = $1",
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

    // 歸還獎勵：發一張流水抽獎號碼給歸還者
    const tickets = await generateSequentialTickets(client, 1, utensil.id, borrowerUserId);

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
      lotteryTicket: tickets[0],
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

// POST /api/staff/redeem
// 店員掃描兌換 QR Code，扣除用戶點數
router.post('/redeem', async (req, res) => {
  const { qrData } = req.body;
  if (!qrData) return res.status(400).json({ success: false, message: '請提供 qrData' });

  // QR 格式：REDEEM-{userId}-{points}-{token}
  const match = qrData.match(/^REDEEM-(\d+)-(\d+)-([a-f0-9]+)$/);
  if (!match) return res.status(400).json({ success: false, message: 'QR Code 格式錯誤' });

  const userId = parseInt(match[1]);
  const points = parseInt(match[2]);
  const token = match[3];

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 驗證 token
    const tokenRes = await client.query(
      `SELECT * FROM redemption_tokens WHERE token = $1 FOR UPDATE`,
      [token]
    );
    if (tokenRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到此兌換 QR，可能已過期或不存在' });
    }
    const rec = tokenRes.rows[0];
    if (rec.status === 'used') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此 QR Code 已使用過' });
    }
    // 10 分鐘過期
    const ageMs = Date.now() - new Date(rec.created_at).getTime();
    if (ageMs > 10 * 60 * 1000) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: 'QR Code 已過期（10 分鐘限制）' });
    }
    // userId 與 points 與 DB 記錄一致性檢查
    if (rec.user_id !== userId || rec.points !== points) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: 'QR Code 資料不一致' });
    }

    // 確認用戶點數足夠
    const userRes = await client.query(
      'SELECT id, username, points FROM users WHERE id = $1 FOR UPDATE',
      [userId]
    );
    if (userRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到用戶' });
    }
    const user = userRes.rows[0];
    if (user.points < points) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: `用戶點數不足（目前 ${user.points} 點，欲兌換 ${points} 點）` });
    }

    // 扣點
    const updated = await client.query(
      'UPDATE users SET points = points - $1 WHERE id = $2 RETURNING points',
      [points, userId]
    );
    // 標記 token 為已使用
    await client.query(
      `UPDATE redemption_tokens SET status = 'used', used_at = NOW() WHERE token = $1`,
      [token]
    );

    await client.query('COMMIT');
    res.json({
      success: true,
      message: `${user.username} 成功兌換 ${points} 點`,
      username: user.username,
      pointsDeducted: points,
      newPoints: updated.rows[0].points,
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
