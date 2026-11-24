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

// Replaces the content of a container but keeps focus on the same button.
// Buttons are matched by their data-key attribute.
export function renderInto(container, children) {
  const active = document.activeElement;
  const key = container.contains(active) ? active.dataset.key : null;

  container.replaceChildren(...children);

  if (key) container.querySelector(`[data-key="${CSS.escape(key)}"]`)?.focus();
}
