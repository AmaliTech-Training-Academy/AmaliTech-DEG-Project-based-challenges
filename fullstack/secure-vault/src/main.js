import './styles/tokens.css';
import './styles/base.css';
import './styles/layout.css';
import data from '../data.json';
import { createLayout } from './components/Layout.js';

const layout = createLayout();
document.getElementById('app').replaceWith(layout.root);

// Temporary placeholders — replaced by real components in the next steps
layout.sidebar.textContent = `${data.length} top-level items`;
layout.main.textContent = 'Main content';
layout.properties.textContent = 'Properties';
