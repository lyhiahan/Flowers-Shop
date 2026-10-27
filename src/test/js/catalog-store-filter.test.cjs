const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const template = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/index.html'), 'utf8');
const start = template.indexOf('window.applyCatalogStore = function');
const end = template.indexOf('window.applyCatalogSort = function', start);

function selectBranch(value) {
    const location = { pathname: '/products', search: '?keyword=hoa&categoryId=2&flowerColor=Đỏ&sort=price_asc&page=3&storeId=1', href: '' };
    const context = { window: { location }, getQueryParams: () => new URLSearchParams(location.search) };
    vm.runInNewContext(template.slice(start, end), context);
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
