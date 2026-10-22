const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

test('cart deletion treats branch names as text while keeping the icon and target item', () => {
    const html = fs.readFileSync('src/main/resources/templates/customer/cart.html', 'utf8');
    const fn = html.match(/function confirmDeleteCartItem\(itemId, productName, storeName\) \{[\s\S]*?\n        \}/)[0];
    const nodes = new Map();
    const document = {
        getElementById(id) {
            if (!nodes.has(id)) nodes.set(id, {replaceChildren(...children) {this.children = children;}});
            return nodes.get(id);
        },
        createElement: tag => ({tagName: tag}),
        createTextNode: value => ({nodeType: 3, textContent: value})
    };
    const context = {document, bootstrap: {Modal: {getInstance: () => ({show() {}})}}};
    vm.createContext(context); vm.runInContext(fn, context);
    const payload = '<img src=x onerror="alert(1)">';
    context.confirmDeleteCartItem(42, 'Mẫu hoa', payload);
    const label = document.getElementById('modalDeleteProductStore');
    assert.equal(label.children.length, 2);
    assert.equal(label.children[0].tagName, 'i');
    assert.equal(label.children[1].nodeType, 3);
    assert.equal(label.children[1].textContent, payload);
    assert.equal(label.innerHTML, undefined);
    assert.equal(document.getElementById('modalDeleteCartForm').action, '/cart/remove/42');
});
