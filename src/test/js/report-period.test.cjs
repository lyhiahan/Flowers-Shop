const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/report-period.js'), 'utf8');

function setup(period = '7days') {
    let submissions = 0;
    function field(value) {
        return {value, listeners: {}, addEventListener(event, listener) { this.listeners[event] = listener; },
            setCustomValidity(message) { this.error = message; }, focus() { this.focused = true; }};
    }
    const select = field(period);
    select.form = {requestSubmit() { submissions++; }};
    const start = field('2026-10-01'), end = field('2026-10-12');
    const custom = [{}, {}, {}];
    const controls = {querySelector(selector) { return {'[data-report-period]': select, '[data-report-start]': start, '[data-report-end]': end}[selector]; },
        querySelectorAll() { return custom; }};
    vm.runInNewContext(source, {document: {querySelectorAll() { return [controls]; }}});
    return {select, start, end, custom, submissions: () => submissions};
}

test('presets disable date fields and submit immediately', () => {
    const ui = setup('custom');
    ui.select.value = 'today'; ui.select.listeners.change();
    assert.equal(ui.submissions(), 1);
    assert.equal(ui.start.disabled, true);
    assert.equal(ui.end.required, false);
    assert.ok(ui.custom.every(field => field.hidden));
});
test('custom selection reveals required dates without premature submission', () => {
    const ui = setup();
    ui.select.value = 'custom'; ui.select.listeners.change();
    assert.equal(ui.submissions(), 0);
    assert.equal(ui.start.disabled, false);
    assert.equal(ui.end.required, true);
    assert.equal(ui.start.focused, true);
    assert.ok(ui.custom.every(field => !field.hidden));
});
test('inverted dates fail validation and correcting them clears the error', () => {
    const ui = setup('custom');
    ui.start.value = '2026-10-13'; ui.start.listeners.input();
    assert.ok(ui.end.error);
    ui.end.value = '2026-10-13'; ui.end.listeners.input();
    assert.equal(ui.end.error, '');
});
test('leaving an invalid custom range clears its error for presets', () => {
    const ui = setup('custom');
    ui.end.value = '2026-09-01'; ui.end.listeners.input();
    ui.select.value = '30days'; ui.select.listeners.change();
    assert.equal(ui.end.error, '');
    assert.equal(ui.submissions(), 1);
});
