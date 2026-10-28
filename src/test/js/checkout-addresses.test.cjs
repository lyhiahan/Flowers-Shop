const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const html = fs.readFileSync('src/main/resources/templates/customer/checkout.html', 'utf8');
const fill = html.match(/function fillAddress\(selectElem\) \{[\s\S]*?\n        \}/)[0];
const init = html.match(/const savedAddress = document.getElementById\('savedAddressSelect'\);[\s\S]*?fillAddress\(savedAddress\);/)[0];
function run(restore, value = '1') {
    const fields = Object.fromEntries(['recipientNameInput', 'recipientPhoneInput', 'recipientAddressInput'].map(id => [id, {value: 'Manual value'}]));
    const attrs = {'data-name': 'Recipient from address', 'data-phone': '0901234567', 'data-address': 'Saved delivery address'};
    fields.savedAddressSelect = {dataset: {restore}, value, selectedIndex: 0, options: [{value, getAttribute: key => attrs[key]}]};
    let updated = 0;
    const context = {document: {getElementById: id => fields[id]}, updateCheckoutTotal: () => updated++, updateDeliveryDirections() {}};
    vm.runInNewContext(fill + '\n' + init, context);
    return {fields, updated};
}
test('default saved address initializes name, phone and address together', () => {
    const {fields, updated} = run('false');
    assert.equal(fields.recipientNameInput.value, 'Recipient from address');
    assert.equal(fields.recipientPhoneInput.value, '0901234567');
    assert.equal(fields.recipientAddressInput.value, 'Saved delivery address');
    assert.equal(updated, 1);
});
for (const [restore, value] of [['true', '1'], ['false', '']]) {
    test('preserves restored or manually entered recipient fields: ' + restore + '/' + value, () => {
        const {fields, updated} = run(restore, value);
        assert.equal(fields.recipientNameInput.value, 'Manual value');
        assert.equal(fields.recipientPhoneInput.value, 'Manual value');
        assert.equal(fields.recipientAddressInput.value, 'Manual value');
        assert.equal(updated, 0);
    });
}
