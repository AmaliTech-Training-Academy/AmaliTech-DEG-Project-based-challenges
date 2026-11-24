/**
 * Minimal state store (same idea as Redux / Zustand, in a few lines).
 *
 * - get()           -> current state object
 * - set(patch)      -> merge a partial update (or a function of the old state)
 * - subscribe(fn)   -> fn(state) runs after every update
 *
 * The UI never changes itself directly: it updates the store,
 * and the store tells the UI to re-render. One direction of data flow.
 */
export function createStore(initialState) {
  let state = initialState;
  const listeners = new Set();

  return {
    get() {
      return state;
    },

    set(patch) {
      const changes = typeof patch === 'function' ? patch(state) : patch;
      state = { ...state, ...changes };
      listeners.forEach((listener) => listener(state));
    },

    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}
