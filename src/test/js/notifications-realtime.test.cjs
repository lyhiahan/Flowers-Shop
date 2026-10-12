const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function notifications(ok = true) {
    const nodes = new Map(), requests = [];
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, { tagName: 'SPAN', textContent: '', style: {},
            classList: { removed: [], remove(...names) { this.removed.push(...names); } } });
        return nodes.get(id);
    };
    const context = {
        document: {
            readyState: 'loading', addEventListener() {}, getElementById: node,
            querySelector(selector) {
                return selector.includes('_csrf') ? { getAttribute() { return selector.includes('_header') ? 'X-CSRF-TOKEN' : 'csrf-value'; } } : null;
            },
            querySelectorAll(selector) { return selector.startsWith('#') && !selector.includes(',') ? [node(selector.slice(1))] : []; }
        },
        fetch: async (url, options) => { requests.push({ url, options }); return { ok }; },
        console: { error() {}, debug() {} }, setTimeout() {}, Date
    };
    context.window = context;
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/notifications-realtime.js'), 'utf8'), context);
    return { api: context.FlowerShopNotif, node, requests };
}

test('notification badges cap at 99+ and hide at zero', () => {
    const app = notifications();
    app.api.updateBadges(120);
    assert.equal(app.node('headerNotifBadge').textContent, '99+');
    app.api.updateBadges(-2);
    assert.equal(app.node('headerNotifBadge').style.display, 'none');
});

test('read notification sends CSRF and only decrements after success', async () => {
    const app = notifications(); app.api.updateBadges(2);
    await app.api.readNotification(7);
    assert.equal(app.node('headerNotifBadge').textContent, 1);
    assert.equal(app.requests[0].options.headers['X-CSRF-TOKEN'], 'csrf-value');
    assert.equal(app.requests[0].url, '/api/notifications/read/7');
});

test('failed read preserves unread UI and releases its button', async () => {
    const app = notifications(false); app.api.updateBadges(2);
    const btn = { disabled: false, removed: false, remove() { this.removed = true; } };
    await app.api.markOneReadPage(btn, 7);
    assert.equal(btn.disabled, false);
    assert.equal(btn.removed, false);
    assert.equal(app.node('notif_7').classList.removed.length, 0);
    assert.equal(app.node('headerNotifBadge').textContent, 2);
    await assert.rejects(app.api.readAllNotifications());
    assert.equal(app.node('headerNotifBadge').textContent, 2);
});
