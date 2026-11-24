import { el } from '../utils/dom.js';
import { getFileInfo } from '../utils/files.js';

// Small coloured label like "PDF" or "DOCX".
export function FileBadge(name, { boxed = false } = {}) {
  const { type, kind } = getFileInfo(name);
  return el('span', { className: `badge badge--${kind}${boxed ? ' badge--boxed' : ''}` }, type);
}
