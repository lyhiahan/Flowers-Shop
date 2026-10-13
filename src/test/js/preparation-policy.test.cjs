const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const templates = path.resolve(__dirname, '../../main/resources/templates');
for (const role of ['admin', 'manager']) {
    const html = fs.readFileSync(path.join(templates, role, 'order-detail.html'), 'utf8');
    const script = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)]
        .find(block => block[1].includes('function triggerStatusUpdateModal'))[1];
    for (const value of ['PREPARING', 'DELIVERING', 'CANCELLED', '']) {
        test(`${role}: preparation warning for ${value || 'no selection'}`, () => {
            let shown = 0, alerts = 0;
            const elements = {
                dropdownOrderStatus: { value, selectedIndex: 0, options: [{ text: value }] },
                hiddenStatusInput: { value: '' },
                selectedStatusText: { innerText: '' },
                preparationCancellationWarning: { hidden: true },
                cancellationReasonGroup: { hidden: true },
                cancellationReasonInput: { disabled: true, required: false, value: '' },
                confirmStatusModal: {},
            };
            const sandbox = {
                document: { getElementById: id => elements[id] },
                alert: () => alerts++,
                bootstrap: { Modal: class { show() { shown++; } } },
            };
            vm.createContext(sandbox);
            vm.runInContext(script, sandbox);
            sandbox.triggerStatusUpdateModal();
            assert.equal(shown, value ? 1 : 0);
            assert.equal(alerts, value ? 0 : 1);
            assert.equal(elements.preparationCancellationWarning.hidden, value !== 'PREPARING');
            assert.equal(elements.hiddenStatusInput.value, value);
            assert.equal(elements.cancellationReasonGroup.hidden, value !== 'CANCELLED');
            assert.equal(elements.cancellationReasonInput.required, value === 'CANCELLED');
            assert.equal(elements.cancellationReasonInput.disabled, value !== 'CANCELLED');
            elements.cancellationReasonInput.value = 'Lý do đã nhập';
            // Switching away must remove the warning rather than leaking it into another action.
            elements.dropdownOrderStatus.value = 'DELIVERING';
            sandbox.triggerStatusUpdateModal();
            assert.equal(elements.preparationCancellationWarning.hidden, true);
            assert.equal(elements.cancellationReasonGroup.hidden, true);
            assert.equal(elements.cancellationReasonInput.required, false);
            assert.equal(elements.cancellationReasonInput.disabled, true);
            assert.equal(elements.cancellationReasonInput.value, '');
        });
    }
}
