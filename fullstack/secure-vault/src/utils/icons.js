/**
 * Inline SVG icon set (16x16 grid). Icons use `currentColor`,
 * so their colour is controlled from CSS with `color: ...`.
 */
const paths = {
  shield:
    '<path d="M8 1.5 13.5 3.5v4.2c0 3.2-2.3 5.6-5.5 6.8-3.2-1.2-5.5-3.6-5.5-6.8V3.5z"/>' +
    '<rect x="5.6" y="7" width="4.8" height="3.6" rx="0.6"/>' +
    '<path d="M6.6 7V5.9a1.4 1.4 0 0 1 2.8 0V7"/>',
  search: '<circle cx="7" cy="7" r="4.8"/><path d="M10.6 10.6 14 14"/>',
  clock: '<circle cx="8" cy="8" r="6"/><path d="M8 4.5V8l2.5 1.5"/>',
  chevron: '<path d="M6 4l4 4-4 4"/>',
  folder:
    '<path d="M2 4.5A1.5 1.5 0 0 1 3.5 3h3l1.5 2h4.5A1.5 1.5 0 0 1 14 6.5v5a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 2 11.5z"/>',
  file: '<path d="M4 1.75h5l3.5 3.5v9H4z"/><path d="M9 1.75v3.5h3.5M6 8.5h4.5M6 11h4.5"/>',
  expandAll: '<path d="M5 6l3-3 3 3M5 10l3 3 3-3"/>',
  collapseAll: '<path d="M5 3l3 3 3-3M5 13l3-3 3 3"/>',
  close: '<path d="M4.5 4.5l7 7M11.5 4.5l-7 7"/>',
};

export function icon(name, { size = 16, className = '' } = {}) {
  const template = document.createElement('template');
  template.innerHTML =
    `<svg class="icon ${className}" width="${size}" height="${size}" viewBox="0 0 16 16" ` +
    'fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" ' +
    `stroke-linejoin="round" aria-hidden="true">${paths[name]}</svg>`;
  return template.content.firstElementChild;
}
