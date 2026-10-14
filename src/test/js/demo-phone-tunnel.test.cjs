const {test} = require('node:test');
const assert = require('node:assert/strict');
const {EventEmitter} = require('node:events');
const {PassThrough} = require('node:stream');
const {createAutoTunnel, tunnelUrl} = require('../../../scripts/demo-phone-tunnel.cjs');

const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function until(condition) {
    for (let i = 0; i < 100; i++) { if (condition()) return; await pause(10); }
    assert.fail('Timed out waiting for tunnel state');
}
function fixture(options = {}) {
    const children = [], events = [], targets = [];
    const spawn = (binary, args, settings) => {
        assert.equal(binary, 'test-cloudflared');
        assert.equal(settings.windowsHide, true);
        assert.equal(settings.shell, false);
        const child = new EventEmitter();
        child.stdout = new PassThrough(); child.stderr = new PassThrough();
        child.exitCode = null; child.signalCode = null; child.killed = false;
        child.kill = () => { child.killed = true; child.signalCode = 'SIGTERM'; queueMicrotask(() => child.emit('close', null)); };
        children.push(child); targets.push(args.at(-1));
        return child;
    };
    const tunnel = createAutoTunnel({appPort:8080, cloudflared:'test-cloudflared', emit:value => events.push(value),
        spawn, probe:async () => true, retryDelayMs:20, healthIntervalMs:20, startupTimeoutMs:1000, ...options});
    return {tunnel, children, events, targets};
}

test('only a strict Cloudflare origin is parsed from logs', () => {
    assert.equal(tunnelUrl('| https://test-phone.trycloudflare.com |'), 'https://test-phone.trycloudflare.com');
    for (const line of ['https://test.trycloudflare.com.evil.test', 'https://test.trycloudflare.com/admin',
        'http://test.trycloudflare.com', 'https://test.trycloudflare.com:443', 'https://test.trycloudflare.com?x=1'])
        assert.equal(tunnelUrl(line), null);
});

test('owned random-port gateway is verified before announcing READY and is private', async t => {
    let healthy = false, probeId;
    const f = fixture({probe:async (url, id) => { probeId = id; return healthy; }});
    t.after(() => f.tunnel.stop());
    await f.tunnel.start();
    assert.match(f.targets[0], /^http:\/\/127\.0\.0\.1:\d+$/);
    assert.notEqual(f.targets[0], 'http://127.0.0.1:8080');
    const local = await fetch(f.targets[0]);
    const child = f.children[0];
    child.stderr.write('URL: https://first-phone.trycloudflare.com\n');
    await until(() => probeId);
    assert.equal(local.headers.get('x-demo-gateway-id'), probeId);
    assert.equal(f.events.some(e => e.state === 'READY'), false);
    assert.equal((await fetch(f.targets[0] + '/admin')).status, 404);
    assert.equal((await fetch(f.targets[0] + '/customer/orders')).status, 404);
    healthy = true;
    await until(() => f.events.at(-1).state === 'READY');
    assert.equal(f.events.at(-1).url, 'https://first-phone.trycloudflare.com');
    await f.tunnel.stop();
    assert.equal(child.killed, true);
    await assert.rejects(fetch(f.targets[0]));
});

test('expired tunnel immediately clears the old origin and reconnects with a new one', async t => {
    const f = fixture(); t.after(() => f.tunnel.stop());
    await f.tunnel.start();
    f.children[0].stderr.write('https://first-phone.trycloudflare.com\n');
    await until(() => f.events.at(-1).state === 'READY');
    f.children[0].stderr.write('Unauthorized: Tunnel not found\n');
    assert.equal(f.events.at(-1).url, null);
    assert.equal(f.children[0].killed, true);
    await until(() => f.children.length === 2);
    f.children[1].stderr.write('https://second-phone.trycloudflare.com\n');
    await until(() => f.events.at(-1).state === 'READY');
    assert.equal(f.events.at(-1).url, 'https://second-phone.trycloudflare.com');
    await f.tunnel.stop();
    await pause(50);
    assert.equal(f.children.length, 2);
});

test('failed health checks revoke QR readiness and clean up the failed child', async t => {
    let healthy = true;
    const f = fixture({probe:async () => healthy}); t.after(() => f.tunnel.stop());
    await f.tunnel.start();
    f.children[0].stderr.write('https://first-phone.trycloudflare.com\n');
    await until(() => f.events.at(-1).state === 'READY');
    healthy = false;
    await until(() => f.events.at(-1).state !== 'READY');
    assert.equal(f.events.at(-1).url, null);
    await until(() => f.children[0].killed);
});

test('a child with no advertised origin times out and retries without hanging the website', async t => {
    const f = fixture({startupTimeoutMs:30}); t.after(() => f.tunnel.stop());
    await f.tunnel.start();
    await until(() => f.children.length > 1);
    assert.equal(f.children[0].killed, true);
    assert.equal(f.events.some(e => e.state === 'READY'), false);
});
