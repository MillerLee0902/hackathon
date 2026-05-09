const express = require('express');
const { pool } = require('../database');
const auth = require('../middleware/auth');

const router = express.Router();
router.use(auth);

router.get('/', async (req, res) => {
  try {
    const { utensilId } = req.query;
    let query = `
      SELECT ln.id, ln.ticket_number, ln.utensil_id, ln.created_at,
             u.type AS utensil_type, u.qr_code AS utensil_qr_code
      FROM lottery_numbers ln
      LEFT JOIN utensils u ON ln.utensil_id = u.id
    `;
    const params = [];
    if (utensilId) { query += ' WHERE ln.utensil_id = $1'; params.push(parseInt(utensilId)); }
    query += ' ORDER BY ln.created_at DESC';
    const result = await pool.query(query, params);
    res.json({
      success: true, total: result.rows.length,
      tickets: result.rows.map(r => ({
        id: r.id, ticketNumber: r.ticket_number, utensilId: r.utensil_id,
        utensilType: r.utensil_type, utensilQrCode: r.utensil_qr_code, createdAt: r.created_at,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

module.exports = router;
