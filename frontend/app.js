// ===== AssetPulse frontend (vanilla JS) =====
const API = localStorage.getItem('ap_api') || 'http://localhost:8080/api';
const TYPES = ['LAPTOP','DESKTOP','MONITOR','MOBILE','PERIPHERAL','SOFTWARE_LICENCE','OTHER'];
const STATUSES = ['AVAILABLE','ALLOCATED','CHECKED_OUT','MAINTENANCE','RETIRED'];
let S = { token: localStorage.ap_t || null, user: JSON.parse(localStorage.ap_u || 'null') };
const $ = (s, e = document) => e.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const fd = d => d ? String(d).slice(0, 10) : '—';
const ft = d => d ? new Date(d).toLocaleString() : '—';
const badge = s => `<span class="badge ${String(s).toLowerCase()}">${esc(String(s).replace('_', ' '))}</span>`;
const opts = (arr, sel, blank) => (blank ? `<option value="">${blank}</option>` : '') + arr.map(x => `<option ${x === sel ? 'selected' : ''}>${x}</option>`).join('');

async function api(p, o = {}) {
  const r = await fetch(API + p, {
    method: o.method || 'GET',
    headers: { 'Content-Type': 'application/json', ...(S.token ? { Authorization: 'Bearer ' + S.token } : {}) },
    body: o.body ? JSON.stringify(o.body) : undefined
  }).catch(() => { throw new Error('Cannot reach backend at ' + API); });
  if (r.status === 401 && S.token) { logout(); throw new Error('Session expired, please sign in'); }
  if (!r.ok) { let m = 'Request failed (' + r.status + ')'; try { m = (await r.json()).message || m; } catch {} throw new Error(m); }
  return o.blob ? r.blob() : r.json().catch(() => null);
}
function toast(m, err) { const t = document.createElement('div'); t.className = 'toast' + (err ? ' err' : ''); t.textContent = m; $('#toasts').append(t); setTimeout(() => t.remove(), 3500); }
const guard = fn => async (...a) => { try { await fn(...a); } catch (e) { toast(e.message, true); } };
function modal(html) { $('#modal').innerHTML = `<div class="mbox">${html}</div>`; $('#modal').onclick = e => { if (e.target.id === 'modal') closeModal(); }; }
function closeModal() { stopCam(); $('#modal').innerHTML = ''; }

// ---------- auth ----------
function logout() { localStorage.removeItem('ap_t'); localStorage.removeItem('ap_u'); S = { token: null, user: null }; render(); }
function setAuth(r) { S = { token: r.token, user: r.user }; localStorage.ap_t = r.token; localStorage.ap_u = JSON.stringify(r.user); location.hash = ''; render(); }

function loginView(reg) {
  $('#app').innerHTML = `<div class="login"><div class="box">
    <div class="logo">◆ AssetPulse</div><p class="mute">${reg ? 'Create an employee account' : 'Smart asset management. Sign in to continue.'}</p>
    <form id="lf">${reg ? '<label>Full name</label><input name="name" required><label>Department</label><input name="department">' : ''}
    <label>Email</label><input name="email" type="email" required value="${reg ? '' : 'admin@assetpulse.com'}">
    <label>Password</label><input name="password" type="password" required minlength="${reg ? 8 : 1}" value="${reg ? '' : 'Admin@123'}">
    <button class="btn" style="width:100%;margin-top:18px">${reg ? 'Register' : 'Sign in'}</button></form>
    <p style="text-align:center"><a href="#" id="sw">${reg ? 'Have an account? Sign in' : 'New employee? Register'}</a></p>
    ${reg ? '' : `<div class="mute" style="font-size:12px;margin-top:10px">Demo: <a href="#" data-d="admin@assetpulse.com|Admin@123">Admin</a> · <a href="#" data-d="manager@assetpulse.com|Manager@123">Manager</a> · <a href="#" data-d="alice@assetpulse.com|Employee@123">Employee</a></div>`}
  </div></div>`;
  $('#sw').onclick = e => { e.preventDefault(); loginView(!reg); };
  document.querySelectorAll('[data-d]').forEach(a => a.onclick = e => { e.preventDefault(); const [u, p] = a.dataset.d.split('|'); $('[name=email]').value = u; $('[name=password]').value = p; });
  $('#lf').onsubmit = guard(async e => {
    e.preventDefault(); const b = Object.fromEntries(new FormData(e.target));
    setAuth(await api(reg ? '/auth/register' : '/auth/login', { method: 'POST', body: b }));
  });
}

