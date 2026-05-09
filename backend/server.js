const express = require('express');
const cors = require('cors');
const { initDb } = require('./database');

const app = express();
app.use(cors());
app.use(express.json());

app.use('/api/auth',     require('./routes/auth'));
app.use('/api/users',    require('./routes/users'));
app.use('/api/utensils', require('./routes/utensils'));
app.use('/api/borrow',   require('./routes/borrow'));
app.use('/api/lottery',  require('./routes/lottery'));

app.get('/', (req, res) => res.json({ message: 'Eco Utensil API OK' }));

const PORT = process.env.PORT || 3000;
initDb()
  .then(() => app.listen(PORT, () => console.log('Server started on port ' + PORT)))
  .catch(err => { console.error('DB init failed:', err); process.exit(1); });
