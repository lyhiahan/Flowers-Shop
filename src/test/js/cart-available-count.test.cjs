const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function cart(rows) {
    const html = fs.readFileSync('src/main/resources/templates/customer/cart.html', 'utf8');
    const summary = html.match(/function updateSummary\(\) \{[\s\S]*?\n        \}/)[0];
    const nodes = new Map();
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, {innerText: '', style: {setProperty() {}, removeProperty() {}}, children: [], appendChild(child) { this.children.push(child); }});
        return nodes.get(id);
    };
    for (const row of rows) nodes.set('row_' + row.id, {getAttribute(name) {
        return {'data-quantity': row.quantity, 'data-subtotal': row.amount || 100, 'data-total': row.amount || 100, 'data-deposit': 0}[name];
    }});
    const document = {getElementById: node, createElement: () => ({}), querySelectorAll: () => rows.filter(row => !row.disabled).map(row => ({value: row.id, checked: row.checked !== false}))};
    const context = {document, formatVnd: value => String(value)};
    vm.createContext(context); vm.runInContext(summary, context);
    return {refresh: () => context.updateSummary(), node};
}

test('header excludes paused or out-of-stock rows and sums available quantities even when unchecked', () => {
    const rows = [{id: 1, quantity: 2}, {id: 2, quantity: 5, disabled: true}, {id: 3, quantity: 3, checked: false}];
    const view = cart(rows); view.refresh();
    assert.equal(view.node('cartRowCount').innerText, 2);
    assert.equal(view.node('cartTotalQty').innerText, 5);
    assert.equal(view.node('selectedCount').innerText, '1 mẫu (2 bó)');
    assert.equal(view.node('selectedGrandTotal').innerText, '100');
    rows[0].quantity = 4; view.refresh();
    assert.equal(view.node('cartTotalQty').innerText, 7);
    rows[0].disabled = true; view.refresh();
    assert.equal(view.node('cartRowCount').innerText, 1);
    assert.equal(view.node('cartTotalQty').innerText, 3);
});

for (const rows of [[], [{id: 1, quantity: 7, disabled: true}]]) {
    test('no available items: zero header counts and checkout disabled (' + rows.length + ' rows)', () => {
        const view = cart(rows); view.refresh();
        assert.equal(view.node('cartRowCount').innerText, 0);
        assert.equal(view.node('cartTotalQty').innerText, 0);
        assert.equal(view.node('selectAll').disabled, true);
        assert.equal(view.node('checkoutBtn').disabled, true);
    });
}
