/**
 * Tiny helper to create DOM elements without a framework.
 *
 *   el('button', { className: 'pill', onClick: handler }, 'Files')
 *
 * - props starting with "on" become event listeners
 * - className / dataset are handled specially
 * - everything else becomes an HTML attribute
 * - children can be strings, nodes, arrays, or null (skipped)
 */
export function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);

  for (const [key, value] of Object.entries(props)) {
    if (value === null || value === undefined || value === false) continue;

    if (key === 'className') {
      node.className = value;
    } else if (key === 'dataset') {
      Object.assign(node.dataset, value);
    } else if (key.startsWith('on')) {
      node.addEventListener(key.slice(2).toLowerCase(), value);
    } else {
      node.setAttribute(key, value === true ? '' : value);
    }
  }

  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }

  return node;
}
