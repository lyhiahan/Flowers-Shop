'use strict';
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {createRequire} = require('node:module');
const test = require('node:test');
const assert = require('node:assert/strict');

// Reuse the existing test harness without changing the production confirmation dialog.
const harness = path.resolve(__dirname, '../../src/test/js/global-modal.test.cjs');
const context = {require:createRequire(harness), module:{exports:{}}, __dirname:path.dirname(harness), console};
vm.runInNewContext(fs.readFileSync(harness, 'utf8') + '\nmodule.exports = {modalApp};', context);
const {modalApp} = context.module.exports;

for (const paid of ['false', 'true']) {
    test(`UC21 admin confirmation preserves paid=${paid} from the clicked submit button`, () => {
        const app = modalApp();
        let submitted;
        const button = {name:'paid', value:paid};
        const form = {
            getAttribute: name => name === 'onsubmit' ? "return confirm('Confirm withdrawal decision?')" : null,
            submit() { submitted = new URLSearchParams([['_csrf', 'test-only']]); },
            requestSubmit(submitter) {
                submitted = new URLSearchParams([['_csrf', 'test-only'], [submitter.name, submitter.value]]);
            }
        };
        app.document.fire('submit', {target:form, submitter:button, preventDefault(){}, stopImmediatePropagation(){}});
        app.node('gModalConfirmBtn').fire('click');
        assert.equal(submitted?.get('paid'), paid, 'The admin controller requires the clicked button decision.');
    });
}
