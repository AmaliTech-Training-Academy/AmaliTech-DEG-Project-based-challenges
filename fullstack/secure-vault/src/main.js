import './styles/tokens.css';
import './styles/base.css';
import './styles/layout.css';
import './styles/tree.css';
import data from '../data.json';
import { el } from './utils/dom.js';
import { createStore } from './state/store.js';
import { createLayout } from './components/Layout.js';
import { Tree } from './components/Tree.js';

const store = createStore({
  // ids of the folders that are currently open
  expanded: new Set(['root_1', 'leg_1']),
});

const layout = createLayout();
document.getElementById('app').replaceWith(layout.root);

function render() {
  const state = store.get();

  layout.sidebar.replaceChildren(
    el(
      'section',
      { className: 'workspace' },
      el('div', { className: 'section-label' }, 'Workspace'),
      Tree({ nodes: data, state }),
    ),
  );
}

store.subscribe(render);
render();

// Temporary placeholders — replaced by real components in the next steps
layout.main.textContent = 'Main content';
layout.properties.textContent = 'Properties';
