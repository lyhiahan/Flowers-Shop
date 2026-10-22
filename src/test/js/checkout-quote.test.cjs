const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function checkout(fetch) {
    const nodes = new Map(), timers = new Map();
    let nextTimer = 0;
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, {
            dataset: { quoteUrl: '/checkout/quote' }, disabled: false, textContent: '', events: {},
            classList: { remove() {} },
            addEventListener(name, callback) { this.events[name] = callback; },
            querySelector() { return null; }
        });
        return nodes.get(id);
    };
    const context = {
        document: { getElementById: node, addEventListener() {}, querySelector() { return null; } },
        AbortController, Intl, FormData: class {}, fetch,
        setTimeout(callback) { const id = ++nextTimer; timers.set(id, callback); return id; },
        clearTimeout(id) { timers.delete(id); }
    };
    context.window = context;
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/checkout-quote.js'), 'utf8'), context);
    return { node, update: context.updateCheckoutTotal, run: () => [...timers.values()].at(-1)() };
}
const ok = total => ({ ok: true, redirected: false, json: async () => ({ subtotal: total, shippingFee: 0, discount: 0, total }) });

test('checkout remains clickable so missing-field validation can show an error while quote is pending', async () => {
    const app = checkout(async () => ok(150000));
    let prevented = false;
    app.node('checkoutForm').events.submit({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(app.node('btnPlaceOrder').disabled, false);
    await app.run();
    assert.equal(app.node('btnPlaceOrder').disabled, false);
    assert.match(app.node('displayGrandTotal').textContent, /150[.,]000/);
    prevented = false;
    app.node('checkoutForm').events.submit({ preventDefault() { prevented = true; } });
    assert.equal(prevented, false);
    assert.equal(app.node('btnPlaceOrder').disabled, true);
});

test('quote rejection and expired login keep checkout clickable so the error can be shown', async () => {
    for (const response of [
        { ok: false, json: async () => ({ message: 'Chi nhánh hết hàng' }) },
        { ok: true, redirected: true }
    ]) {
        const app = checkout(async () => response);
        app.update(); await app.run();
        assert.equal(app.node('btnPlaceOrder').disabled, false);
        assert.equal(app.node('displayGrandTotal').textContent, 'Chưa xác định');
        assert.ok(app.node('quoteStatus').textContent.length > 0);
    }
});

test('late quote response cannot overwrite the latest selection', async () => {
    let resolveFirst, calls = 0;
    const app = checkout(() => ++calls === 1 ? new Promise(resolve => { resolveFirst = resolve; }) : Promise.resolve(ok(200000)));
    app.update(); const first = app.run();
    app.update(); await app.run();
    resolveFirst(ok(100000)); await first;
    assert.match(app.node('displayGrandTotal').textContent, /200[.,]000/);
    assert.equal(app.node('btnPlaceOrder').disabled, false);
});

test('changing the explicit demo delivery zone requests a fresh server quote', async () => {
    let calls = 0;
    const app = checkout(async () => { calls++; return ok(175000); });
    app.node('checkoutForm').events.change({ target: { name: 'deliveryZone' } });
    assert.equal(app.node('btnPlaceOrder').disabled, false);
    await app.run();
    assert.equal(calls, 1);
    assert.equal(app.node('btnPlaceOrder').disabled, false);
});

test('quote response shows the carrier assigned by the server', async () => {
    const app = checkout(async () => ({
        ok: true, redirected: false,
        json: async () => ({
            subtotal: 150000, shippingFee: 25000, discount: 0, total: 175000,
            deliveryUnitName: 'Giao hàng tiêu chuẩn'
        })
    }));
    app.node('checkoutForm').querySelector = () => ({ checked: true });
    app.update();
    await app.run();
    assert.equal(app.node('assignedDeliveryName').textContent, 'Giao hàng tiêu chuẩn');
});

test('a missing-field error takes precedence over quote submission', async () => {
    const app = checkout(async () => ok(150000));
    app.update();
    await app.run();
    app.node('checkoutForm').events.submit({
        defaultPrevented: true,
        preventDefault() { throw new Error('Form validation already handled the error'); }
    });
    assert.equal(app.node('btnPlaceOrder').disabled, false);
});

test('a pickup stock failure remains blocked after a quote response', async () => {
    const app = checkout(async () => ok(150000));
    app.node('btnPlaceOrder').dataset.pickupStockBlocked = 'true';
    app.update();
    await app.run();
    assert.equal(app.node('btnPlaceOrder').disabled, true);
});
