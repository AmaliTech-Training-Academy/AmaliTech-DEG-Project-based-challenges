// All changes to the state go through these functions.
export function createActions(store) {
  return {
    toggleFolder(id) {
      store.set(({ expanded }) => {
        const next = new Set(expanded); // copy, do not change the old Set
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return { expanded: next };
      });
    },

    selectFile(id) {
      store.set({ selectedId: id });
    },
  };
}
