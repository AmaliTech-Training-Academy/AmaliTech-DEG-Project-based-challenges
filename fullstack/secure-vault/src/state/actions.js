/**
 * Actions = the only functions allowed to change the state.
 * Components call these; they never edit the state themselves.
 */
export function createActions(store) {
  return {
    /** Open a closed folder, or close an open one. */
    toggleFolder(id) {
      store.set(({ expanded }) => {
        // Copy the Set instead of mutating it, so every update is a new state
        const next = new Set(expanded);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return { expanded: next };
      });
    },
  };
}
