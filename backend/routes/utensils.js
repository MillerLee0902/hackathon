const express = require('express');
const { pool } = require('../database');
const authMiddleware = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

async function generateUniqueLotteryNumber(client) {
  let ticket;
  let conflict = true;
  while (conflict) {
    ticket = String(Math.floor(100000 + Math.random() * 900000));
    const check = await client.query(
      'SELECT 1 FROM lottery_numbers WHERE ticket_number = $1',
      [ticket]
    );
    conflict = check.rows.length > 0;
  }
  return ticket;
}

// GET /api/utensils
router.get('/', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT u.id, u.qr_code, u.type, u.status, u.deposit_amount,
              u.add_quantity,
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
      borrowerName: u.borrower_name || null,
    })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// GET /api/utensils/:qrCode
router.get('/:qrCode', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM utensils WHERE qr_code = $1',
      [req.params.qrCode]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Utensil not found' });
    }
    const u = result.rows[0];
    res.json({
      id: u.id,
      qrCode: u.qr_code,
      type: u.type,
      status: u.status,
      depositAmount: parseFloat(u.deposit_amount),
      addQuantity: u.add_quantity,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// POST /api/utensils/:id/restock
router.post('/:id/restock', async (req, res) => {
  const utensilId = parseInt(req.params.id);
  const addQuantity = parseInt(req.body.addQuantity);

  if (!Number.isInteger(addQuantity) || addQuantity <= 0) {
    return res.status(400).json({ success: false, message: 'addQuantity must be positive integer' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const utensilResult = await client.query(
      'SELECT * FROM utensils WHERE id = $1 FOR UPDATE',
      [utensilId]
    );
    if (utensilResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, message: 'Utensil not found' });
    }

    await client.query(
      'UPDATE utensils SET add_quantity = $1 WHERE id = $2',
      [addQuantity, utensilId]
    );

    const tickets = [];
    for (let i = 0; i < addQuantity; i++) {
      const ticket = await generateUniqueLotteryNumber(client);
      await client.query(
        'INSERT INTO lottery_numbers (ticket_number, utensil_id) VALUES ($1, $2)',
        [ticket, utensilId]
      );
      tickets.push(ticket);
    }

    await client.query(
      'UPDATE utensils SET add_quantity = 0 WHERE id = $1',
      [utensilId]
    );

    await client.query('COMMIT');

    res.json({
      success: true,
      message: 'Restock done, tickets generated: ' + addQuantity,
      generatedTickets: tickets,
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
