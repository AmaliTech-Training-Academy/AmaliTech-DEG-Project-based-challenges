// Walks the whole tree once (recursively) and saves each node by id with its parent id.
// After this, finding any node is instant: index.get(id).
export function buildIndex(nodes, parentId = null, index = new Map()) {
  for (const node of nodes) {
    index.set(node.id, { node, parentId });
    if (node.children) buildIndex(node.children, node.id, index);
  }
  return index;
}

// Folders above a node, from the top level down.
export function getAncestors(index, id) {
  const ancestors = [];
  let parentId = index.get(id)?.parentId;

  while (parentId) {
    const entry = index.get(parentId);
    ancestors.unshift(entry.node);
    parentId = entry.parentId;
  }
  return ancestors;
}
