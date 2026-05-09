const express = require('express');
const { pool } = require('../database');
const authMiddleware = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

router.post('/borrow', async (req, res) => {
  const { utensilQrCode } = req.body;
  if (!utensilQrCode) {
    return res.status(400).json({ success: false, message: '請提供餐具 QR Code' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const utensilResult = await client.query(
      'SELECT * FROM utensils WHERE qr_code = $1 FOR UPDATE',
      [utensilQrCode]
    );
    if (utensilResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到此餐具' });
    }

    const utensil = utensilResult.rows[0];
    if (utensil.status === 'borrowed') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此餐具目前已被借用中' });
    }

    const userResult = await client.query(
      'SELECT * FROM users WHERE id = $1 FOR UPDATE',
      [req.user.id]
    );
    const user = userResult.rows[0];
    if (parseFloat(user.wallet_balance) < parseFloat(utensil.deposit_amount)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: `錢包餘額不足，需要押金 $${utensil.deposit_amount}` });
    }

    await client.query(
      `UPDATE utensils SET status = 'borrowed', current_borrower_id = $1, borrowed_at = NOW() WHERE id = $2`,
      [req.user.id, utensil.id]
    );

    const updatedUser = await client.query(
      'UPDATE users SET wallet_balance = wallet_balance - $1 WHERE id = $2 RETURNING wallet_balance',
      [utensil.deposit_amount, req.user.id]
    );

    await client.query(
      `INSERT INTO transactions (user_id, utensil_id, action, points_earned, deposit_change, note)
       VALUES ($1, $2, 'borrow', 0, $3, $4)`,
      [req.user.id, utensil.id, -parseFloat(utensil.deposit_amount), `借用 ${utensil.type}`]
    );

    await client.query('COMMIT');

    res.json({
      success: true,
      message: `成功借用 ${utensil.type}！已扣除押金 $${utensil.deposit_amount}`,
      utensilType: utensil.type,
      walletBalance: parseFloat(updatedUser.rows[0].wallet_balance),
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  } finally {
    client.release();
  }
});

router.post('/return', async (req, res) => {
  const { userQrCode, utensilQrCode } = req.body;
  if (!userQrCode || !utensilQrCode) {
    return res.status(400).json({ success: false, message: '請提供用戶和餐具 QR Code' });
  }

  const userIdMatch = userQrCode.match(/^USER-(\d+)$/);
  if (!userIdMatch) {
    return res.status(400).json({ success: false, message: '用戶 QR Code 格式錯誤' });
  }
  const borrowerUserId = parseInt(userIdMatch[1]);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const utensilResult = await client.query(
      'SELECT * FROM utensils WHERE qr_code = $1 FOR UPDATE',
      [utensilQrCode]
    );
    if (utensilResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: '找不到此餐具' });
    }

    const utensil = utensilResult.rows[0];
    if (utensil.status === 'available') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此餐具尚未被借出' });
    }
    if (utensil.current_borrower_id !== borrowerUserId) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: '此餐具不是該用戶所借用的' });
    }

    const RETURN_POINTS = 1;

    await client.query(
      `UPDATE utensils SET status = 'available', current_borrower_id = NULL, borrowed_at = NULL WHERE id = $1`,
      [utensil.id]
    );

    const updatedBorrower = await client.query(
      `UPDATE users SET wallet_balance = wallet_balance + $1, points = points + $2
       WHERE id = $3
       RETURNING username, wallet_balance, points`,
      [utensil.deposit_amount, RETURN_POINTS, borrowerUserId]
    );

    await client.query(
      `INSERT INTO transactions (user_id, utensil_id, action, points_earned, deposit_change, note)
       VALUES ($1, $2, 'return', $3, $4, $5)`,
      [borrowerUserId, utensil.id, RETURN_POINTS, parseFloat(utensil.deposit_amount), `歸還 ${utensil.type}`]
    );

    await client.query('COMMIT');

    const borrower = updatedBorrower.rows[0];
    res.json({
      success: true,
      message: `${borrower.username} 成功歸還 ${utensil.type}！`,
      borrowerName: borrower.username,
      pointsEarned: RETURN_POINTS,
      depositReturned: parseFloat(utensil.deposit_amount),
      newPoints: borrower.points,
      newWalletBalance: parseFloat(borrower.wallet_balance),
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  } finally {
    client.release();
  }
});

module.exports = router;
