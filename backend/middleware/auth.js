const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'eco_hackathon_secret_2026';

// 一般 JWT 驗證 middleware
const auth = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.status(401).json({ success: false, message: 'Please login first' });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    res.status(403).json({ success: false, message: 'Token invalid or expired' });
  }
};

// 店員/管理者專用 middleware（需先通過 auth）
const staffOnly = (req, res, next) => {
  if (!req.user) return res.status(401).json({ success: false, message: 'Please login first' });
  if (req.user.role !== 'staff' && req.user.role !== 'admin') {
    return res.status(403).json({ success: false, message: 'Staff only' });
  }
  next();
};

module.exports = auth;
module.exports.staffOnly = staffOnly;
module.exports.JWT_SECRET = JWT_SECRET;
