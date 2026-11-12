// cafe 화면. 빌드 도구 없이 브라우저가 바로 실행하는 순수 JavaScript.
// 로그인하면 받은 JWT 를 localStorage 에 두고, 모든 /api 요청에 Authorization: Bearer 헤더로 보냄.
// 역할(CUSTOMER · STAFF)에 따라 손님 화면 / 직원 화면을 보여 줌. 실제 권한 검사는 서버가 함.

const STATUS_LABEL = { RECEIVED: '접수', PREPARING: '제조 중', COMPLETED: '완료', CANCELED: '취소' };

// 직원이 누를 수 있는 버튼. 서버의 OrderStatus.canChangeTo 와 같은 규칙
const STAFF_ACTIONS = {
  RECEIVED: [{ to: 'PREPARING', label: '제조 시작' }, { to: 'CANCELED', label: '취소', danger: true }],
  PREPARING: [{ to: 'COMPLETED', label: '완료' }, { to: 'CANCELED', label: '취소', danger: true }],
  COMPLETED: [],
  CANCELED: [],
};

const AUTH_KEY = 'cafe.auth';
let auth = null;            // { token, username, role }
let staffFilter = '';
let timers = [];

const $ = (id) => document.getElementById(id);
const won = (n) => n.toLocaleString() + '원';

// ---------- 공통 ----------

function showMessage(text, type) {
  const el = $('message');
  el.textContent = text;
  el.className = 'message ' + (type || '');
}

function loadAuth() {
  try {
    const saved = JSON.parse(localStorage.getItem(AUTH_KEY));
    if (saved && saved.token) auth = saved;
  } catch (e) { /* 저장값이 깨졌으면 무시 */ }
}

function saveAuth(value) {
  auth = value;
  try {
    if (value) localStorage.setItem(AUTH_KEY, JSON.stringify(value));
    else localStorage.removeItem(AUTH_KEY);
  } catch (e) { /* 사생활 보호 모드 등 — 새로 고침하면 다시 로그인 */ }
}

// 실패 응답은 서버가 항상 {status, code, message, timestamp} 형태로 줌
async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (!(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  if (auth) headers['Authorization'] = 'Bearer ' + auth.token;

  const res = await fetch(path, { ...options, headers });
  if (res.status === 401 && auth) {
    // 토큰 만료(1시간) · 무효 → 로그인 화면으로
    logout('로그인이 만료됨. 다시 로그인해 줘');
    throw new Error('로그인이 필요함');
  }
  if (res.status === 204) return null;
  const body = await res.json().catch(() => null);
  if (!res.ok) throw new Error(body && body.message ? body.message : '요청 실패 (' + res.status + ')');
  return body;
}

function cell(content) {
  const td = document.createElement('td');
  if (content instanceof Node) td.appendChild(content);
  else td.textContent = content;
  return td;
}

function button(label, onClick, className) {
  const b = document.createElement('button');
  b.type = 'button';
  b.textContent = label;
  b.className = className || 'action';
  b.addEventListener('click', () => onClick(b));
  return b;
}

function badge(status) {
  const s = document.createElement('span');
  s.className = 'badge ' + status;
  s.textContent = STATUS_LABEL[status] || status;
  return s;
}

function emptyRow(tbody, cols, text) {
  const tr = document.createElement('tr');
  const td = cell(text);
  td.colSpan = cols;
  td.className = 'empty';
  tr.appendChild(td);
  tbody.appendChild(tr);
}

async function loadVersion() {
  try {
    const info = await api('/actuator/info');
    $('version').textContent = 'v' + (info.build ? info.build.version : '?');
  } catch (e) {
    $('version').textContent = '버전 확인 실패';
  }
}

// ---------- 화면 전환 ----------

function render() {
  timers.forEach(clearInterval);
  timers = [];
  ['auth-view', 'customer-view', 'staff-view'].forEach((id) => $(id).classList.add('hidden'));
  $('logout').classList.toggle('hidden', !auth);

  if (!auth) {
    $('who').textContent = '';
    $('auth-view').classList.remove('hidden');
    return;
  }
  $('who').textContent = auth.username + (auth.role === 'STAFF' ? ' (직원)' : ' (손님)');

  if (auth.role === 'STAFF') {
    $('staff-view').classList.remove('hidden');
    loadStaffOrders();
    loadStaffMenus();
    timers.push(setInterval(loadStaffOrders, 5000));
  } else {
    $('customer-view').classList.remove('hidden');
    loadCustomerMenus();
    loadMyOrders();
    timers.push(setInterval(loadMyOrders, 5000));
  }
}

function logout(message) {
  saveAuth(null);
  render();
  if (message) showMessage(message, 'error');
}

// ---------- 로그인 · 가입 ----------

async function login(username, password) {
  const t = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) });
  saveAuth({ token: t.accessToken, username: t.username, role: t.role });
  $('password').value = '';
  showMessage(t.username + ' 로그인됨', 'ok');
  render();
}

