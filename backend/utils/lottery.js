/**
 * lottery.js — 流水號抽獎號碼產生器
 *
 * generateSequentialTickets(client, quantity, utensilId, userId?)
 *   - client      : pg PoolClient（呼叫方負責 BEGIN/COMMIT/ROLLBACK）
 *   - quantity    : 要產生的票數
 *   - utensilId   : 對應的餐具 ID
 *   - userId      : （可選）獲得票的使用者 ID
 *   - 回傳        : 產生的號碼陣列（字串），例如 ['000001', '000002', ...]
 *
 * 票號格式：6 位流水號，從資料表 lottery_numbers 目前最大號碼 +1 開始。
 */

async function generateSequentialTickets(client, quantity, utensilId, userId = null) {
  // 取得目前最大流水號（數字部分），使用 database.js 已建立的 lottery_numbers 資料表
  const maxResult = await client.query(
    `SELECT COALESCE(MAX(CAST(ticket_number AS BIGINT)), 0) AS max_num FROM lottery_numbers`
  );
  let nextNum = parseInt(maxResult.rows[0].max_num) + 1;

  const tickets = [];
  for (let i = 0; i < quantity; i++) {
    const ticketNumber = String(nextNum).padStart(6, '0');
    await client.query(
      `INSERT INTO lottery_numbers (ticket_number, utensil_id, user_id) VALUES ($1, $2, $3)`,
      [ticketNumber, utensilId, userId]
    );
    tickets.push(ticketNumber);
    nextNum++;
  }

  return tickets;
}

module.exports = { generateSequentialTickets };
