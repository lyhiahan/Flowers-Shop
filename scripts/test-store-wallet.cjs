// Contract checks for the withdrawal review and one-request submit flow.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const elements = {};
function element(id) {
    return elements[id] ||= {
        value: '', dataset: {}, hidden: false, disabled: false, textContent: '', listeners: {},
        addEventListener(event, handler) { this.listeners[event] = handler; },
        setCustomValidity(message) { this.error = message; }, focus() {},
        querySelector() { return element('closeButton'); }
    };
}
['formWithdraw', 'withdrawModal', 'inputWithdrawAmount', 'inputWithdrawPassword', 'inputAccountNumber', 'inputAccountHolder', 'inputBankName',
 'withdrawFields', 'withdrawReview', 'btnOpenConfirmWithdraw', 'btnConfirm', 'btnCancel', 'withdrawStep', 'displayConfirmAmount',
 'displayConfirmBank', 'displayConfirmAccount', 'displayConfirmHolder', 'btnCloseWithdraw'].forEach(element);
element('inputWithdrawAmount').value = '500000.12';
element('inputWithdrawAmount').dataset.balance = '1000000';
element('inputWithdrawPassword').value = 'demo-password';
element('inputAccountNumber').value = ' 1012345678 ';
element('inputAccountHolder').value = ' NGUYEN VAN A ';
element('inputBankName').selectedIndex = 0;
element('inputBankName').options = [{text: 'Vietcombank'}];
let valid = true;
const context = vm.createContext({document: {getElementById: element}, window: {StoreFormValidation: {validate: () => valid, reset() {}}}});
vm.runInContext(fs.readFileSync('src/main/resources/static/js/store-wallet.js', 'utf8'), context);
function emit(id, name) {
    const event = {prevented: false, preventDefault() { this.prevented = true; }};
    element(id).listeners[name](event);
    return event;
}
assert.equal(emit('formWithdraw', 'submit').prevented, true, 'Continue must not POST');
assert.equal(element('withdrawReview').hidden, false);
assert.equal(element('displayConfirmAccount').textContent, '1012345678');
assert.match(element('displayConfirmAmount').textContent, /500[.]000,12/);
emit('btnCancel', 'click');
assert.equal(element('withdrawReview').hidden, true);
assert.equal(element('inputWithdrawAmount').value, '500000.12', 'Back preserves draft');
valid = false;
assert.equal(emit('formWithdraw', 'submit').prevented, true, 'Invalid data cannot enter review');
assert.equal(element('withdrawReview').hidden, true);
valid = true;
emit('formWithdraw', 'submit');
valid = false;
assert.equal(emit('formWithdraw', 'submit').prevented, true, 'Confirm revalidates data');
valid = true;
emit('formWithdraw', 'submit');
assert.equal(emit('formWithdraw', 'submit').prevented, false, 'Explicit confirm permits one POST');
assert.equal(element('btnConfirm').disabled, true);
assert.equal(emit('formWithdraw', 'submit').prevented, true, 'Duplicate confirm is blocked');
assert.equal(emit('withdrawModal', 'hide.bs.modal').prevented, true, 'Pending request keeps dialog stable');
emit('withdrawModal', 'hidden.bs.modal');
assert.equal(element('inputWithdrawPassword').value, '', 'Closing clears password');
console.log('Wallet review, cancel, revalidation and duplicate-submit checks passed.');