// ---------- shell ----------
const NAV = {
  dashboard: ['📊', 'Dashboard', ['ADMIN', 'MANAGER']], assets: ['💻', 'Assets', ['ADMIN', 'MANAGER']],
  mine: ['🧑‍💼', 'My Assets', ['ADMIN', 'MANAGER', 'EMPLOYEE']], scan: ['📷', 'QR Scan', ['ADMIN', 'MANAGER', 'EMPLOYEE']],
  approvals: ['✅', 'Approvals', ['ADMIN', 'MANAGER']], alerts: ['🔔', 'Alerts', ['ADMIN', 'MANAGER']],
  insights: ['💡', 'Insights', ['ADMIN', 'MANAGER']], users: ['👥', 'Users', ['ADMIN']]
};
const PAGES = { dashboard, assets, mine, scan, approvals, alerts, insights, users };
function render() {
  if (!S.token) return loginView();
  const role = S.user.role, allowed = Object.keys(NAV).filter(k => NAV[k][2].includes(role));
  let page = location.hash.slice(1); if (!allowed.includes(page)) page = allowed[0];
  $('#app').innerHTML = `<div class="layout"><aside><div class="logo">◆ AssetPulse</div>
    ${allowed.map(k => `<a data-p="${k}" class="${k === page ? 'on' : ''}">${NAV[k][0]} ${NAV[k][1]}</a>`).join('')}
    <div class="me"><b>${esc(S.user.name)}</b>${role}<br><a id="lo" style="padding:6px 0 0;color:#fda4af">Sign out</a></div></aside><main id="main"></main></div>`;
  document.querySelectorAll('[data-p]').forEach(a => a.onclick = () => { location.hash = a.dataset.p; });
  $('#lo').onclick = logout;
  $('#main').innerHTML = '<div class="empty">Loading…</div>';
  guard(PAGES[page])();
}
window.onhashchange = () => S.token && render();
const head = (t, sub, btn = '') => `<div class="head"><div><h1>${t}</h1><div class="mute">${sub}</div></div><div>${btn}</div></div>`;
const table = (cols, rows) => rows.length ? `<div class="tw"><table><thead><tr>${cols.map(c => `<th>${c}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table></div>` : '<div class="empty">Nothing here yet 🎉</div>';
const bars = (obj, total) => Object.entries(obj).map(([k, v]) => `<div class="row"><span>${k.replace('_', ' ')}</span><div class="track"><div class="fill" style="width:${total ? v * 100 / total : 0}%"></div></div><b>${v}</b></div>`).join('') || '<div class="empty">No data</div>';

// ---------- dashboard ----------
async function dashboard() {
  const [d, u] = await Promise.all([api('/analytics/dashboard'), api('/analytics/utilisation-by-type')]);
  const k = (l, v, c = '') => `<div class="kpi ${c}"><small>${l}</small><div>${v}</div></div>`;
  $('#main').innerHTML = head('Dashboard', 'Live overview of your asset estate') + `<div class="grid">
    ${k('Total assets', d.totalAssets)}${k('Utilisation', d.utilisationRate + '%', 'g')}${k('Idle / underused', d.idleAssets, 'w')}${k('Pending requests', d.pendingRequests)}
    ${k('Open alerts', d.activeAlerts, 'b')}${k('Warranties expiring', d.warrantiesExpiringSoon, 'w')}${k('Licences expiring', d.licencesExpiringSoon, 'w')}${k('Asset value', '₹' + Number(d.totalAssetValue).toLocaleString())}</div>
    <div class="grid two"><div class="card"><h2>Assets by status</h2>${bars(d.byStatus, Object.values(d.byStatus).reduce((a, b) => a + b, 0))}</div>
    <div class="card"><h2>Assets by type</h2>${bars(d.byType, d.totalAssets)}</div></div>
    <div class="grid two"><div class="card"><h2>Utilisation by type</h2>${u.map(t => `<div class="row"><span>${t.type.replace('_', ' ')}</span><div class="track"><div class="fill" style="width:${t.total ? t.inUse * 100 / t.total : 0}%"></div></div><b>${t.inUse}/${t.total}</b></div>`).join('') || '<div class="empty">No data</div>'}<small class="mute">In-use vs total · ${u.reduce((a, t) => a + t.idle, 0)} idle</small></div>
    <div class="card"><h2>Recent activity</h2>${table(['Asset', 'Action', 'User', 'When'], d.recentActivity.slice(0, 8).map(r => `<tr><td>${esc(r.assetName)}</td><td>${badge(r.action)}</td><td>${esc(r.userName || '—')}</td><td>${ft(r.timestamp)}</td></tr>`))}</div></div>`;
}

// ---------- assets ----------
async function assets() {
  const isAdmin = S.user.role === 'ADMIN';
  $('#main').innerHTML = head('Assets', 'Manage laptops, licences and equipment', isAdmin ? '<button class="btn" id="add">+ Add asset</button>' : '') +
    `<div class="card"><div class="bar"><input id="q" placeholder="Search name, tag, serial…"><select id="fs">${opts(STATUSES, '', 'All statuses')}</select><select id="ft">${opts(TYPES, '', 'All types')}</select></div><div id="tbl"></div></div>`;
  const load = guard(async () => {
    const qs = new URLSearchParams({ q: $('#q').value, status: $('#fs').value, type: $('#ft').value });
    ['q', 'status', 'type'].forEach(k => !qs.get(k) && qs.delete(k));
    const list = await api('/assets?' + qs);
    $('#tbl').innerHTML = table(['Tag', 'Name', 'Type', 'Status', 'Assigned to', 'Warranty', 'Licence', ''], list.map(a => `<tr><td><b>${a.assetTag}</b></td><td>${esc(a.name)}<div class="mute">${esc(a.brand || '')} ${esc(a.model || '')}</div></td><td>${a.type.replace('_', ' ')}</td><td>${badge(a.status)}</td><td>${esc(a.assignedToName || '—')}</td><td>${fd(a.warrantyExpiry)}</td><td>${fd(a.licenceExpiry)}</td>
      <td style="white-space:nowrap"><button class="btn s g" data-qr="${a.id}" data-t="${a.assetTag}">QR</button> <button class="btn s g" data-h="${a.id}">History</button>${isAdmin ? ` <button class="btn s g" data-e="${a.id}">Edit</button> <button class="btn s bad" data-r="${a.id}">Retire</button>` : ''}</td></tr>`));
    const by = id => list.find(a => a.id == id);
    document.querySelectorAll('[data-qr]').forEach(b => b.onclick = guard(() => qrModal(b.dataset.qr, b.dataset.t)));
    document.querySelectorAll('[data-h]').forEach(b => b.onclick = guard(() => historyModal(b.dataset.h)));
    document.querySelectorAll('[data-e]').forEach(b => b.onclick = () => assetForm(by(b.dataset.e), load));
    document.querySelectorAll('[data-r]').forEach(b => b.onclick = guard(async () => { if (confirm('Retire this asset?')) { await api('/assets/' + b.dataset.r, { method: 'DELETE' }); toast('Asset retired'); load(); } }));
  });
  ['q', 'fs', 'ft'].forEach(i => $('#' + i).oninput = load); if (isAdmin) $('#add').onclick = () => assetForm(null, load);
  load();
}
function assetForm(a, reload) {
  a = a || {}; const v = k => esc(a[k] ?? '');
  modal(`<h2>${a.id ? 'Edit' : 'Add'} asset</h2><form id="af"><div class="f2">
    <div><label>Name *</label><input name="name" required value="${v('name')}"></div><div><label>Type *</label><select name="type">${opts(TYPES, a.type)}</select></div>
    <div><label>Brand</label><input name="brand" value="${v('brand')}"></div><div><label>Model</label><input name="model" value="${v('model')}"></div>
    <div><label>Serial number</label><input name="serialNumber" value="${v('serialNumber')}"></div><div><label>Status</label><select name="status">${opts(['AVAILABLE', 'MAINTENANCE', 'RETIRED'], a.status)}</select></div>
    <div><label>Purchase date</label><input type="date" name="purchaseDate" value="${fd(a.purchaseDate).replace('—', '')}"></div><div><label>Purchase cost</label><input type="number" step="0.01" name="purchaseCost" value="${v('purchaseCost')}"></div>
    <div><label>Warranty expiry</label><input type="date" name="warrantyExpiry" value="${fd(a.warrantyExpiry).replace('—', '')}"></div><div><label>Licence expiry</label><input type="date" name="licenceExpiry" value="${fd(a.licenceExpiry).replace('—', '')}"></div>
    <div><label>Location</label><input name="location" value="${v('location')}"></div><div><label>Notes</label><input name="notes" value="${v('notes')}"></div></div>
    <div class="acts"><button type="button" class="btn g" onclick="closeModal()">Cancel</button><button class="btn">Save</button></div></form>`);
  $('#af').onsubmit = guard(async e => {
    e.preventDefault(); const b = Object.fromEntries(new FormData(e.target)); Object.keys(b).forEach(k => b[k] === '' && (b[k] = null));
    if (a.id && ['ALLOCATED', 'CHECKED_OUT'].includes(a.status)) delete b.status;
    await api('/assets' + (a.id ? '/' + a.id : ''), { method: a.id ? 'PUT' : 'POST', body: b }); closeModal(); toast('Asset saved'); reload();
  });
}
async function qrModal(id, tag) {
  const url = URL.createObjectURL(await api(`/assets/${id}/qr?size=320`, { blob: true }));
  modal(`<div style="text-align:center"><h2>${tag}</h2><img src="${url}" width="280" alt="QR"><p class="mute">Encodes ASSETPULSE:${tag}</p>
    <div class="acts" style="justify-content:center"><a class="btn g" href="${url}" download="${tag}.png">Download</a><button class="btn" onclick="const w=window.open();w.document.write('<img src=&quot;${url}&quot; onload=&quot;print()&quot;>')">Print</button><button class="btn g" onclick="closeModal()">Close</button></div></div>`);
}
async function historyModal(id) {
  const h = await api(`/assets/${id}/history`);
  modal(`<h2>Asset history</h2>${table(['When', 'Action', 'User', 'Note'], h.map(r => `<tr><td>${ft(r.timestamp)}</td><td>${badge(r.action)}</td><td>${esc(r.userName || '—')}</td><td>${esc(r.note || '')}</td></tr>`))}<div class="acts"><button class="btn g" onclick="closeModal()">Close</button></div>`);
}

// ---------- QR scan ----------
let cam = null, camTimer = null;
function stopCam() { clearInterval(camTimer); cam && cam.getTracks().forEach(t => t.stop()); cam = null; }
async function scan() {
  $('#main').innerHTML = head('QR Scan', 'Scan an asset QR code to check it out, in, or confirm usage') + `<div class="grid two"><div class="card"><h2>Camera</h2><video id="vid" playsinline muted></video><div class="acts" style="justify-content:flex-start"><button class="btn" id="cs">Start camera</button></div><small class="mute">${'BarcodeDetector' in window ? 'Point the camera at a QR code.' : 'Camera decoding needs Chrome/Edge/Android. Use manual entry instead.'}</small></div>
    <div class="card"><h2>Asset code</h2><label>Scanned / typed code</label><input id="code" placeholder="ASSETPULSE:AST-XXXXXXXX"><div class="acts" style="justify-content:flex-start"><button class="btn g" id="lk">Look up</button><button class="btn ok" id="co">Check out / confirm usage</button><button class="btn" id="ci">Check in</button></div><div id="res" style="margin-top:14px"></div></div></div>`;
  const show = a => $('#res').innerHTML = `<div class="rec"><b>${esc(a.name)}</b> (${a.assetTag}) ${badge(a.status)}<br><span class="mute">Holder: ${esc(a.assignedToName || 'none')} · Last activity ${ft(a.lastActivityAt)}</span></div>`;
  const act = (path, msg) => guard(async () => { const a = await api(path, { method: path.includes('lookup') ? 'GET' : 'POST', body: path.includes('lookup') ? undefined : { code: $('#code').value.trim() } }); show(a); toast(msg); });
  $('#lk').onclick = guard(async () => show(await api('/scan/lookup?code=' + encodeURIComponent($('#code').value.trim()))));
  $('#co').onclick = act('/scan/check-out', 'Checked out / usage logged'); $('#ci').onclick = act('/scan/check-in', 'Checked in');
  $('#cs').onclick = guard(async () => {
    if (!('BarcodeDetector' in window)) throw new Error('Camera scanning not supported in this browser');
    cam = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } }); $('#vid').srcObject = cam; await $('#vid').play();
    const det = new BarcodeDetector({ formats: ['qr_code'] });
    camTimer = setInterval(async () => { const r = await det.detect($('#vid')).catch(() => []); if (r.length) { $('#code').value = r[0].rawValue; stopCam(); $('#lk').click(); } }, 400);
  });
}

// ---------- my assets & requests ----------
async function mine() {
  const [as, rq] = await Promise.all([api('/assets/mine'), api('/requests/mine')]);
  $('#main').innerHTML = head('My Assets', 'Assets assigned to you and your requests', '<button class="btn" id="nr">+ Request an asset</button>') +
    `<div class="card"><h2>Assigned to me</h2>${table(['Tag', 'Name', 'Type', 'Status', 'Warranty', ''], as.map(a => `<tr><td><b>${a.assetTag}</b></td><td>${esc(a.name)}</td><td>${a.type.replace('_', ' ')}</td><td>${badge(a.status)}</td><td>${fd(a.warrantyExpiry)}</td><td>${a.status === 'ALLOCATED' ? `<button class="btn s g" data-ret="${a.id}">Request return</button>` : ''}</td></tr>`))}</div>
    <div class="card"><h2>My requests</h2>${table(['#', 'Type', 'Asset', 'Reason', 'Status', 'Comments', ''], rq.map(r => `<tr><td>${r.id}</td><td>${r.type}</td><td>${esc(r.assetName || r.requestedAssetType)}</td><td>${esc(r.reason || '')}</td><td>${badge(r.status)}</td><td class="mute">${esc([r.managerComment, r.adminComment].filter(Boolean).join(' | '))}</td><td>${r.status.startsWith('PENDING') ? `<button class="btn s bad" data-c="${r.id}">Cancel</button>` : ''}</td></tr>`))}</div>`;
  $('#nr').onclick = guard(async () => {
    const av = await api('/assets/available');
    modal(`<h2>Request an asset</h2><form id="rf"><label>Asset type</label><select name="assetType">${opts(TYPES)}</select><label>Specific asset (optional)</label><select name="assetId"><option value="">Any available</option>${av.map(a => `<option value="${a.id}">${esc(a.name)} (${a.assetTag})</option>`).join('')}</select><label>Reason</label><textarea name="reason" required rows="3"></textarea><div class="acts"><button type="button" class="btn g" onclick="closeModal()">Cancel</button><button class="btn">Submit</button></div></form>`);
    $('#rf').onsubmit = guard(async e => { e.preventDefault(); const b = Object.fromEntries(new FormData(e.target)); b.assetId = b.assetId ? +b.assetId : null; await api('/requests/allocation', { method: 'POST', body: b }); closeModal(); toast('Request submitted'); mine(); });
  });
  document.querySelectorAll('[data-ret]').forEach(b => b.onclick = guard(async () => { const reason = prompt('Reason for return?') ; if (reason === null) return; await api('/requests/return', { method: 'POST', body: { assetId: +b.dataset.ret, reason } }); toast('Return requested'); mine(); }));
  document.querySelectorAll('[data-c]').forEach(b => b.onclick = guard(async () => { await api(`/requests/${b.dataset.c}/cancel`, { method: 'POST' }); toast('Cancelled'); mine(); }));
}

// ---------- approvals ----------
async function approveFull(r, approve, comment, assetId) {
  const body = { approve, comment, assetId: assetId || null };
  if (r.status === 'PENDING_MANAGER') { await api(`/requests/${r.id}/manager-decision`, { method: 'POST', body }); if (!approve || S.user.role !== 'ADMIN') return; }
  if (S.user.role === 'ADMIN') await api(`/requests/${r.id}/admin-decision`, { method: 'POST', body });
}
async function approvals() {
  const admin = S.user.role === 'ADMIN';
  const list = admin ? [...await api('/requests?status=PENDING_MANAGER'), ...await api('/requests/pending/admin')] : await api('/requests/pending/manager');
  $('#main').innerHTML = head('Approvals', admin ? 'Manager and admin approval queue' : 'Requests from your team') +
    `<div class="card">${table(['#', 'Requester', 'Type', 'Asset', 'Reason', 'Stage', 'Raised', ''], list.map(r => `<tr><td>${r.id}</td><td>${esc(r.requesterName)}</td><td>${r.type}</td><td>${esc(r.assetName || r.requestedAssetType)}</td><td>${esc(r.reason || '')}</td><td>${badge(r.status)}</td><td>${ft(r.createdAt)}</td><td><button class="btn s ok" data-a="${r.id}">Review</button></td></tr>`))}</div>`;
  document.querySelectorAll('[data-a]').forEach(b => b.onclick = guard(async () => {
    const r = list.find(x => x.id == b.dataset.a); let sel = '';
    if (admin && r.type === 'ALLOCATION') { const av = await api('/assets/available?type=' + r.requestedAssetType); sel = `<label>Asset to allocate</label><select id="as"><option value="">— choose —</option>${av.map(a => `<option value="${a.id}" ${a.id == r.assetId ? 'selected' : ''}>${esc(a.name)} (${a.assetTag})</option>`).join('')}</select>`; }
    modal(`<h2>Request #${r.id}: ${r.type}</h2><p><b>${esc(r.requesterName)}</b> · ${esc(r.assetName || r.requestedAssetType)}<br><span class="mute">${esc(r.reason || '')}</span></p>${sel}<label>Comment</label><textarea id="cm" rows="2"></textarea><div class="acts"><button class="btn g" onclick="closeModal()">Close</button><button class="btn bad" id="rj">Reject</button><button class="btn ok" id="ap">Approve</button></div>`);
    const go = ok => guard(async () => { await approveFull(r, ok, $('#cm').value, $('#as') && +$('#as').value); closeModal(); toast(ok ? 'Approved' : 'Rejected'); approvals(); });
    $('#ap').onclick = go(true); $('#rj').onclick = go(false);
  }));
}

