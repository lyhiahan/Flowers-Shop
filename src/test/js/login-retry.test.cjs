const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/login-retry.js'), 'utf8');

function setup(failed = true, attempted = 'customer@example.test') {
    const username = {value: 'lygiahan', dataset: {attemptedUsername: attempted}};
    const password = {value: 'browser-saved-password'};
    const document = {listeners: {}, querySelector() {return {dataset: {loginFailed: String(failed)}};},
        getElementById(id) {return id === 'usernameInput' ? username : password;},
        addEventListener(event, listener) {this.listeners[event] = listener;}};
    const window = {listeners: {}, addEventListener(event, listener) {this.listeners[event] = listener;}};
    const timers = [];
    vm.runInNewContext(source, {document, window, setTimeout(listener) {timers.push(listener);}});
    return {username, password, document, window, timers};
}

test('failed login restores submitted email and clears saved password', () => {
    const ui = setup();
    assert.equal(ui.username.value, 'customer@example.test');
    assert.equal(ui.password.value, '');
});

test('late browser autofill is replaced by the attempted identifier before interaction', () => {
    const ui = setup(true, 'wrong-user');
    ui.username.value = 'lygiahan'; ui.password.value = 'browser-saved-password';
    ui.window.listeners.load();
    for (const timer of ui.timers) timer();
    assert.equal(ui.username.value, 'wrong-user');
    assert.equal(ui.password.value, '');
});

test('retry restoration never overwrites typing or an explicitly selected demo account', () => {
    for (const event of ['pointerdown', 'keydown', 'beforeinput']) {
        const ui = setup();
        ui.document.listeners[event]();
        ui.username.value = 'new-user'; ui.password.value = 'new-password';
        for (const timer of ui.timers) timer();
        ui.window.listeners.pageshow();
        assert.equal(ui.username.value, 'new-user');
        assert.equal(ui.password.value, 'new-password');
    }
});

test('normal login page does not interfere with intentional browser autofill', () => {
    const ui = setup(false);
    assert.equal(ui.username.value, 'lygiahan');
    assert.equal(ui.password.value, 'browser-saved-password');
    assert.equal(ui.timers.length, 0);
});

test('empty failed identifier stays empty instead of falling back to another account', () => {
    const ui = setup(true, '');
    assert.equal(ui.username.value, '');
    assert.equal(ui.password.value, '');
});
