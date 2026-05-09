const express = require('express');
const cors = require('cors');
const { initDb } = require('./database');

const app = express();
app.use(cors());
app.use(express.json());

app.use('/api/auth', require('./routes/auth'));
app.use('/api/users', require('./routes/users'));
app.use('/api/utensils', require('./routes/utensils'));
app.use('/api/borrow', require('./routes/borrow'));

app.get('/', (req, res) => res.json({ message: '環保餐具借還系統 API 運行中' }));

const PORT = process.env.PORT || 3000;

initDb()
  .then(() => {
    app.listen(PORT, () => console.log(`伺服器已啟動 port ${PORT}`));
  })
  .catch(err => {
    console.error('資料庫初始化失敗:', err);
    process.exit(1);
  });