// ---------- alerts ----------
async function alerts() {
  const admin = S.user.role === 'ADMIN', l = await api('/alerts');
  $('#main').innerHTML = head('Alerts', 'Warranty and licence expiry', admin ? '<button class="btn" id="sc">Run expiry scan</button>' : '') +
    `<div class="card">${table(['Type', 'Asset', 'Message', 'Expires', ''], l.map(a => `<tr><td>${badge(a.type.replace('_EXPIRY', ''))}</td><td><b>${a.assetTag}</b> ${esc(a.assetName)}</td><td>${esc(a.message)}</td><td>${fd(a.expiryDate)}</td><td>${admin ? `<button class="btn s g" data-k="${a.id}">Acknowledge</button>` : ''}</td></tr>`))}</div>`;
  if (admin) $('#sc').onclick = guard(async () => { const r = await api('/alerts/scan', { method: 'POST' }); toast(r.newAlerts + ' new alert(s)'); alerts(); });
  document.querySelectorAll('[data-k]').forEach(b => b.onclick = guard(async () => { await api(`/alerts/${b.dataset.k}/acknowledge`, { method: 'POST' }); toast('Acknowledged'); alerts(); }));
}

// ---------- insights ----------
async function insights() {
  const admin = S.user.role === 'ADMIN', [idle, recs] = await Promise.all([api('/analytics/idle-assets'), api('/analytics/recommendations')]);
  $('#main').innerHTML = head('Insights', 'Idle equipment and smart reassignment suggestions') +
    `<div class="card"><h2>💡 Recommendations</h2>${recs.length ? recs.map(r => `<div class="rec"><b>${esc(r.requesterName)}</b> requested a <b>${r.requestedType.replace('_', ' ')}</b> <span class="mute">(request #${r.requestId})</span><br>${esc(r.message)}
      ${r.suggestions.length && admin ? `<div style="margin-top:8px">${r.suggestions.map(s => `<button class="btn s" data-as="${r.requestId}|${s.assetId}">Assign ${esc(s.name)} (${s.assetTag})</button>`).join(' ')}</div>` : ''}</div>`).join('') : '<div class="empty">No open allocation requests</div>'}</div>
    <div class="card"><h2>Idle & underused assets</h2>${table(['Tag', 'Name', 'Type', 'Category', 'Holder', 'Idle days', 'Last activity'], idle.map(i => `<tr><td><b>${i.assetTag}</b></td><td>${esc(i.name)}</td><td>${i.type.replace('_', ' ')}</td><td>${badge(i.category === 'UNUSED_IN_STOCK' ? 'available' : 'allocated')} ${i.category.replace(/_/g, ' ').toLowerCase()}</td><td>${esc(i.assignedToName || '—')}</td><td><b>${i.idleDays}</b></td><td>${ft(i.lastActivityAt)}</td></tr>`))}</div>`;
  document.querySelectorAll('[data-as]').forEach(b => b.onclick = guard(async () => {
    const [rid, aid] = b.dataset.as.split('|'); if (!confirm('Approve this request with the suggested asset?')) return;
    const r = (await api('/requests?status=PENDING_MANAGER')).concat(await api('/requests/pending/admin')).find(x => x.id == rid);
    await approveFull(r, true, 'Approved via AssetPulse recommendation', +aid); toast('Asset assigned'); insights();
  }));
}

// ---------- users ----------
async function users() {
  const l = await api('/users');
  $('#main').innerHTML = head('Users', 'Manage accounts and roles', '<button class="btn" id="nu">+ Add user</button>') +
    `<div class="card">${table(['Name', 'Email', 'Role', 'Department', 'Manager', 'Active', ''], l.map(u => `<tr><td>${esc(u.name)}</td><td>${esc(u.email)}</td><td>${badge(u.role)}</td><td>${esc(u.department || '—')}</td><td>${esc(u.managerName || '—')}</td><td>${u.active ? '✔' : '✖'}</td><td><button class="btn s g" data-u="${u.id}">Edit</button></td></tr>`))}</div>`;
  const form = u => { u = u || {}; const mgrs = l.filter(x => x.role !== 'EMPLOYEE');
    modal(`<h2>${u.id ? 'Edit' : 'Add'} user</h2><form id="uf"><div class="f2"><div><label>Name</label><input name="name" required value="${esc(u.name || '')}"></div><div><label>Email</label><input type="email" name="email" required value="${esc(u.email || '')}"></div>
      <div><label>Password ${u.id ? '(leave blank to keep)' : ''}</label><input type="password" name="password" minlength="8" ${u.id ? '' : 'required'}></div><div><label>Role</label><select name="role">${opts(['EMPLOYEE', 'MANAGER', 'ADMIN'], u.role)}</select></div>
      <div><label>Department</label><input name="department" value="${esc(u.department || '')}"></div><div><label>Manager</label><select name="managerId"><option value="">None</option>${mgrs.map(m => `<option value="${m.id}" ${m.id === u.managerId ? 'selected' : ''}>${esc(m.name)}</option>`).join('')}</select></div>
      <div><label>Active</label><select name="active"><option value="true">Yes</option><option value="false" ${u.active === false ? 'selected' : ''}>No</option></select></div></div>
      <div class="acts"><button type="button" class="btn g" onclick="closeModal()">Cancel</button><button class="btn">Save</button></div></form>`);
    $('#uf').onsubmit = guard(async e => { e.preventDefault(); const b = Object.fromEntries(new FormData(e.target)); b.managerId = b.managerId ? +b.managerId : null; b.active = b.active === 'true'; if (!b.password) delete b.password;
      await api('/users' + (u.id ? '/' + u.id : ''), { method: u.id ? 'PUT' : 'POST', body: b }); closeModal(); toast('User saved'); users(); }); };
  $('#nu').onclick = () => form(); document.querySelectorAll('[data-u]').forEach(b => b.onclick = () => form(l.find(x => x.id == b.dataset.u)));
}

render();
