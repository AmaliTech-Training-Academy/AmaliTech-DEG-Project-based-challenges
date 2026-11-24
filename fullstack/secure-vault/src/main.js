import './styles/tokens.css';
import './styles/base.css';
import './styles/layout.css';
import './styles/tree.css';
import './styles/properties.css';
import './styles/content.css';
import './styles/search.css';
import data from '../data.json';
import { el, renderInto } from './utils/dom.js';
import { buildIndex, getAncestors, filterTree } from './utils/tree.js';
import { createStore } from './state/store.js';
import { createActions } from './state/actions.js';
import { createLayout } from './components/Layout.js';
import { Tree } from './components/Tree.js';
import { handleTreeKey } from './components/treeKeyboard.js';
import { Properties } from './components/Properties.js';
import { FolderView } from './components/FolderView.js';
import { SearchBox } from './components/SearchBox.js';

const index = buildIndex(data);

const store = createStore({
  expanded: new Set(['root_1', 'leg_1']), // ids of open folders
  selectedId: null, // id of the selected file
  focusedId: null, // id of the item with keyboard focus
  currentFolderId: null, // folder shown in the main area (null = top level)
  listFilter: 'all', // 'all' | 'folder' | 'file'
  query: '', // search text
});
const actions = createActions(store, index);

const layout = createLayout();
document.getElementById('app').replaceWith(layout.root);

// The tree to show: everything, or only the search results
function getTreeNodes(state) {
  return state.query.trim() ? filterTree(data, state.query).nodes : data;
}

function onSearch(query) {
  const { openIds } = filterTree(data, query);
  actions.setQuery(query, query.trim() ? openIds : []);
}

function onTreeKeyDown(event) {
  const state = store.get();
  handleTreeKey(event, { nodes: getTreeNodes(state), index, state, actions });
}

layout.search.append(SearchBox({ onSearch }));

// Press "/" anywhere to jump to the search box
document.addEventListener('keydown', (event) => {
  const typing = event.target.closest('input, textarea');
  if (event.key === '/' && !typing) {
    event.preventDefault();
    document.getElementById('vault-search').focus();
  }
});

function renderSidebar(state) {
  // The tree is rebuilt on every render, so remember if it had focus
  const treeHadFocus = layout.sidebar.contains(document.activeElement);

  const isSearching = state.query.trim() !== '';
  const result = isSearching ? filterTree(data, state.query) : null;
  const nodes = isSearching ? result.nodes : data;

  let status = null;
  if (isSearching) {
    const count = result.matchCount;
    status = el('p', { className: 'search-status', role: 'status' }, `${count} ${count === 1 ? 'match' : 'matches'}`);
  }

  layout.sidebar.replaceChildren(
    el(
      'section',
      { className: 'workspace' },
      el('div', { className: 'section-label' }, isSearching ? 'Search results' : 'Workspace'),
      status,
      nodes.length > 0
        ? Tree({
            nodes,
            state,
            onToggle: actions.toggleFolder,
            onSelect: actions.selectFile,
            onKeyDown: onTreeKeyDown,
          })
        : el('p', { className: 'search-status' }, `Nothing found for "${state.query.trim()}".`),
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

// Opens the file's folders, then moves keyboard focus to it in the tree
function showInFolder(id) {
  actions.revealFile(id);
  const item = layout.sidebar.querySelector(`[data-id="${id}"]`);
  item?.focus();
  item?.scrollIntoView({ block: 'nearest' });
}

function renderProperties(state) {
  const selected = state.selectedId ? index.get(state.selectedId).node : null;

  layout.properties.replaceChildren(
    ...Properties({
      node: selected,
      ancestors: selected ? getAncestors(index, selected.id) : [],
      onShowInFolder: showInFolder,
    }),
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
