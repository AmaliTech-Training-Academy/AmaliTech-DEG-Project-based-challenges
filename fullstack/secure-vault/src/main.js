import './styles/tokens.css';
import './styles/base.css';
import './styles/layout.css';
import './styles/tree.css';
import './styles/properties.css';
import './styles/content.css';
import data from '../data.json';
import { el, renderInto } from './utils/dom.js';
import { buildIndex, getAncestors } from './utils/tree.js';
import { createStore } from './state/store.js';
import { createActions } from './state/actions.js';
import { createLayout } from './components/Layout.js';
import { Tree } from './components/Tree.js';
import { handleTreeKey } from './components/treeKeyboard.js';
import { Properties } from './components/Properties.js';
import { FolderView } from './components/FolderView.js';

const index = buildIndex(data);

const store = createStore({
  expanded: new Set(['root_1', 'leg_1']), // ids of open folders
  selectedId: null, // id of the selected file
  focusedId: null, // id of the item with keyboard focus
  currentFolderId: null, // folder shown in the main area (null = top level)
  listFilter: 'all', // 'all' | 'folder' | 'file'
});
const actions = createActions(store, index);

const layout = createLayout();
document.getElementById('app').replaceWith(layout.root);

function onTreeKeyDown(event) {
  handleTreeKey(event, { nodes: data, index, state: store.get(), actions });
}

function renderSidebar(state) {
  // The tree is rebuilt on every render, so remember if it had focus
  const treeHadFocus = layout.sidebar.contains(document.activeElement);

  layout.sidebar.replaceChildren(
    el(
      'section',
      { className: 'workspace' },
      el('div', { className: 'section-label' }, 'Workspace'),
      Tree({
        nodes: data,
        state,
        onToggle: actions.toggleFolder,
        onSelect: actions.selectFile,
        onKeyDown: onTreeKeyDown,
      }),
    ),
  );

  if (treeHadFocus && state.focusedId) {
    const item = layout.sidebar.querySelector(`[data-id="${state.focusedId}"]`);
    item?.focus();
    item?.scrollIntoView({ block: 'nearest' });
  }
}

function renderMain(state) {
  const folder = state.currentFolderId ? index.get(state.currentFolderId).node : null;

  renderInto(
    layout.main,
    FolderView({
      folder,
      ancestors: folder ? getAncestors(index, folder.id) : [],
      items: folder ? folder.children ?? [] : data,
      state,
      actions,
    }),
  );
}

function renderProperties(state) {
  const selected = state.selectedId ? index.get(state.selectedId).node : null;

  layout.properties.replaceChildren(
    ...Properties({ node: selected, ancestors: selected ? getAncestors(index, selected.id) : [] }),
  );
}

function render() {
  const state = store.get();
  renderSidebar(state);
  renderMain(state);
  renderProperties(state);
}

store.subscribe(render);
render();
