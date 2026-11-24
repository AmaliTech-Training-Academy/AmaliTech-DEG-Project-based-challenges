import { el } from '../utils/dom.js';
import { icon } from '../utils/icons.js';

/**
 * Root of the file explorer tree.
 * It only renders the top level — each TreeNode renders its own children.
 *
 * Event delegation: instead of one click listener per row (hundreds in a
 * big vault), there is ONE listener on the tree. It finds which row was
 * clicked with closest().
 */
export function Tree({ nodes, state, onToggle }) {
  function handleClick(event) {
    const row = event.target.closest('.tree__row');
    if (!row) return;

    const item = row.parentElement; // the <li role="treeitem">
    if (item.dataset.type === 'folder') {
      onToggle(item.dataset.id);
    }
  }

  return el(
    'ul',
    { className: 'tree', role: 'tree', 'aria-label': 'Vault folders and files', onClick: handleClick },
    nodes.map((node) => TreeNode({ node, depth: 0, state })),
  );
}

/**
 * RECURSIVE component: renders one node, and if it is an open folder,
 * calls itself for every child with depth + 1.
 *
 * Base case: a file, or a closed folder -> no recursive call.
 * Because there is no hard-coded level, the same code handles
 * 2 levels or 20 levels.
 */
function TreeNode({ node, depth, state }) {
  const isFolder = node.type === 'folder';
  const children = node.children ?? [];
  const isExpanded = isFolder && state.expanded.has(node.id);

  const row = el(
    'div',
    { className: 'tree__row', style: `--depth: ${depth}`, title: node.name },
    isFolder
      ? icon('chevron', { size: 14, className: 'tree__chevron' })
      : el('span', { className: 'tree__spacer' }),
    icon(isFolder ? 'folder' : 'file', { className: 'tree__icon' }),
    el('span', { className: 'tree__name' }, node.name),
  );

  let group = null;
  if (isExpanded) {
    group = el(
      'ul',
      { className: 'tree__group', role: 'group' },
      children.length > 0
        ? children.map((child) => TreeNode({ node: child, depth: depth + 1, state }))
        : el('li', { className: 'tree__empty', role: 'none', style: `--depth: ${depth + 1}` }, 'Empty folder'),
    );
  }

  return el(
    'li',
    {
      className: 'tree__item',
      role: 'treeitem',
      'aria-level': depth + 1,
      'aria-expanded': isFolder ? String(isExpanded) : null,
      dataset: { id: node.id, type: node.type },
    },
    row,
    group,
  );
}