$('auth-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await login($('username').value.trim(), $('password').value);
  } catch (err) {
    showMessage(err.message, 'error');
  }
});

$('signup-btn').addEventListener('click', async () => {
  const username = $('username').value.trim();
  const password = $('password').value;
  try {
    await api('/api/auth/signup', { method: 'POST', body: JSON.stringify({ username, password }) });
    await login(username, password);
    showMessage('가입 완료 · ' + username + ' 로그인됨', 'ok');
  } catch (err) {
    showMessage(err.message, 'error');
  }
});

$('logout').addEventListener('click', () => {
  logout();
  showMessage('로그아웃됨', 'ok');
});

// ---------- 손님 ----------

async function loadCustomerMenus() {
  try {
    const menus = await api('/api/menus');
    const grid = $('menu-grid');
    grid.innerHTML = '';
    if (menus.length === 0) grid.textContent = '판매 중인 메뉴 없음';
    for (const m of menus) {
      const card = document.createElement('div');
      card.className = 'menu-card';

      const photo = document.createElement('div');
      photo.className = 'photo';
      if (m.imageUrl) photo.style.backgroundImage = 'url("' + encodeURI(m.imageUrl) + '")';
      else photo.textContent = '사진 없음';

      const body = document.createElement('div');
      body.className = 'body';
      const name = document.createElement('div');
      name.className = 'name';
      name.textContent = m.name;
      const price = document.createElement('div');
      price.className = 'price';
      price.textContent = won(m.price);

      const row = document.createElement('div');
      row.className = 'order-row';
      const qty = document.createElement('input');
      qty.type = 'number';
      qty.min = 1;
      qty.max = 10;
      qty.value = 1;
      row.appendChild(qty);
      row.appendChild(button('주문', (b) => placeOrder(m, Number(qty.value), b), ''));

      body.append(name, price, row);
      card.append(photo, body);
      grid.appendChild(card);
    }
  } catch (e) {
    showMessage('메뉴를 불러오지 못함: ' + e.message, 'error');
  }
}

async function placeOrder(menu, quantity, btn) {
  btn.disabled = true;
  try {
    const o = await api('/api/orders', { method: 'POST', body: JSON.stringify({ menuId: menu.id, quantity }) });
    showMessage('주문 #' + o.id + ' 접수됨 · ' + o.menuName + ' × ' + o.quantity + ' = ' + won(o.totalPrice), 'ok');
    await loadMyOrders();
  } catch (e) {
    showMessage(e.message, 'error');
  } finally {
    btn.disabled = false;
  }
}

async function loadMyOrders() {
  try {
    const orders = await api('/api/orders');
    const tbody = $('my-orders');
    tbody.innerHTML = '';
    if (orders.length === 0) return emptyRow(tbody, 7, '주문 없음');
    for (const o of orders) {
      const tr = document.createElement('tr');
      tr.append(cell('#' + o.id), cell(o.menuName), cell(o.quantity), cell(won(o.totalPrice)),
        cell(badge(o.status)), cell(new Date(o.createdAt).toLocaleString()));
      const actions = cell('');
      if (o.status === 'RECEIVED') {
        actions.appendChild(button('취소', (b) => changeStatus(o.id, 'CANCELED', b, loadMyOrders), 'action danger'));
      }
      tr.appendChild(actions);
      tbody.appendChild(tr);
    }
  } catch (e) {
    showMessage('주문을 불러오지 못함: ' + e.message, 'error');
  }
}

async function changeStatus(id, to, btn, reload) {
  btn.disabled = true;
  try {
    await api('/api/orders/' + id + '/status', { method: 'PATCH', body: JSON.stringify({ status: to }) });
    showMessage('#' + id + ' → ' + STATUS_LABEL[to], 'ok');
  } catch (e) {
    showMessage(e.message, 'error');
  }
  await reload();
}

// ---------- 직원 : 주문 ----------

async function loadStaffOrders() {
  const query = staffFilter ? '?status=' + encodeURIComponent(staffFilter) : '';
  try {
    const orders = await api('/api/orders' + query);
    const tbody = $('staff-orders');
    tbody.innerHTML = '';
    if (orders.length === 0) return emptyRow(tbody, 8, '주문 없음');
    for (const o of orders) {
      const tr = document.createElement('tr');
      tr.append(cell('#' + o.id), cell(o.customer), cell(o.menuName), cell(o.quantity), cell(won(o.totalPrice)),
        cell(badge(o.status)), cell(new Date(o.createdAt).toLocaleString()));
      const actions = cell('');
      for (const a of STAFF_ACTIONS[o.status] || []) {
        actions.appendChild(button(a.label, (b) => changeStatus(o.id, a.to, b, loadStaffOrders),
          'action' + (a.danger ? ' danger' : '')));
      }
      tr.appendChild(actions);
      tbody.appendChild(tr);
    }
  } catch (e) {
    showMessage('주문을 불러오지 못함: ' + e.message, 'error');
  }
}

