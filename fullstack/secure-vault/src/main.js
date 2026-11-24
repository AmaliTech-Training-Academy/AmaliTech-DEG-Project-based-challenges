import data from '../data.json';

const app = document.getElementById('app');
app.textContent = `SecureVault loaded ${data.length} top-level items.`;
