const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function paymentPicker(includeWallet = true, balance = 100000) {
    const html = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/customer/checkout.html'), 'utf8');
    const start = html.indexOf('        function selectPayment(');
    const end = html.indexOf('        /*', start);
    assert.ok(start > 0 && end > start);
    const nodes = new Map();
    for (const id of ['pay_cod', 'pay_momo', 'pay_wallet', 'payOptionCod', 'payOptionMomo', 'payOptionWallet',
        'desc_pay_cod', 'desc_pay_momo', 'desc_pay_wallet', 'walletAvailabilityStatus', 'btnPlaceOrder']) {
        const classes = new Set();
        nodes.set(id, { checked: id === 'pay_cod', disabled: false, hidden: false, innerHTML: '',
            dataset: {walletBalance: String(balance)}, attributes: {},
            setAttribute(key, value) { this.attributes[key] = value; }, classList: {
            add: c => classes.add(c), remove: c => classes.delete(c), contains: c => classes.has(c),
            toggle(c, enabled) { if (enabled) classes.add(c); else classes.delete(c); }
        } });
    }
    if (!includeWallet) for (const id of ['pay_wallet', 'payOptionWallet', 'desc_pay_wallet']) nodes.delete(id);
    const context = { document: { getElementById: id => nodes.get(id) || null } };
    vm.runInNewContext(html.slice(start, end), context);
    context.updateWalletAvailability(100000, true);
    return { node: id => nodes.get(id), select: context.selectPayment,
        quote: context.updateWalletAvailability, update: context.updatePaymentStyle };
}

test('wallet selection has its own active state, balance and checkout button', () => {
    const app = paymentPicker();
    app.select('pay_wallet');
    assert.equal(app.node('payOptionWallet').classList.contains('active'), true);
    assert.equal(app.node('desc_pay_wallet').classList.contains('d-none'), false);
    assert.equal(app.node('payOptionCod').classList.contains('active'), false);
    assert.equal(app.node('payOptionMomo').classList.contains('active'), false);
    assert.match(app.node('btnPlaceOrder').innerHTML, /Thanh toán bằng ví/);
    assert.doesNotMatch(app.node('btnPlaceOrder').innerHTML, /demo/);
});

test('wallet balance stays visible when another payment method is selected', () => {
    const app = paymentPicker();
    app.select('pay_wallet'); app.select('pay_cod');
    assert.equal(app.node('payOptionWallet').classList.contains('active'), false);
    assert.equal(app.node('desc_pay_wallet').classList.contains('d-none'), false);
    assert.match(app.node('btnPlaceOrder').innerHTML, /Đặt hàng/);
    app.select('pay_momo');
    assert.equal(app.node('payOptionMomo').classList.contains('active'), true);
    assert.match(app.node('btnPlaceOrder').innerHTML, /Tiếp tục thanh toán/);
});

test('insufficient funds hide and disable the wallet choice without changing COD or MoMo', () => {
    const app = paymentPicker(true, 99999);
    assert.equal(app.node('pay_wallet').disabled, true);
    assert.equal(app.node('pay_wallet').hidden, true);
    assert.equal(app.node('payOptionWallet').classList.contains('is-unavailable'), true);
    assert.equal(app.node('payOptionWallet').attributes['aria-disabled'], 'true');
    assert.equal(app.node('walletAvailabilityStatus').textContent, 'Số dư ví không đủ');
    app.select('pay_wallet');
    assert.equal(app.node('pay_cod').checked, true);
    app.select('pay_momo'); app.select('pay_wallet');
    assert.equal(app.node('pay_momo').checked, true);
    assert.equal(app.node('btnPlaceOrder').disabled, false);
});

test('an exact balance enables the wallet but one additional dong disables it', () => {
    const app = paymentPicker();
    assert.equal(app.node('pay_wallet').disabled, false);
    assert.equal(app.node('pay_wallet').hidden, false);
    app.select('pay_wallet'); app.quote(100001, true);
    assert.equal(app.node('pay_wallet').checked, false);
    assert.equal(app.node('pay_wallet').disabled, true);
    assert.equal(app.node('pay_cod').checked, false);
    assert.equal(app.node('pay_momo').checked, false);
    assert.equal(app.node('btnPlaceOrder').disabled, true);
});

test('a lower server total re-enables the wallet without auto-selecting it', () => {
    const app = paymentPicker(true, 90000);
    app.quote(90000, true);
    assert.equal(app.node('pay_wallet').disabled, false);
    assert.equal(app.node('pay_wallet').checked, false);
    app.select('pay_wallet');
    assert.equal(app.node('pay_wallet').checked, true);
});

test('pending quotes temporarily lock a selected wallet and preserve it if still affordable', () => {
    const app = paymentPicker(); app.select('pay_wallet');
    app.quote(null, false);
    assert.equal(app.node('pay_wallet').disabled, true);
    assert.equal(app.node('pay_wallet').checked, true);
    assert.equal(app.node('btnPlaceOrder').disabled, true);
    app.quote(100000, true);
    assert.equal(app.node('pay_wallet').checked, true);
    assert.equal(app.node('pay_wallet').disabled, false);
    assert.equal(app.node('btnPlaceOrder').disabled, false);
});

test('wallet availability never overrides a pickup stock block', () => {
    const app = paymentPicker();
    app.node('btnPlaceOrder').dataset.pickupStockBlocked = 'true';
    app.select('pay_wallet'); app.quote(50000, true);
    assert.equal(app.node('btnPlaceOrder').disabled, true);
});

test('missing or invalid totals never enable wallet spending', () => {
    const app = paymentPicker();
    for (const total of [null, undefined, 'invalid', -1, 0, Infinity]) {
        app.quote(total, true);
        assert.equal(app.node('pay_wallet').disabled, true);
    }
});

test('checkout still works when demo-wallet controls are not rendered', () => {
    const app = paymentPicker(false);
    assert.doesNotThrow(app.update);
    app.select('pay_momo');
    assert.match(app.node('btnPlaceOrder').innerHTML, /Tiếp tục thanh toán/);
});
