const express = require('express');
const { pool } = require('../database');
const authMiddleware = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT u.id, u.qr_code, u.type, u.status, u.deposit_amount,
              usr.username AS borrower_name
       FROM utensils u
       LEFT JOIN users usr ON u.current_borrower_id = usr.id
       ORDER BY u.id`
    );

    res.json(result.rows.map(u => ({
      id: u.id,
      qrCode: u.qr_code,
      type: u.type,
      status: u.status,
      depositAmount: parseFloat(u.deposit_amount),
      borrowerName: u.borrower_name || null,
    })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  }
});

router.get('/:qrCode', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM utensils WHERE qr_code = $1',
      [req.params.qrCode]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: '找不到此餐具 QR Code' });
    }
    const u = result.rows[0];

    res.json({
      id: u.id,
      qrCode: u.qr_code,
      type: u.type,
      status: u.status,
      depositAmount: parseFloat(u.deposit_amount),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '伺服器錯誤' });
  }
});

module.exports = router;
