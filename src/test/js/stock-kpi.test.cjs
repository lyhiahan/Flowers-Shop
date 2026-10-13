const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/stock-kpi.js'), 'utf8');
const settle = () => new Promise(resolve => setImmediate(resolve));

function setup(fetcher, withCard = true, options = {}) {
    const value = {textContent: '10'}, status = {hidden: true, textContent: ''};
    const card = {dataset: {stockEndpoint: options.endpoint || '/manager/dashboard/stock?storeId=1',
        stockCatalog: options.catalog ? 'true' : undefined},
        querySelector(selector) {return selector === '[data-stock-value]' ? value : status;}};
    const document = {hidden: false, listeners: {}, querySelector() {return withCard ? card : null;},
        querySelectorAll() {return options.cards || [];},
        addEventListener(event, listener) {this.listeners[event] = listener;}};
    const window = {listeners: {}, addEventListener(event, listener) {this.listeners[event] = listener;}};
    const requests = [], timers = [];
    vm.runInNewContext(source, {document, window, AbortController,
        fetch(url, options) {requests.push({url, options}); return fetcher(url, options);},
        setTimeout(fn, delay) {return {fn, delay};}, clearTimeout() {},
        setInterval(fn, delay) {const timer = {fn, delay}; timers.push(timer); return timer;},
        clearInterval(timer) {timer.cleared = true;}});
    return {value, status, requests, document, window, timers};
}
const ok = quantity => Promise.resolve({ok: true, json: async () => ({currentStockQuantity: quantity})});

test('loads remaining units without caching and polls every five seconds', async () => {
    let quantity = 8;
    const ui = setup(() => ok(quantity));
    await settle();
    assert.equal(ui.value.textContent, '8');
    assert.equal(ui.requests[0].options.cache, 'no-store');
    assert.equal(ui.requests[0].options.credentials, 'same-origin');
    assert.equal(ui.timers[0].delay, 5000);
    quantity = 10;
    await ui.timers[0].fn();
    assert.equal(ui.value.textContent, '10');
});

test('failed HTTP or invalid stock keeps last confirmed quantity and shows stale warning', async () => {
    for (const response of [{ok: false}, {ok: true, json: async () => ({currentStockQuantity: null})},
        {ok: true, json: async () => ({currentStockQuantity: -1})},
        {ok: true, json: async () => ({currentStockQuantity: '8'})}]) {
        const ui = setup(async () => response);
        await settle();
        assert.equal(ui.value.textContent, '10');
        assert.equal(ui.status.hidden, false);
        assert.match(ui.status.textContent, /lần tải gần nhất/);
    }
});

test('network failure recovers on next refresh without replacing stock by zero', async () => {
    let failed = true;
    const ui = setup(() => failed ? Promise.reject(new Error('offline')) : ok(6));
    await settle();
    assert.equal(ui.value.textContent, '10');
    assert.equal(ui.status.hidden, false);
    failed = false;
    await ui.timers[0].fn();
    assert.equal(ui.value.textContent, '6');
    assert.equal(ui.status.hidden, true);
});

test('does not overlap requests or poll hidden pages; refreshes when page returns', async () => {
    let resolve;
    const ui = setup(() => new Promise(done => {resolve = done;}));
    await ui.timers[0].fn();
    assert.equal(ui.requests.length, 1);
    resolve({ok: true, json: async () => ({currentStockQuantity: 8})});
    await settle();
    ui.document.hidden = true;
    await ui.timers[0].fn();
    assert.equal(ui.requests.length, 1);
    ui.document.hidden = false;
    ui.document.listeners.visibilitychange();
    assert.equal(ui.requests.length, 2);
});

test('page exit stops polling and prevents a late response from changing the page', async () => {
    let resolve;
    const ui = setup(() => new Promise(done => {resolve = done;}));
    ui.window.listeners.pagehide();
    assert.equal(ui.timers[0].cleared, true);
    assert.equal(ui.requests[0].options.signal.aborted, true);
    resolve({ok: true, json: async () => ({currentStockQuantity: 8})});
    await settle();
    assert.equal(ui.value.textContent, '10');
    await ui.timers[0].fn();
    assert.equal(ui.requests.length, 1);
});

test('pages without stock card make no requests or timers', () => {
    const ui = setup(() => ok(8), false);
    assert.equal(ui.requests.length, 0);
    assert.equal(ui.timers.length, 0);
});

test('back-forward cache restoration resumes refresh and polling', async () => {
    const ui = setup(() => ok(8));
    await settle();
    ui.window.listeners.pagehide();
    ui.window.listeners.pageshow();
    await settle();
    assert.equal(ui.timers.length, 2);
    assert.equal(ui.requests.length, 2);
    assert.equal(ui.value.textContent, '8');
});

