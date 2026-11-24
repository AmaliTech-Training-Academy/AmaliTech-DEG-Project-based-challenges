// Creates an element. Example: el('button', { className: 'pill', onClick: fn }, 'Files')
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

// Wraps the part of the text that matches the search in <mark>.
export function highlight(text, query) {
  const start = query ? text.toLowerCase().indexOf(query.trim().toLowerCase()) : -1;
  if (!query.trim() || start === -1) return [text];

  const end = start + query.trim().length;
  return [text.slice(0, start), el('mark', {}, text.slice(start, end)), text.slice(end)];
}

// Replaces the content of a container but keeps focus on the same button.
// Buttons are matched by their data-key attribute.
export function renderInto(container, children) {
  const active = document.activeElement;
  const key = container.contains(active) ? active.dataset.key : null;

  container.replaceChildren(...children);

  if (key) container.querySelector(`[data-key="${CSS.escape(key)}"]`)?.focus();
}
