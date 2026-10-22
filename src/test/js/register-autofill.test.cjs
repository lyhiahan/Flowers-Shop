const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/register-autofill.js'), 'utf8');

function setup(fresh = true) {
    const username = {value: 'saved-account'};
    const password = {value: 'saved-password'};
    const form = {
        dataset: {freshRegistration: String(fresh)},
        listeners: {},
        addEventListener(name, callback) {this.listeners[name] = callback;}
    };
    const document = {
        activeElement: null,
        listeners: {},
        getElementById(id) {
            return {registerForm: form, usernameInput: username, passwordInput: password}[id];
        },
        addEventListener(name, callback) {this.listeners[name] = callback;}
    };
    const window = {
        listeners: {},
        addEventListener(name, callback) {this.listeners[name] = callback;}
    };
    const timers = [];
    vm.runInNewContext(source, {document, window, setTimeout(callback) {timers.push(callback);}});
    return {username, password, form, document, window, timers};
}

test('fresh registration clears credentials saved by the browser', () => {
    const ui = setup();
    assert.equal(ui.username.value, '');
    assert.equal(ui.password.value, '');
    ui.username.value = 'late-autofill';
    ui.password.value = 'late-password';
    ui.window.listeners.load();
    for (const timer of ui.timers) timer();
    assert.equal(ui.username.value, '');
    assert.equal(ui.password.value, '');
});

test('non-fresh validation response does not run credential cleanup', () => {
    const ui = setup(false);
    assert.equal(ui.username.value, 'saved-account');
    assert.equal(ui.password.value, 'saved-password');
    assert.equal(ui.timers.length, 0);
});

test('delayed cleanup never erases typing after interaction', () => {
    for (const eventName of ['focusin', 'pointerdown', 'keydown', 'beforeinput']) {
        const ui = setup();
        ui.window.listeners.load();
        ui.form.listeners[eventName]();
        ui.username.value = 'my-new-account';
        ui.password.value = 'my-new-password';
        for (const timer of ui.timers) timer();
        assert.equal(ui.username.value, 'my-new-account');
        assert.equal(ui.password.value, 'my-new-password');
    }
});

test('returning through browser history preserves the draft', () => {
    const ui = setup();
    ui.username.value = 'draft-account';
    ui.password.value = 'draft-password';
    ui.window.listeners.pageshow({persisted: true});
    assert.equal(ui.username.value, 'draft-account');
    assert.equal(ui.password.value, 'draft-password');
});
