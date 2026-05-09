const express = require('express');
const { pool } = require('../database');
const auth = require('../middleware/auth');

const router = express.Router();
router.use(auth);

router.post('/borrow', async (req, res) => {
  const { utensilQrCode } = req.body;
  if (!utensilQrCode) return res.status(400).json({ success: false, message: 'Missing utensil QR code' });

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ur = await client.query('SELECT * FROM utensils WHERE qr_code = $1 FOR UPDATE', [utensilQrCode]);
    if (ur.rows.length === 0) { await client.query('ROLLBACK'); return res.status(404).json({ success: false, message: 'Utensil not found' }); }
    const utensil = ur.rows[0];
    if (utensil.status === 'borrowed') { await client.query('ROLLBACK'); return res.status(400).json({ success: false, message: 'Utensil already borrowed' }); }

    const userR = await client.query('SELECT * FROM users WHERE id = $1 FOR UPDATE', [req.user.id]);
    const user = userR.rows[0];
    if (parseFloat(user.wallet_balance) < parseFloat(utensil.deposit_amount)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: 'Insufficient wallet balance for deposit $' + utensil.deposit_amount });
    }

    await client.query("UPDATE utensils SET status = 'borrowed', current_borrower_id = $1, borrowed_at = NOW() WHERE id = $2", [req.user.id, utensil.id]);
    const updated = await client.query('UPDATE users SET wallet_balance = wallet_balance - $1 WHERE id = $2 RETURNING wallet_balance', [utensil.deposit_amount, req.user.id]);
    await client.query(
      "INSERT INTO transactions (user_id, utensil_id, action, points_earned, deposit_change, note) VALUES ($1, $2, 'borrow', 0, $3, $4)",
      [req.user.id, utensil.id, -parseFloat(utensil.deposit_amount), 'Borrow ' + utensil.type]
    );
    await client.query('COMMIT');
    res.json({ success: true, message: 'Borrowed ' + utensil.type + '. Deposit $' + utensil.deposit_amount + ' deducted.', utensilType: utensil.type, walletBalance: parseFloat(updated.rows[0].wallet_balance) });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally { client.release(); }
});

router.post('/return', async (req, res) => {
  const { userQrCode, utensilQrCode } = req.body;
  if (!userQrCode || !utensilQrCode) return res.status(400).json({ success: false, message: 'Missing QR codes' });

  const match = userQrCode.match(/^USER-(\d+)$/);
  if (!match) return res.status(400).json({ success: false, message: 'Invalid user QR code format' });
  const borrowerUserId = parseInt(match[1]);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ur = await client.query('SELECT * FROM utensils WHERE qr_code = $1 FOR UPDATE', [utensilQrCode]);
    if (ur.rows.length === 0) { await client.query('ROLLBACK'); return res.status(404).json({ success: false, message: 'Utensil not found' }); }
    const utensil = ur.rows[0];
    if (utensil.status === 'available') { await client.query('ROLLBACK'); return res.status(400).json({ success: false, message: 'Utensil not borrowed' }); }
    if (utensil.current_borrower_id !== borrowerUserId) { await client.query('ROLLBACK'); return res.status(400).json({ success: false, message: 'This utensil was not borrowed by this user' }); }

    await client.query("UPDATE utensils SET status = 'available', current_borrower_id = NULL, borrowed_at = NULL WHERE id = $1", [utensil.id]);
    const updatedUser = await client.query(
      'UPDATE users SET wallet_balance = wallet_balance + $1, points = points + 1 WHERE id = $2 RETURNING username, wallet_balance, points',
      [utensil.deposit_amount, borrowerUserId]
    );
    await client.query(
      "INSERT INTO transactions (user_id, utensil_id, action, points_earned, deposit_change, note) VALUES ($1, $2, 'return', 1, $3, $4)",
      [borrowerUserId, utensil.id, parseFloat(utensil.deposit_amount), 'Return ' + utensil.type]
    );
    await client.query('COMMIT');
    const b = updatedUser.rows[0];
    res.json({ success: true, message: b.username + ' returned ' + utensil.type, borrowerName: b.username, pointsEarned: 1, depositReturned: parseFloat(utensil.deposit_amount), newPoints: b.points, newWalletBalance: parseFloat(b.wallet_balance) });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally { client.release(); }
});

module.exports = router;
