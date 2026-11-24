import './styles/tokens.css';
import './styles/base.css';
import data from '../data.json';

const app = document.getElementById('app');
app.textContent = `SecureVault loaded ${data.length} top-level items.`;
