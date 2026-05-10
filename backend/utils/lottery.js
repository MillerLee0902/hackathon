/**
 * lottery.js — 流水號抽獎號碼產生器
 *
 * generateSequentialTickets(client, quantity, utensilId)
 *   - client      : pg PoolClient（呼叫方負責 BEGIN/COMMIT/ROLLBACK）
 *   - quantity     : 要產生的票數
 *   - utensilId   : 對應的餐具 ID（記錄來源）
 *   - 回傳        : 產生的號碼陣列（字串），例如 ['000001', '000002', ...]
 *
 * 票號格式：6 位流水號，從資料表 lottery_tickets 目前最大號碼 +1 開始。
 * 若資料表不存在則自動建立。
 */

async function generateSequentialTickets(client, quantity, utensilId) {
  // 確保 lottery_tickets 資料表存在
  await client.query(`
    CREATE TABLE IF NOT EXISTS lottery_tickets (
      id            SERIAL PRIMARY KEY,
      ticket_number VARCHAR(20) NOT NULL UNIQUE,
      utensil_id    INTEGER,
      created_at    TIMESTAMP DEFAULT NOW()
    )
  `);

  // 取得目前最大流水號（數字部分）
  const maxResult = await client.query(
    `SELECT COALESCE(MAX(CAST(ticket_number AS BIGINT)), 0) AS max_num FROM lottery_tickets`
  );
  let nextNum = parseInt(maxResult.rows[0].max_num) + 1;

  const tickets = [];
  for (let i = 0; i < quantity; i++) {
    const ticketNumber = String(nextNum).padStart(6, '0');
    await client.query(
      `INSERT INTO lottery_tickets (ticket_number, utensil_id) VALUES ($1, $2)`,
      [ticketNumber, utensilId]
    );
    tickets.push(ticketNumber);
    nextNum++;
  }

  return tickets;
}

module.exports = { generateSequentialTickets };
