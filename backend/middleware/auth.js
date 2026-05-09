const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'eco_hackathon_secret_2026';

module.exports = (req, res, next) => {
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

module.exports.JWT_SECRET = process.env.JWT_SECRET || 'eco_hackathon_secret_2026';
