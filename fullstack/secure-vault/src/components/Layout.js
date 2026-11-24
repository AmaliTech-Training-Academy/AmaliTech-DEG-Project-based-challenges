import { el } from '../utils/dom.js';
import { icon } from '../utils/icons.js';

/** The SecureVault logo shown at the top of the sidebar. */
function Brand() {
  return el(
    'div',
    { className: 'brand' },
    icon('shield', { size: 26, className: 'brand__icon' }),
    el('span', { className: 'brand__name' }, 'Secure', el('strong', {}, 'Vault')),
  );
}

/**
 * Builds the three-column page shell and returns each region,
 * so other components can render into them.
 */
export function createLayout() {
  const sidebarBody = el('div', { className: 'sidebar__body' });
  const sidebar = el('nav', { className: 'sidebar', 'aria-label': 'Vault navigation' }, Brand(), sidebarBody);

  const main = el('main', { className: 'content' });

  const properties = el('aside', { className: 'properties', 'aria-label': 'File properties' });

  const root = el('div', { className: 'app' }, sidebar, main, properties);

  return { root, sidebar: sidebarBody, main, properties };
}
