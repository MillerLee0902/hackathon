const express = require('express');
const { pool } = require('../database');
const auth = require('../middleware/auth');
const { generateSequentialTickets } = require('../utils/lottery');

const router = express.Router();
router.use(auth);

router.get('/', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT u.id, u.qr_code, u.type, u.status, u.deposit_amount, u.add_quantity, u.return_quantity,
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
      addQuantity: u.add_quantity,
      returnQuantity: u.return_quantity,
      borrowerName: u.borrower_name || null,
    })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

router.get('/:qrCode', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM utensils WHERE qr_code = $1', [req.params.qrCode]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Utensil not found' });
    }
    const u = result.rows[0];
    res.json({
      id: u.id, qrCode: u.qr_code, type: u.type,
      status: u.status, depositAmount: parseFloat(u.deposit_amount),
      addQuantity: u.add_quantity, returnQuantity: u.return_quantity,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// 依 add_quantity 生成流水抽獎號碼後歸零
router.post('/:id/restock', async (req, res) => {
  const utensilId = parseInt(req.params.id);
  const addQuantity = parseInt(req.body.addQuantity);
  if (!Number.isInteger(addQuantity) || addQuantity <= 0) {
    return res.status(400).json({ success: false, message: 'addQuantity must be positive integer' });
  }
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ur = await client.query('SELECT * FROM utensils WHERE id = $1 FOR UPDATE', [utensilId]);
    if (ur.rows.length === 0) { await client.query('ROLLBACK'); return res.status(404).json({ success: false, message: 'Utensil not found' }); }
    const tickets = await generateSequentialTickets(client, addQuantity, utensilId);
    await client.query('UPDATE utensils SET add_quantity = 0 WHERE id = $1', [utensilId]);
    await client.query('COMMIT');
    res.json({ success: true, message: 'Restock done', generatedTickets: tickets });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    client.release();
  }
});

// 依 return_quantity 生成流水抽獎號碼後歸零
router.post('/:id/process-returns', async (req, res) => {
  const utensilId = parseInt(req.params.id);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ur = await client.query('SELECT * FROM utensils WHERE id = $1 FOR UPDATE', [utensilId]);
    if (ur.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: 'Utensil not found' });
    }
    const returnQty = parseInt(ur.rows[0].return_quantity);
    if (returnQty <= 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, message: 'No pending return_quantity to process' });
    }
    const tickets = await generateSequentialTickets(client, returnQty, utensilId);
    await client.query('UPDATE utensils SET return_quantity = 0 WHERE id = $1', [utensilId]);
    await client.query('COMMIT');
    res.json({ success: true, message: 'Returns processed', generatedTickets: tickets, count: tickets.length });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  } finally {
    client.release();
  }
});

module.exports = router;
