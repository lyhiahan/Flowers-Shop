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

function bellDropdown({ id = 'notifBellDropdown', list = [], ok = true, redirected = false } = {}) {
    let init;
    const requests = [], events = new Map();
    const container = {
        html: '', rows: [],
        set innerHTML(value) { this.html = value; this.rows = []; },
        get innerHTML() { return this.html; },
        appendChild(row) { this.rows.push(row); }
    };
    const trigger = {
        id,
        addEventListener(name, callback) { events.set(name, callback); },
        closest() { return { querySelector() { return container; } }; }
    };
    const context = {
        document: {
            readyState: 'loading',
            addEventListener(name, callback) { if (name === 'DOMContentLoaded') init = callback; },
            getElementById() { return null; },
            querySelector() { return null; },
            querySelectorAll(selector) { return selector === '[data-bs-toggle="dropdown"]' ? [trigger] : []; },
            createElement() {
                return {
                    textContent: '', style: {}, addEventListener() {},
                    set innerHTML(value) { this.html = value; },
                    get innerHTML() {
                        return this.html ?? this.textContent.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    }
                };
            }
        },
        fetch: async url => {
            requests.push(url);
            return url.endsWith('/latest')
                ? { ok, redirected, json: async () => list }
                : { ok: true, json: async () => ({ unreadCount: 2 }) };
        },
        location: { pathname: '/' }, console: { debug() {} }, setTimeout() {}, Date
    };
    context.window = context;
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/notifications-realtime.js'), 'utf8'), context);
    init();
    return {
        requests, container,
        async open() {
            assert.ok(events.has('show.bs.dropdown'));
            events.get('show.bs.dropdown')();
            await new Promise(resolve => setImmediate(resolve));
        }
    };
}

for (const id of ['notifBellDropdown', 'floralNotifDropdown']) {
    test(`${id} loads customer notifications and links to their content`, async () => {
        const app = bellDropdown({ id, list: [
            { id: 7, title: '<script>private</script>', content: 'Delivery updated', isRead: false },
            { id: 8, title: 'Order ready', relatedOrderId: 42, isRead: true }
        ] });
        await app.open();
        assert.ok(app.requests.includes('/api/notifications/latest'));
        assert.equal(app.container.rows.length, 2);
        assert.equal(app.container.rows[0].href, '/notifications#notif_7');
        assert.ok(app.container.rows[0].innerHTML.includes('&lt;script&gt;'));
        assert.ok(app.container.rows[0].innerHTML.includes('Delivery updated'));
        assert.equal(app.container.rows[1].href, '/customer/orders/detail/42');
    });
}

test('empty bell list displays an explicit empty state', async () => {
    const app = bellDropdown();
    await app.open();
    assert.ok(app.container.innerHTML.includes('Không có thông báo mới nào'));
});

for (const response of [{ ok: false }, { redirected: true }, { list: { error: 'invalid response' } }]) {
    test(`failed bell list keeps a usable notification-page link: ${JSON.stringify(response)}`, async () => {
        const app = bellDropdown(response);
        await app.open();
        assert.ok(app.container.innerHTML.includes('href="/notifications"'));
        assert.ok(app.container.innerHTML.includes('Không thể tải thông báo'));
        assert.equal(app.container.rows.length, 0);
    });
}

test('bell dropdown uses server targetUrl when provided', async () => {
    const app = bellDropdown({ list: [
        { id: 12, title: 'Khách hàng bị khóa', targetUrl: '/admin/orders/6', isRead: false },
        { id: 13, title: 'Cảnh báo hệ thống', targetUrl: '/admin/notifications#notif_13', isRead: true }
    ] });
    await app.open();
    assert.equal(app.container.rows.length, 2);
    assert.equal(app.container.rows[0].href, '/admin/orders/6');
    assert.equal(app.container.rows[1].href, '/admin/notifications#notif_13');
});

test('bell dropdown rejects malicious or external targetUrl and falls back safely', async () => {
    const app = bellDropdown({ list: [
        { id: 14, title: 'Lừa đảo', targetUrl: 'https://evil.com/phish', relatedOrderId: 10, isRead: false },
        { id: 15, title: 'Protocol bypass', targetUrl: '//evil.com/orders', isRead: true }
    ] });
    await app.open();
    assert.equal(app.container.rows[0].href, '/customer/orders/detail/10');
    assert.equal(app.container.rows[1].href, '/notifications#notif_15');
});

test('bell dropdown rejects route traversal and notification lookalike paths', async () => {
    for (const targetUrl of ['/admin/orders/4/../../users', '/notifications-evil',
        '/admin/orders/%2e%2e/users', '/notifications" onclick="alert(1)', '/admin/orders/0']) {
        const app = bellDropdown({ list: [{ id: 16, title: 'Invalid route', targetUrl, isRead: true }] });
        await app.open();
        assert.equal(app.container.rows[0].href, '/notifications#notif_16');
    }
});