$('filters').addEventListener('click', (e) => {
  const btn = e.target.closest('button[data-status]');
  if (!btn) return;
  staffFilter = btn.dataset.status;
  document.querySelectorAll('.filter').forEach((b) => b.classList.toggle('active', b === btn));
  loadStaffOrders();
});

document.querySelectorAll('.refresh').forEach((b) => b.addEventListener('click', render));

// ---------- 직원 : 메뉴 ----------

async function loadStaffMenus() {
  try {
    const menus = await api('/api/menus?all=true');
    const tbody = $('staff-menus');
    tbody.innerHTML = '';
    if (menus.length === 0) return emptyRow(tbody, 5, '메뉴 없음');
    for (const m of menus) tbody.appendChild(staffMenuRow(m));
  } catch (e) {
    showMessage('메뉴를 불러오지 못함: ' + e.message, 'error');
  }
}

function staffMenuRow(m) {
  const tr = document.createElement('tr');
  if (!m.available) tr.className = 'off';

  let thumb;
  if (m.imageUrl) {
    thumb = document.createElement('img');
    thumb.className = 'thumb';
    thumb.src = m.imageUrl;
    thumb.alt = m.name;
  } else {
    thumb = document.createElement('div');
    thumb.className = 'thumb-empty';
  }

  const name = document.createElement('input');
  name.className = 'inline';
  name.value = m.name;
  name.maxLength = 30;
  const price = document.createElement('input');
  price.className = 'inline-price';
  price.type = 'number';
  price.min = 100;
  price.step = 100;
  price.value = m.price;

  const actions = cell('');
  const upload = document.createElement('label');
  upload.className = 'upload';
  upload.textContent = '사진';
  const file = document.createElement('input');
  file.type = 'file';
  file.accept = 'image/jpeg,image/png,image/webp';
  file.addEventListener('change', () => uploadImage(m.id, file));
  upload.appendChild(file);

  actions.append(
    upload,
    button('저장', (b) => updateMenu(m.id, { name: name.value, price: Number(price.value) }, b)),
    m.available
      ? button('판매 중지', (b) => discontinue(m.id, b), 'action danger')
      : button('판매 재개', (b) => updateMenu(m.id, { available: true }, b)),
  );

  tr.append(cell(thumb), cell(name), cell(price), cell(m.available ? '판매 중' : '중지'), actions);
  return tr;
}

$('menu-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const m = await api('/api/menus', {
      method: 'POST',
      body: JSON.stringify({ name: $('menu-name').value, price: Number($('menu-price').value) }),
    });
    showMessage('메뉴 추가됨 : ' + m.name, 'ok');
    $('menu-name').value = '';
    $('menu-price').value = '';
    await loadStaffMenus();
  } catch (err) {
    showMessage(err.message, 'error');
  }
});

async function updateMenu(id, changes, btn) {
  btn.disabled = true;
  try {
    const m = await api('/api/menus/' + id, { method: 'PATCH', body: JSON.stringify(changes) });
    showMessage('메뉴 저장됨 : ' + m.name, 'ok');
  } catch (e) {
    showMessage(e.message, 'error');
  }
  await loadStaffMenus();
}

async function discontinue(id, btn) {
  if (!confirm('판매를 중지할까? 손님 화면에서 사라지고, 판매 재개로 되돌릴 수 있음.')) return;
  btn.disabled = true;
  try {
    await api('/api/menus/' + id, { method: 'DELETE' });
    showMessage('판매 중지됨', 'ok');
  } catch (e) {
    showMessage(e.message, 'error');
  }
  await loadStaffMenus();
}

async function uploadImage(id, input) {
  const f = input.files[0];
  if (!f) return;
  if (f.size > 2 * 1024 * 1024) {
    showMessage('사진은 2MB 이하', 'error');
    return;
  }
  const form = new FormData();
  form.append('file', f);
  try {
    await api('/api/menus/' + id + '/image', { method: 'PUT', body: form });
    showMessage('사진 올림', 'ok');
  } catch (e) {
    showMessage(e.message, 'error');
  }
  await loadStaffMenus();
}

// ---------- 시작 ----------

loadVersion();
loadAuth();
render();
