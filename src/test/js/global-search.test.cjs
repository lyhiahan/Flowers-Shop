const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/global-search.js'), 'utf8');

function search(pathname = '/', present = true) {
    const input = { value: '' };
    const scope = { value: 'products' };
    let submit;
    const form = { querySelector: () => input, addEventListener: (_, fn) => { submit = fn; } };
    const location = { pathname, href: '' };
    vm.runInNewContext(source, { document: { getElementById: id => present ?
        (id === 'globalSearchForm' ? form : scope) : null }, window: { location } });
    return { scope, location, submit(value) {
        input.value = value;
        let prevented = false;
        submit({ preventDefault() { prevented = true; } });
        return { prevented, keyword: input.value };
    } };
}

test('product search uses the existing GET form and trims the keyword', () => {
    const app = search();
    assert.deepEqual(app.submit('  hoa hong  '), { prevented: false, keyword: 'hoa hong' });
    assert.equal(app.location.href, '');
});

for (const scope of ['stores', 'users']) {
    test(`${scope} search encodes the keyword without changing scope`, () => {
        const app = search();
        app.scope.value = scope;
        assert.equal(app.submit('  Hoa & Lan +  ').prevented, true);
        assert.equal(app.location.href, `/${scope}?keyword=Hoa%20%26%20Lan%20%2B`);
    });
    test(`${scope} search supports an empty keyword`, () => {
        const app = search();
        app.scope.value = scope;
        app.submit('   ');
        assert.equal(app.location.href, `/${scope}`);
    });
    test(`${scope} results and detail pages retain the matching search scope`, () => {
        assert.equal(search(`/${scope}`).scope.value, scope);
        assert.equal(search(`/${scope}/12`).scope.value, scope);
    });
}

test('unrelated pages do not accidentally select a different scope', () => {
    for (const page of ['/customer/orders', '/products', '/stores-other', '/users-other']) {
        assert.equal(search(page).scope.value, 'products');
    }
    assert.doesNotThrow(() => search('/', false));
});

test('home and shared header use one search fragment without local style overrides', () => {
    const index = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/index.html'), 'utf8');
    assert.match(index, /fragments\/home-header :: header/);
    const home = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/fragments/home-header.html'), 'utf8');
    const shared = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/fragments/floral.html'), 'utf8');
    assert.match(home, /th:replace="~\{fragments\/floral :: search\}"/);
    assert.match(shared, /th:replace="~\{fragments\/floral :: search\}"/);
    assert.equal((shared.match(/id="searchScopeSelect"/g) || []).length, 1);
    assert.match(shared, /value="products" selected>🌸 Mẫu hoa<\/option>/);
    assert.match(shared, /value="stores">🏬 Cửa hàng<\/option>/);
    assert.match(shared, /value="users">👥 Người dùng<\/option>/);
    assert.match(shared, /placeholder="Tìm hoa hồng, cửa hàng hoặc người dùng\.\.\."/);
    assert.doesNotMatch(home, /\.header-search-(?:type|box|input|btn)\s*\{/);
    assert.doesNotMatch(home + shared, /scopeSel\.style\.width|this\.style\.width/);
});

test('search scope uses the shared outer focus indicator without an inner frame', () => {
    const css = fs.readFileSync(path.join(__dirname, '../../main/resources/static/css/floral-theme.css'), 'utf8');
    const focus = css.match(/\.header-search-type:focus-visible\s*\{([^}]+)\}/);
    assert.ok(focus);
    assert.match(focus[1], /outline:\s*none\s*!important/);
    assert.match(focus[1], /box-shadow:\s*none\s*!important/);
    assert.match(css, /\.header-search-box:focus-within\s*\{[^}]*border-color:[^}]*box-shadow:/);
});

test('checkout retains balance and availability without the removed short note', () => {
    const checkout = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/customer/checkout.html'), 'utf8');
    assert.doesNotMatch(checkout, /Số dư mô phỏng, không phải tiền thật\./);
    assert.match(checkout, /id="checkoutWalletBalance"/);
    assert.match(checkout, /id="walletAvailabilityStatus"/);
    const wallet = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/customer/wallet.html'), 'utf8');
    assert.doesNotMatch(wallet, /Số dư mô phỏng, không phải tiền thật\./);
    assert.match(wallet, /Không hỗ trợ nạp, rút hoặc chuyển tiền/);
});
