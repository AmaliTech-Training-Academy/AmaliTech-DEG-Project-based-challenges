import { el, highlight } from '../utils/dom.js';
import { icon } from '../utils/icons.js';

export function Tree({ nodes, state, onToggle, onSelect, onKeyDown }) {
  // One click listener for the whole tree (event delegation)
  function handleClick(event) {
    const row = event.target.closest('.tree__row');
    if (!row) return;

    const item = row.parentElement;
    if (item.dataset.type === 'folder') onToggle(item.dataset.id);
    else onSelect(item.dataset.id);
  }

  // Only one item can be reached with Tab (roving tabindex)
  const activeId = state.focusedId ?? nodes[0]?.id;

  return el(
    'ul',
    {
      className: 'tree',
      role: 'tree',
      'aria-label': 'Vault folders and files',
      onClick: handleClick,
      onKeyDown,
    },
    nodes.map((node) => TreeNode({ node, depth: 0, state, activeId })),
  );
}

// Recursive: an open folder calls TreeNode again for each child.
// It stops at files and closed folders, so any depth works.
function TreeNode({ node, depth, state, activeId }) {
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
    el('span', { className: 'tree__name' }, highlight(node.name, state.query)),
  );

  let group = null;
  if (isExpanded) {
    group = el(
      'ul',
      { className: 'tree__group', role: 'group' },
      children.length > 0
        ? children.map((child) => TreeNode({ node: child, depth: depth + 1, state, activeId }))
        : el('li', { className: 'tree__empty', role: 'none', style: `--depth: ${depth + 1}` }, 'Empty folder'),
    );
  }

  return el(
    'li',
    {
      className: 'tree__item',
      role: 'treeitem',
      tabindex: node.id === activeId ? '0' : '-1',
      'aria-level': depth + 1,
      'aria-expanded': isFolder ? String(isExpanded) : null,
      'aria-selected': isFolder ? null : String(state.selectedId === node.id),
      dataset: { id: node.id, type: node.type },
    },
    row,
    group,
  );
}
