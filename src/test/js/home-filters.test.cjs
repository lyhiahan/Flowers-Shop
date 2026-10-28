const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/home-filters.js'), 'utf8');

function fixture(search) {
    const window = {location:{pathname:'/products', search, href:''}};
    const document = {getElementById:() => null, querySelectorAll:() => [], addEventListener() {}};
    vm.runInNewContext(source, {window, document, URLSearchParams, Set, setTimeout});
    return {window, params:() => new URL(window.location.href, 'http://localhost').searchParams};
}

test('sorting keeps the active query and branch, resets pagination and targets the catalog', () => {
    const ui = fixture('?keyword=rose&storeId=7&flowerColor=red&page=4');
    ui.window.applyCatalogSort('price_asc');
    assert.equal(ui.params().get('keyword'), 'rose');
    assert.equal(ui.params().get('storeId'), '7');
    assert.equal(ui.params().get('flowerColor'), 'red');
    assert.equal(ui.params().get('page'), '1');
    assert.equal(ui.params().get('sort'), 'price_asc');
    assert.ok(ui.window.location.href.endsWith('#product-catalog'));
});

test('removing rating preserves category and unrelated filters', () => {
    const ui = fixture('?minRating=4&categoryId=2&flowerColor=pink&page=3');
    ui.window.removeRatingFilter();
    assert.equal(ui.params().has('minRating'), false);
    assert.equal(ui.params().get('categoryId'), '2');
    assert.equal(ui.params().get('flowerColor'), 'pink');
    assert.equal(ui.params().get('page'), '1');
});

test('clearing filters removes branch and all drawer attributes while keeping keyword, category and sort', () => {
    const ui = fixture('?keyword=hoa&categoryId=2&storeId=7&flowerStyle=box&flowerType=rose&flowerColor=pink&flowerOccasion=birthday&minPrice=100&maxPrice=500&minRating=4&tab=toprated&sort=price_asc&page=3');
    ui.window.resetAttributeFiltersOnly();
    for (const key of ['storeId', 'flowerStyle', 'flowerType', 'flowerColor', 'flowerOccasion', 'minPrice', 'maxPrice', 'minRating', 'tab']) assert.equal(ui.params().has(key), false);
    assert.equal(ui.params().get('keyword'), 'hoa');
    assert.equal(ui.params().get('categoryId'), '2');
    assert.equal(ui.params().get('sort'), 'price_asc');
    assert.equal(ui.params().get('page'), '1');
});