function productCard(id, withForm = true) {
    const stockText = {textContent: 'Còn 10 sản phẩm'}, stockIcon = {className: 'bi bi-box-seam'};
    const label = {outOfStock: false, classList: {toggle(name, enabled) {label.outOfStock = enabled;}},
        querySelector(selector) {return selector === 'span' ? stockText : stockIcon;}};
    const buttonText = {textContent: 'Thêm vào giỏ'}, buttonIcon = {className: 'bi bi-cart-plus'};
    const button = {disabled: false, querySelector(selector) {return selector === 'span' ? buttonText : buttonIcon;}};
    const store = {value: '1'};
    const form = {dataset: {}, querySelector(selector) {return selector === '[name="storeId"]' ? store : button;}};
    return {dataset: {catalogProductId: id}, label, stockText, stockIcon, form, store, button, buttonText,
        querySelector(selector) {return selector === '.catalog-stock' ? label : withForm ? form : null;}};
}
const snapshot = (quantity, products) => Promise.resolve({ok: true, json: async () => ({currentStockQuantity: quantity, products})});

test('customer KPI and duplicate featured/catalog cards update after purchase and cancellation', async () => {
    const featured = productCard('11', false), catalog = productCard('11');
    let quantity = 8;
    const ui = setup(() => snapshot(quantity, {11: {quantity, purchaseStoreId: 1}}), true,
        {catalog: true, endpoint: '/api/public/catalog-stock', cards: [featured, catalog]});
    await settle();
    assert.equal(ui.value.textContent, '8');
    assert.equal(featured.stockText.textContent, 'Còn 8 sản phẩm');
    assert.equal(catalog.stockText.textContent, 'Còn 8 sản phẩm');
    assert.equal(ui.requests[0].url, '/api/public/catalog-stock?productIds=11');
    quantity = 10;
    await ui.timers[0].fn();
    assert.equal(ui.value.textContent, '10');
    assert.equal(catalog.stockText.textContent, 'Còn 10 sản phẩm');
});

test('customer sold-out card disables buying; restock restores branch target and buy label', async () => {
    const product = productCard('12');
    let quantity = 0;
    const ui = setup(() => snapshot(quantity, {12: {quantity, purchaseStoreId: quantity ? 2 : null}}), true,
        {catalog: true, endpoint: '/api/public/catalog-stock?storeId=2', cards: [product]});
    await settle();
    assert.equal(product.stockText.textContent, 'Hết hàng');
    assert.equal(product.label.outOfStock, true);
    assert.equal(product.button.disabled, true);
    assert.equal(product.buttonText.textContent, 'Hết hàng');
    assert.equal(product.store.value, '');
    assert.equal(product.form.dataset.stockAvailable, 'false');
    assert.equal(ui.requests[0].url, '/api/public/catalog-stock?storeId=2&productIds=12');
    quantity = 7;
    await ui.timers[0].fn();
    assert.equal(product.stockText.textContent, 'Còn 7 sản phẩm');
    assert.equal(product.label.outOfStock, false);
    assert.equal(product.button.disabled, false);
    assert.equal(product.buttonText.textContent, 'Thêm vào giỏ');
    assert.equal(product.store.value, '2');
});

test('stock refresh never re-enables a pending cart submission', async () => {
    const product = productCard('11');
    product.form.dataset.pending = 'true';
    const ui = setup(() => snapshot(8, {11: {quantity: 8, purchaseStoreId: 1}}), true,
        {catalog: true, endpoint: '/api/public/catalog-stock', cards: [product]});
    await settle();
    assert.equal(product.button.disabled, true);
    delete product.form.dataset.pending;
    await ui.timers[0].fn();
    assert.equal(product.button.disabled, false);
});

test('invalid product snapshot leaves customer KPI and cards unchanged instead of partial updates', async () => {
    const product = productCard('11');
    for (const products of [{}, {11: {quantity: -1, purchaseStoreId: 1}},
        {11: {quantity: 8, purchaseStoreId: null}}]) {
        const ui = setup(() => snapshot(8, products), true,
            {catalog: true, endpoint: '/api/public/catalog-stock', cards: [product]});
        await settle();
        assert.equal(ui.value.textContent, '10');
        assert.equal(product.stockText.textContent, 'Còn 10 sản phẩm');
        assert.equal(ui.status.hidden, false);
    }
});

test('customer product labels update again after returning to a cached page', async () => {
    const product = productCard('11');
    let quantity = 10;
    const ui = setup(() => snapshot(quantity, {11: {quantity, purchaseStoreId: 1}}), true,
        {catalog: true, endpoint: '/api/public/catalog-stock', cards: [product]});
    await settle();
    ui.window.listeners.pagehide();
    quantity = 8;
    ui.window.listeners.pageshow();
    await settle();
    assert.equal(ui.value.textContent, '8');
    assert.equal(product.stockText.textContent, 'Còn 8 sản phẩm');
});
