import { el } from '../utils/dom.js';
import { icon } from '../utils/icons.js';
import { formatSize } from '../utils/files.js';
import { FileBadge } from './FileBadge.js';

const FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'folder', label: 'Folders' },
  { key: 'file', label: 'Files' },
];

function Breadcrumbs({ ancestors, folder, onNavigate }) {
  const trail = [{ id: null, name: 'Workspace' }, ...ancestors, ...(folder ? [folder] : [])];

  return el(
    'nav',
    { className: 'breadcrumbs', 'aria-label': 'Breadcrumb' },
    trail.map((crumb, i) => {
      const isLast = i === trail.length - 1;
      return [
        el('span', { className: 'breadcrumbs__sep', 'aria-hidden': 'true' }, '/'),
        el(
          'button',
          {
            type: 'button',
            className: `crumb${isLast ? ' crumb--current' : ''}`,
            dataset: { key: `crumb-${crumb.id}` },
            'aria-current': isLast ? 'page' : null,
            onClick: () => onNavigate(crumb.id),
          },
          crumb.name,
        ),
      ];
    }),
  );
}

function StatCard(label, value) {
  return el(
    'div',
    { className: 'stat' },
    el('span', { className: 'stat__label' }, label),
    el('span', { className: 'stat__value' }, value),
  );
}

function ListItem({ node, isSelected, onOpenFolder, onSelectFile }) {
  const isFolder = node.type === 'folder';
  const count = node.children?.length ?? 0;

  const meta = isFolder
    ? el('span', { className: 'item__meta' }, `${count} ${count === 1 ? 'item' : 'items'}`)
    : [FileBadge(node.name, { boxed: true }), el('span', { className: 'item__meta item__size' }, formatSize(node.size))];

  return el(
    'li',
    {},
    el(
      'button',
      {
        type: 'button',
        className: `item${isSelected ? ' item--selected' : ''}`,
        dataset: { key: `item-${node.id}` },
        onClick: () => (isFolder ? onOpenFolder(node.id) : onSelectFile(node.id)),
      },
      icon(isFolder ? 'folder' : 'file', { size: 18, className: 'item__icon' }),
      el('span', { className: 'item__name' }, node.name),
      el('span', { className: 'item__end' }, meta),
    ),
  );
}

function KeyboardHints() {
  const hint = (keys, text) => el('span', { className: 'hints__item' }, keys.map((k) => el('kbd', {}, k)), text);
  return el(
    'div',
    { className: 'hints', 'aria-label': 'Keyboard shortcuts' },
    hint(['↑', '↓'], 'move'),
    hint(['→'], 'expand'),
    hint(['←'], 'collapse'),
    hint(['Enter'], 'open / select'),
  );
}

// Main area: the content of the current folder (or the top level).
export function FolderView({ folder, ancestors, items, state, actions }) {
  const folders = items.filter((item) => item.type === 'folder');
  const files = items.filter((item) => item.type === 'file');
  const counts = { all: items.length, folder: folders.length, file: files.length };

  const filter = state.listFilter;
  const shown = filter === 'all' ? items : items.filter((item) => item.type === filter);

  return [
    el(
      'header',
      { className: 'content__header' },
      Breadcrumbs({ ancestors, folder, onNavigate: actions.openFolder }),
      el('h1', { className: 'content__title' }, folder ? folder.name : 'Workspace'),
    ),

    el(
      'div',
      { className: 'stats' },
      StatCard('Documents', counts.all),
      StatCard('Folders', counts.folder),
      StatCard('Files', counts.file),
    ),

    el(
      'section',
      { className: 'documents', 'aria-label': 'Folder contents' },
      el(
        'div',
        { className: 'documents__header' },
        el('h2', { className: 'documents__title' }, 'Documents'),
        el(
          'div',
          { className: 'pills', role: 'group', 'aria-label': 'Filter by type' },
          FILTERS.map(({ key, label }) =>
            el(
              'button',
              {
                type: 'button',
                className: `pill${filter === key ? ' pill--active' : ''}`,
                dataset: { key: `pill-${key}` },
                'aria-pressed': String(filter === key),
                onClick: () => actions.setListFilter(key),
              },
              `${label} · ${counts[key]}`,
            ),
          ),
        ),
      ),
      shown.length > 0
        ? el(
            'ul',
            { className: 'documents__list' },
            shown.map((node) =>
              ListItem({
                node,
                isSelected: node.id === state.selectedId,
                onOpenFolder: actions.openFolder,
                onSelectFile: actions.selectFile,
              }),
            ),
          )
        : el('p', { className: 'documents__empty' }, items.length === 0 ? 'This folder is empty.' : 'Nothing matches this filter.'),
    ),

    KeyboardHints(),
  ];
}
