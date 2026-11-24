import { getAncestors } from '../utils/tree.js';

// All changes to the state go through these functions.
// Each one also moves the keyboard focus to the item it acted on.
export function createActions(store, index) {
  function setExpanded(id, open) {
    store.set(({ expanded }) => {
      const next = new Set(expanded); // copy, do not change the old Set
      if (open) next.add(id);
      else next.delete(id);
      return { expanded: next, focusedId: id, currentFolderId: id };
    });
  }

  return {
    toggleFolder(id) {
      setExpanded(id, !store.get().expanded.has(id));
    },

    expandFolder(id) {
      setExpanded(id, true);
    },

    collapseFolder(id) {
      setExpanded(id, false);
    },

    // Show a folder in the main area and open it (and its parents) in the tree.
    // id = null means the top level "Workspace".
    openFolder(id) {
      if (!id) {
        store.set({ currentFolderId: null });
        return;
      }
      store.set(({ expanded }) => {
        const next = new Set(expanded);
        getAncestors(index, id).forEach((folder) => next.add(folder.id));
        next.add(id);
        return { expanded: next, currentFolderId: id, focusedId: id };
      });
    },

    selectFile(id) {
      store.set({ selectedId: id, focusedId: id, currentFolderId: index.get(id).parentId });
    },

    focusItem(id) {
      store.set({ focusedId: id });
    },

    setListFilter(filter) {
      store.set({ listFilter: filter });
    },
  };
}
