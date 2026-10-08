const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function widget() {
    const nodes = new Map(), sockets = [], timers = new Map();
    let next = 0, denied = false, reads = 0;
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, {
            dataset: { csrfHeader: 'X-CSRF-TOKEN', csrfToken: 'token' }, hidden: true, disabled: true,
            value: '', files: [], children: [], events: {}, textContent: '',
            addEventListener(name, callback) { this.events[name] = callback; },
            setAttribute() {}, replaceChildren() {}, append() {}, focus() {}, querySelector() { return null; }
        });
        return nodes.get(id);
    };
    class Socket {
        static OPEN = 1; static CONNECTING = 0;
        constructor(url, protocols) { this.url = url; this.protocols = protocols; this.readyState = 0; sockets.push(this); }
        send(value) { this.lastFrame = value; }
        close() { this.readyState = 3; this.onclose?.(); }
        open() { this.readyState = 1; this.onopen?.(); }
        message(data) { this.onmessage?.({ data }); }
    }
    const context = {
        document: { getElementById: node, createElement: node, addEventListener() {} },
        window: { addEventListener() {} }, location: { protocol: 'http:', host: 'localhost:8080' },
        WebSocket: Socket,
        setTimeout(callback, delay) { const id = ++next; timers.set(id, { callback, delay }); return id; },
        clearTimeout(id) { timers.delete(id); }, setInterval() { return 99; }, clearInterval() {},
        fetch: async () => {
            reads++;
            return { ok: !denied, status: denied ? 403 : 200, redirected: false,
                json: async () => denied ? { message: 'Tài khoản đã bị khóa' } : { userId: 1, role: 'USER', stores: [], conversations: [], hasMore: false } };
        }
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/chat-widget.js'), 'utf8'), context);
    return { node, sockets, timers, deny() { denied = true; }, reads: () => reads };
}
const flush = () => new Promise(resolve => setImmediate(resolve));

test('connection timeout releases socket and allows reconnect', async () => {
    const app = widget(); await flush();
    assert.deepEqual(Array.from(app.sockets[0].protocols), ['v12.stomp']);
    const timeout = [...app.timers.values()].find(timer => timer.delay === 10000);
    assert.ok(timeout); timeout.callback();
    assert.equal(app.sockets[0].readyState, 3);
    assert.match(app.node('chatStatus').textContent, /10 giây/);
    assert.equal(app.node('chatSend').disabled, true);
    await app.node('chatReconnect').events.click();
    assert.equal(app.sockets.length, 2);
    app.sockets[0].message('CONNECTED\nversion:1.2\n\n\0');
    assert.match(app.node('chatStatus').textContent, /Đang kết nối/);
});

test('STOMP error closes connection, keeps draft and stops automatic retries', async () => {
    const app = widget(); await flush();
    app.node('chatText').value = 'Tin chưa gửi';
    const socket = app.sockets[0]; socket.open(); socket.message('CONNECTED\nversion:1.2\n\n\0'); await flush();
    socket.message('ERROR\nmessage:Denied\n\nAccess denied\0');
    assert.equal(socket.readyState, 3);
    assert.match(app.node('chatStatus').textContent, /Server từ chối/);
    assert.equal(app.node('chatText').value, 'Tin chưa gửi');
    assert.equal(app.node('chatSend').disabled, true);
    assert.equal(app.timers.size, 0);
});

test('security JSON message is displayed and locked session is not reconnected', async () => {
    const app = widget(); await flush(); app.deny();
    await app.node('chatLauncher').events.click();
    assert.match(app.node('chatStatus').textContent, /Tài khoản đã bị khóa/);
    assert.equal(app.sockets[0].readyState, 3);
    const count = app.sockets.length;
    await app.node('chatReconnect').events.click();
    assert.equal(app.sockets.length, count);
    assert.equal(app.timers.size, 0);
});
