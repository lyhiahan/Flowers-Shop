const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/home-filters.js'), 'utf8');

function selectBranch(value) {
    const location = { pathname: '/products', search: '?keyword=hoa&categoryId=2&flowerColor=Đỏ&sort=price_asc&page=3&storeId=1', href: '' };
    const context = { window: { location }, document: { getElementById: () => null, querySelectorAll: () => [] }, URLSearchParams, Set, setTimeout };
    vm.runInNewContext(source, context);
    context.window.applyCatalogStore(value);
    return new URL(location.href, 'http://localhost');
}

test('changing branch preserves search filters and sorting, resetting pagination', () => {
    const result = selectBranch('7');
    assert.equal(result.searchParams.get('storeId'), '7');
    assert.equal(result.searchParams.get('keyword'), 'hoa');
    assert.equal(result.searchParams.get('categoryId'), '2');
    assert.equal(result.searchParams.get('flowerColor'), 'Đỏ');
    assert.equal(result.searchParams.get('sort'), 'price_asc');
    assert.equal(result.searchParams.get('page'), '1');
    assert.equal(result.hash, '#product-catalog');
});

test('all branches removes only the branch selection and resets pagination', () => {
    const result = selectBranch('');
    assert.equal(result.searchParams.has('storeId'), false);
    assert.equal(result.searchParams.get('keyword'), 'hoa');
    assert.equal(result.searchParams.get('flowerColor'), 'Đỏ');
    assert.equal(result.searchParams.get('sort'), 'price_asc');
    assert.equal(result.searchParams.get('page'), '1');
});

function drawer() {
    const listeners = {};
    const reset = {}, apply = {};
    const chips = ['', '1', '2'].map(id => {
        const classes = new Set();
        const attrs = { 'data-store-id': id };
        return {
            getAttribute: key => attrs[key] ?? null,
            hasAttribute: key => key in attrs,
            setAttribute: (key, value) => { attrs[key] = value; },
            classList: {
                toggle(key, enabled) { if (enabled) classes.add(key); else classes.delete(key); },
                contains: key => classes.has(key)
            }
        };
    });
    const panel = {
        addEventListener: (name, handler) => { listeners[name] = handler; },
        querySelectorAll: selector => selector.includes('[data-store-id]') ? chips : []
    };
    reset.addEventListener = (_, handler) => { reset.click = handler; };
    apply.addEventListener = (_, handler) => { apply.click = handler; };
    const nodes = { allFiltersOffcanvas: panel, btnDrawerReset: reset, btnDrawerApply: apply };
    const location = { pathname: '/products', search: '?keyword=hoa&storeId=1&page=3&sort=price_asc&categoryId=2', href: '' };
    const context = {
        window: { location }, URLSearchParams, Set, setTimeout,
        document: { getElementById: id => nodes[id] || null, querySelectorAll: () => [] },
        bootstrap: { Offcanvas: { getInstance: () => ({ hide: () => listeners['hidden.bs.offcanvas']() }) } }
    };
    vm.runInNewContext(source, context);
    return {
        location, chips, reset: () => reset.click(), apply: () => apply.click(),
        open: () => listeners['show.bs.offcanvas'](), close: () => listeners['hidden.bs.offcanvas'](),
        choose(id) { listeners.click({ target: { closest: () => chips.find(chip => chip.getAttribute('data-store-id') === id) }, preventDefault() {} }); }
    };
}

test('drawer changes branch only after Apply, preserving keyword and other query settings', () => {
    const app = drawer();
    app.open(); app.choose('2');
    assert.equal(app.location.href, '');
    assert.equal(app.chips[2].getAttribute('aria-pressed'), 'true');
    assert.equal(app.chips[1].classList.contains('active'), false);
    app.apply();
    const url = new URL(app.location.href, 'http://localhost');
    assert.equal(url.searchParams.get('storeId'), '2');
    assert.equal(url.searchParams.get('page'), '1');
    assert.equal(url.searchParams.get('keyword'), 'hoa');
    assert.equal(url.searchParams.get('categoryId'), '2');
    assert.equal(url.searchParams.get('sort'), 'price_asc');
});

test('closing the drawer discards an unapplied branch selection', () => {
    const app = drawer();
    app.open(); app.choose('2'); app.close(); app.open();
    assert.equal(app.chips[1].getAttribute('aria-pressed'), 'true');
    assert.equal(app.chips[2].getAttribute('aria-pressed'), 'false');
    assert.equal(app.location.href, '');
});

test('Reset clears the branch draft and Apply returns to all branches', () => {
    const app = drawer();
    app.open(); app.choose('2'); app.reset();
    assert.equal(app.chips[0].getAttribute('aria-pressed'), 'true');
    assert.equal(app.location.href, '');
    app.apply();
    const url = new URL(app.location.href, 'http://localhost');
    assert.equal(url.searchParams.has('storeId'), false);
    assert.equal(url.searchParams.get('keyword'), 'hoa');
});

test('all-branches chip is exclusive and also waits for Apply', () => {
    const app = drawer();
    app.open(); app.choose('');
    assert.equal(app.location.href, '');
    assert.deepEqual(app.chips.map(chip => chip.getAttribute('aria-pressed')), ['true', 'false', 'false']);
    app.apply();
    assert.equal(new URL(app.location.href, 'http://localhost').searchParams.has('storeId'), false);
});
