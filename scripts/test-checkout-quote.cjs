// Regression tests for async checkout state, with built-in Node modules only.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function fixture() {
    const listeners = {}, pending = [], timers = [];
    const elements = Object.fromEntries(['btnPlaceOrder','quoteStatus','displaySubtotal','displayShippingFee','displayDiscount','displayGrandTotal'].map(id => [id, { textContent: '', disabled: false }]));
    const form = { dataset: { quoteUrl: '/checkout/quote' }, addEventListener: (event, callback) => { listeners[event] = callback; } };
    elements.checkoutForm = form;
    const context = vm.createContext({ Intl, AbortController,
        document: { getElementById: id => elements[id], addEventListener() {} },
        FormData: class {},
        fetch: () => new Promise(resolve => pending.push(resolve)),
        setTimeout: callback => { timers.push(callback); return timers.length; }, clearTimeout() {}
    });
    context.window = context;
    vm.runInContext(fs.readFileSync('src/main/resources/static/js/checkout-quote.js', 'utf8'), context);
    return { elements, listeners, pending, timers, update: context.updateCheckoutTotal };
}
const flush = () => new Promise(resolve => setImmediate(resolve));
const response = (total, ok = true) => ({ ok, redirected: false, json: async () => ({ subtotal: 160000, shippingFee: 30000, discount: 0, total, message: 'Voucher expired' }) });
(async () => {
    const f = fixture();
    f.update();
    assert.equal(f.elements.btnPlaceOrder.disabled, true);
    f.timers.shift()();
    f.pending.shift()(response(190000));
    await flush();
    assert.equal(f.elements.btnPlaceOrder.disabled, false);
    assert.match(f.elements.displayGrandTotal.textContent, /190\.000/);
    assert.match(f.elements.displaySubtotal.textContent, /160\.000/);
    f.update(); f.timers.shift()();
    const stale = f.pending.shift();
    f.update(); f.timers.shift()();
    f.pending.shift()(response(170000));
    await flush();
    stale(response(990000));
    await flush();
    assert.match(f.elements.displayGrandTotal.textContent, /170\.000/);
    f.update(); f.timers.shift()();
    f.pending.shift()(response(0, false));
    await flush();
    assert.equal(f.elements.btnPlaceOrder.disabled, true);
    assert.equal(f.elements.quoteStatus.textContent, 'Voucher expired');
    let prevented = false;
    f.listeners.submit({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    console.log('Checkout async checks passed: server amounts, pending state, stale responses, failed quote and blocked submit.');
})().catch(error => { console.error(error); process.exitCode = 1; });
