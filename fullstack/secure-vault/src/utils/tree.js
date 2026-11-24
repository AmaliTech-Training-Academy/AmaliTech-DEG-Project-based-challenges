// Walks the whole tree once (recursively) and saves each node by id with its parent id.
// After this, finding any node is instant: index.get(id).
export function buildIndex(nodes, parentId = null, index = new Map()) {
  for (const node of nodes) {
    index.set(node.id, { node, parentId });
    if (node.children) buildIndex(node.children, node.id, index);
  }
  return index;
}

// Ids of the items you can see now, top to bottom. Only goes inside open folders.
export function getVisibleIds(nodes, expanded, result = []) {
  for (const node of nodes) {
    result.push(node.id);
    if (node.type === 'folder' && expanded.has(node.id)) {
      getVisibleIds(node.children ?? [], expanded, result);
    }
  }
  return result;
}

// Search. Returns a smaller copy of the tree: the matches plus the folders that lead to them.
// openIds = folders that hold a match deeper inside, so they must be opened.
export function filterTree(nodes, query) {
  const text = query.trim().toLowerCase();
  const openIds = new Set();
  let matchCount = 0;

  function visit(list) {
    const result = [];
    for (const node of list) {
      const isMatch = node.name.toLowerCase().includes(text);
      if (isMatch) matchCount += 1;

      const children = node.type === 'folder' ? visit(node.children ?? []) : [];

      if (children.length > 0) {
        openIds.add(node.id);
        result.push({ ...node, children }); // copy, the original data is not changed
      } else if (isMatch) {
        result.push(node);
      }
    }
    return result;
  }

  return { nodes: visit(nodes), openIds, matchCount };
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
