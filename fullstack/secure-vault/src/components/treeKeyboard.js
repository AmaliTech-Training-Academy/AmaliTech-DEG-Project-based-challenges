import { getVisibleIds } from '../utils/tree.js';

// Keyboard control for the tree, following the WAI-ARIA tree pattern.
export function handleTreeKey(event, { nodes, index, state, actions }) {
  const visible = getVisibleIds(nodes, state.expanded);
  if (visible.length === 0) return;

  // If the focused item is hidden, start from the first item
  const position = Math.max(visible.indexOf(state.focusedId), 0);
  const id = visible[position];
  const { node, parentId } = index.get(id);
  const isFolder = node.type === 'folder';
  const isOpen = isFolder && state.expanded.has(id);

  switch (event.key) {
    case 'ArrowDown':
      actions.focusItem(visible[Math.min(position + 1, visible.length - 1)]);
      break;
    case 'ArrowUp':
      actions.focusItem(visible[Math.max(position - 1, 0)]);
      break;
    case 'ArrowRight':
      // Closed folder: open it. Open folder: go to its first visible child.
      if (isFolder && !isOpen) {
        actions.expandFolder(id);
      } else if (isOpen) {
        const next = visible[position + 1];
        if (next && index.get(next).parentId === id) actions.focusItem(next);
      }
      break;
    case 'ArrowLeft':
      // Open folder: close it. Otherwise: go up to the parent folder.
      if (isOpen) actions.collapseFolder(id);
      else if (parentId) actions.focusItem(parentId);
      break;
    case 'Home':
      actions.focusItem(visible[0]);
      break;
    case 'End':
      actions.focusItem(visible[visible.length - 1]);
      break;
    case 'Enter':
      if (isFolder) actions.toggleFolder(id);
      else actions.selectFile(id);
      break;
    default:
      return; // other keys keep their normal behaviour
  }

  event.preventDefault(); // stop arrow keys from scrolling the page
}
