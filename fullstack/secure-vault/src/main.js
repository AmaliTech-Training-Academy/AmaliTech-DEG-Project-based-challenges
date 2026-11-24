import './styles/tokens.css';
import './styles/base.css';
import './styles/layout.css';
import './styles/tree.css';
import './styles/properties.css';
import data from '../data.json';
import { el } from './utils/dom.js';
import { buildIndex, getAncestors } from './utils/tree.js';
import { createStore } from './state/store.js';
import { createActions } from './state/actions.js';
import { createLayout } from './components/Layout.js';
import { Tree } from './components/Tree.js';
import { Properties } from './components/Properties.js';

const index = buildIndex(data);

const store = createStore({
  expanded: new Set(['root_1', 'leg_1']), // ids of open folders
  selectedId: null, // id of the selected file
});
const actions = createActions(store);

const layout = createLayout();
document.getElementById('app').replaceWith(layout.root);

function render() {
  const state = store.get();
  const selected = state.selectedId ? index.get(state.selectedId).node : null;

  layout.sidebar.replaceChildren(
    el(
      'section',
      { className: 'workspace' },
      el('div', { className: 'section-label' }, 'Workspace'),
      Tree({ nodes: data, state, onToggle: actions.toggleFolder, onSelect: actions.selectFile }),
    ),
  );

  layout.properties.replaceChildren(
    ...Properties({ node: selected, ancestors: selected ? getAncestors(index, selected.id) : [] }),
  );
}

store.subscribe(render);
render();

// Placeholder for now
layout.main.textContent = 'Main content';
