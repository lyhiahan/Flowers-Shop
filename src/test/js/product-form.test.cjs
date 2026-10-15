const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/product-form.js'), 'utf8');

function form(regular = '120000', promotionValue = '') {
    const price = { value: regular, listeners: {}, addEventListener(name, fn) { this.listeners[name] = fn; } };
    const promotion = { value: promotionValue, listeners: {}, validity: '',
        addEventListener(name, fn) { this.listeners[name] = fn; }, setCustomValidity(message) { this.validity = message; } };
    vm.runInNewContext(source, { document: { getElementById(id) { return id === 'productPrice' ? price : promotion; } } });
    return { price, promotion };
}

test('blank promotion means no discount', () => assert.equal(form().promotion.validity, ''));
test('fractional dong, zero and excessive promotion are rejected', () => {
    for (const value of ['0.01', '0', '-1', '999.99', '120001']) assert.notEqual(form('120000', value).promotion.validity, '');
});
test('whole dong is accepted', () => {
    for (const value of ['1', '100000', '100000.00', '120000']) assert.equal(form('120000', value).promotion.validity, '');
});
test('changing regular price updates promotion validation', () => {
    const { price, promotion } = form('120000', '100000');
    price.value = '90000'; price.listeners.input(); assert.notEqual(promotion.validity, '');
    promotion.value = ''; promotion.listeners.input(); assert.equal(promotion.validity, '');
});
test('pages without product form remain unaffected', () => {
    assert.doesNotThrow(() => vm.runInNewContext(source, { document: { getElementById() { return null; } } }));
});
