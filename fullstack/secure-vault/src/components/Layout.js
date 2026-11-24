import { el } from '../utils/dom.js';
import { icon } from '../utils/icons.js';

function Brand() {
  return el(
    'div',
    { className: 'brand' },
    icon('shield', { size: 26, className: 'brand__icon' }),
    el('span', { className: 'brand__name' }, 'Secure', el('strong', {}, 'Vault')),
  );
}

// Page with 3 columns. Returns each column so we can render into it.
export function createLayout() {
  const sidebarBody = el('div', { className: 'sidebar__body' });
  const sidebar = el('nav', { className: 'sidebar', 'aria-label': 'Vault navigation' }, Brand(), sidebarBody);

  const main = el('main', { className: 'content' });

  // aria-live: screen readers read the new details when the selection changes
  const properties = el('aside', { className: 'properties', 'aria-label': 'File properties', 'aria-live': 'polite' });

  const root = el('div', { className: 'app' }, sidebar, main, properties);

  return { root, sidebar: sidebarBody, main, properties };
}
