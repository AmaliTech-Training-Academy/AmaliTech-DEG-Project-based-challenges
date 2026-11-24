import { el } from '../utils/dom.js';
import { icon } from '../utils/icons.js';

// Created once and never re-rendered, so typing is never interrupted.
export function SearchBox({ onSearch }) {
  const input = el('input', {
    id: 'vault-search',
    type: 'search',
    placeholder: 'Enter the document name',
    autocomplete: 'off',
    spellcheck: 'false',
  });

  const clearButton = el(
    'button',
    { type: 'button', className: 'searchbox__clear', 'aria-label': 'Clear search', hidden: true },
    icon('close', { size: 14 }),
  );

  function update(value) {
    input.value = value;
    clearButton.hidden = value === '';
    onSearch(value);
  }

  input.addEventListener('input', () => update(input.value));

  input.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') update('');
    // Arrow down jumps from the search into the results
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      document.querySelector('.tree [tabindex="0"]')?.focus();
    }
  });

  clearButton.addEventListener('click', () => {
    update('');
    input.focus();
  });

  return el(
    'div',
    { className: 'searchbox', role: 'search' },
    el('label', { className: 'sr-only', for: 'vault-search' }, 'Search the vault'),
    input,
    clearButton,
    icon('search', { size: 20, className: 'searchbox__icon' }),
  );
}
