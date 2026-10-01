const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/script.js'), 'utf8');
function functions(start, end) {
  const from = source.indexOf(start), to = source.indexOf(end, from);
  assert.ok(from >= 0 && to > from);
  return source.slice(from, to);
}
function load(code, values) {
  const context = vm.createContext({ URLSearchParams, Set, ...values });
  vm.runInContext(code, context);
  return context;
}

test('user directory loads every backend page using the actual page size', async () => {
  const requests = [];
  const context = load(functions('async function loadUserDirectory(', 'async function showUserDirectory('), {
    api: async (url) => {
      requests.push(url);
      const page = Number(new URLSearchParams(url.split('?')[1]).get('page'));
      const count = page === 3 ? 5 : 100;
      return { list: Array.from({ length: count }, (_, i) => ({ userId: (page - 1) * 100 + i + 1 })), total: 205, size: 100 };
    },
  });
  const users = await context.loadUserDirectory();
  assert.equal(users.length, 205);
  assert.equal(users.at(-1).userId, 205);
  assert.deepEqual(requests, ['/users?page=1&size=100', '/users?page=2&size=100', '/users?page=3&size=100']);
});

test('late project preference response cannot overwrite the newer selection', async () => {
  let release;
  const delayed = new Promise(resolve => { release = resolve; });
  const context = load(functions('async function loadProject(', 'async function loadAllProjects('), {
    projectLoadId: 0, projectDirectoryLoadId: 0, currentProjectId: 1,
    savedFiltersProjectId: null, currentView: 'all', activePage: 'tasks', savedFilters: [],
    viewPreference: {}, collapsedGroups: new Set(), currentSort: '', taskPage: 1,
    taskPageSize: 20, selectedIds: new Set(), workItems: [], columnDefinitions: { status: true },
    boardMode: false, selectedId: null, rememberProject() {}, applySavedFilterState() {},
    buildWorkItemQuery: () => '', renderProjectNavigation() {}, renderSummary() {}, renderAll() {},
    loadTaskCounts: async () => ({}),
    api: async url => {
      if (url === '/projects/1/saved-filters') return delayed;
      if (url.endsWith('/saved-filters')) return [];
      if (url.endsWith('/view-preference')) return { columns: ['status'], sort: url.includes('/1/') ? 'priority,asc' : 'createdAt,desc' };
      if (url.includes('/work-items?')) return { list: [{ id: 'B-1' }], total: 1, page: 1, size: 20 };
      if (url.endsWith('/summary')) return {};
      return [];
    },
  });
  const first = context.loadProject(1);
  await context.loadProject(2);
  release([]); await first;
  assert.equal(context.currentProjectId, 2);
  assert.equal(context.savedFiltersProjectId, 2);
  assert.equal(context.currentSort, 'createdAt,desc');
  assert.equal(context.workItems[0].id, 'B-1');
});

function boardContext(api) {
  return load(functions('async function loadBoard(', 'function descriptionWithImagePreviews(')
    + functions('async function saveQuickField(', 'async function mutate('), {
    boardMode: true, boardRequestId: 0, activePage: 'tasks', currentProjectId: 1, boardColumns: null,
    workItems: [{ id: 'TASK-1', status: '新建' }], isAllProjects: () => false,
    renderBoard() {}, showToast() {}, api,
    buildWorkItemQuery: () => 'page=1&size=200&keyword=target&ownerId=9&view=assigned-to-me',
    canQuickEdit: () => true, statusTransitions: { '新建': ['进行中'] },
    buildStatusPayload: () => ({ status: '进行中' }), selectedIds: new Set(),
    loadCurrentSelection: async () => {},
  });
}

test('board sends filters and can update or drag cards outside the list page', async () => {
  const requests = [];
  const context = boardContext(async (url, options) => {
    requests.push({ url, options });
    return [{ status: '新建', items: [{ id: 'TASK-2', status: '新建' }] }];
  });
  await context.loadBoard();
  const params = new URLSearchParams(requests[0].url.split('?')[1]);
  assert.equal(params.get('keyword'), 'target'); assert.equal(params.get('ownerId'), '9');
  assert.equal(params.get('view'), 'assigned-to-me'); assert.equal(params.get('limit'), '50');
  assert.equal(params.has('page'), false);
  await context.saveQuickField({ dataset: { itemId: 'TASK-2', quickField: 'priority', previous: 'P2' }, value: 'P0' });
  await context.moveBoardCardToStatus('TASK-2', '进行中');
  assert.equal(requests[1].url, '/work-items/TASK-2'); assert.equal(requests[1].options.method, 'PUT');
  assert.equal(requests[2].url, '/work-items/TASK-2/status'); assert.equal(requests[2].options.method, 'PATCH');
});

test('a late board response cannot replace the new project board', async () => {
  let release;
  const delayed = new Promise(resolve => { release = resolve; });
  const context = boardContext(async url => url.startsWith('/projects/1/') ? delayed : [{ status: '新建', items: [{ id: 'B-1' }] }]);
  const first = context.loadBoard(); context.currentProjectId = 2;
  await context.loadBoard(); release([{ status: '新建', items: [{ id: 'A-1' }] }]); await first;
  assert.equal(context.boardColumns[0].items[0].id, 'B-1');
});
