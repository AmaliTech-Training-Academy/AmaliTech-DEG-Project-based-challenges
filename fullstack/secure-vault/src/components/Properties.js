import { el } from '../utils/dom.js';
import { icon } from '../utils/icons.js';
import { getFileInfo, formatSize } from '../utils/files.js';
import { FileBadge } from './FileBadge.js';

function Field(label, value, className = '') {
  return [el('dt', {}, `${label}:`), el('dd', { className }, value)];
}

// Right panel. Shows the selected file, or a message when nothing is selected.
export function Properties({ node, ancestors }) {
  const title = el('h2', { className: 'section-label' }, 'Properties');

  if (!node) {
    return [
      title,
      el(
        'div',
        { className: 'properties__empty' },
        icon('file', { size: 28 }),
        el('p', { className: 'properties__empty-title' }, 'No file selected'),
        el('p', {}, 'Select a file in the workspace to see its details.'),
      ),
    ];
  }

  const info = getFileInfo(node.name);
  const location = ['', ...ancestors.map((folder) => folder.name)].join(' / ').trim() || '/';

  return [
    title,
    el(
      'div',
      { className: 'properties__card' },
      el(
        'div',
        { className: 'properties__header' },
        el('div', { className: 'properties__thumb' }, FileBadge(node.name)),
        el('span', { className: 'properties__name' }, node.name),
      ),
      el(
        'dl',
        { className: 'properties__list' },
        Field('Name', info.baseName),
        Field('Type', info.type),
        Field('Size', formatSize(node.size), 'mono'),
        Field('Format', info.format),
        Field('Location', location, 'mono mono--small'),
      ),
    ),
  ];
}
