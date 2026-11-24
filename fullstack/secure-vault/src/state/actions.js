// All changes to the state go through these functions.
// Each one also moves the keyboard focus to the item it acted on.
export function createActions(store) {
  function setExpanded(id, open) {
    store.set(({ expanded }) => {
      const next = new Set(expanded); // copy, do not change the old Set
      if (open) next.add(id);
      else next.delete(id);
      return { expanded: next, focusedId: id };
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

    selectFile(id) {
      store.set({ selectedId: id, focusedId: id });
    },

    focusItem(id) {
      store.set({ focusedId: id });
    },
  };
}
