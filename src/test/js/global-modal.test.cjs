const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function modalApp() {
    const nodes = new Map();
    class Element {
        constructor() {
            this.children = []; this.style = {}; this.listeners = new Map(); this.classes = new Set();
            this.classList = {
                add: (...values) => values.forEach(value => this.classes.add(value)),
                remove: (...values) => values.forEach(value => this.classes.delete(value)),
                contains: value => this.classes.has(value)
            };
        }
        set className(value) { this.classes = new Set(value.split(/\s+/)); }
        get className() { return [...this.classes].join(' '); }
        set innerHTML(value) {
            this.html = value;
            // Only materialize the fixed IDs needed by the shared component.
            for (const tag of value.matchAll(/<[a-z][^>]*\bid="([^"]+)"[^>]*>/g)) {
                const child = new Element(); child.id = tag[1];
                child.className = /class="([^"]+)"/.exec(tag[0])?.[1] || '';
                this.children.push(child);
            }
        }
        get innerHTML() { return this.html; }
        appendChild(child) {
            this.children.push(child);
            function register(element) {
                if (element.id) nodes.set(element.id, element);
                element.children.forEach(register);
            }
            register(child);
        }
        addEventListener(type, callback) {
            if (!this.listeners.has(type)) this.listeners.set(type, []);
            this.listeners.get(type).push(callback);
        }
        fire(type, event = {}) {
            for (const callback of this.listeners.get(type) || []) callback({ target: this, ...event });
        }
    }
    const document = new Element();
    Object.assign(document, {
        readyState: 'complete', head: new Element(), body: new Element(),
        createElement: () => new Element(), getElementById: id => nodes.get(id) || null
    });
    const context = vm.createContext({ document, console }); context.window = context;
    vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/global-modal.js'), 'utf8'), context);
    return { context, document, node: id => nodes.get(id) };
}

function confirmationForm(app, options = {}) {
    const attributes = new Map([['onsubmit', "return confirm('Confirm withdrawal decision?')"]]);
    const submissions = [];
    const form = {
        valid: true,
        getAttribute: name => attributes.get(name) ?? null,
        setAttribute: (name, value) => attributes.set(name, value),
        removeAttribute: name => attributes.delete(name),
        submit() { assert.fail('Direct submit loses the clicked button and bypasses validation.'); },
        requestSubmit(submitter) {
            if (!this.valid) return;
            if (options.onRequest) options.onRequest();
            if (!request(submitter)) return;
            // Inline onsubmit would run after the capture listener in a real browser.
            if (attributes.has('onsubmit') && !app.context.confirm('Unexpected second confirmation')) return;
            const payload = new URLSearchParams([['_csrf', 'test-only']]);
            if (submitter?.name && !submitter.disabled) payload.append(submitter.name, submitter.value);
            submissions.push(payload);
        }
    };
    function request(submitter) {
        let prevented = false;
        app.document.fire('submit', { target: form, submitter,
            preventDefault() { prevented = true; }, stopImmediatePropagation() {} });
        return !prevented;
    }
    return { form, submissions, request };
}

test('shared dialogs have explicit opaque backgrounds, padding and small-screen overflow protection', () => {
    const app = modalApp();
    const css = app.node('global-modal-styles').textContent;
    assert.match(css, /\.g-modal-box\s*\{[^}]*background:\s*#FFFFFF;/);
    assert.match(css, /\.g-modal-box\s*\{[^}]*max-height:\s*calc\(100dvh - 32px\);[^}]*overflow-y:\s*auto;/);
    assert.match(css, /\.g-modal-body\s*\{[^}]*padding:\s*22px 24px;/);
    assert.match(css, /\.g-modal-btn-confirm-primary\s*\{[^}]*background:\s*#355447;[^}]*color:\s*#FFFFFF;/);
    assert.match(css, /@media\s*\(max-width:\s*480px\)/);
    assert.match(app.node('gModalBackdrop').innerHTML, /role="dialog" aria-modal="true"/);
});

test('cancel closes confirmation without executing the action', () => {
    const app = modalApp(); let submitted = 0;
    app.context.showGlobalConfirmModal({ title: 'Confirm', message: 'Long message', onConfirm: () => submitted++ });
    assert.equal(app.node('gModalTitle').innerText, 'Confirm');
    assert.equal(app.node('gModalMessageBox').innerText, 'Long message');
    assert.ok(app.node('gModalBackdrop').classList.contains('show'));
    app.node('gModalCancelBtn').fire('click');
    assert.equal(submitted, 0);
    assert.equal(app.node('gModalBackdrop').classList.contains('show'), false);
});

test('confirm executes its callback once and alert resets the shared button state', () => {
    const app = modalApp(); let confirmed = 0, closed = 0;
    app.context.showGlobalConfirmModal({ isDanger: true, onConfirm: () => confirmed++ });
    app.node('gModalConfirmBtn').fire('click');
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(confirmed, 1);
    app.context.showGlobalAlertModal({ message: '<script>not executable</script>', onClose: () => closed++ });
    assert.equal(app.node('gModalMessageBox').innerText, '<script>not executable</script>');
    assert.equal(app.node('gModalCancelBtn').style.display, 'none');
    assert.ok(app.node('gModalConfirmBtn').classList.contains('g-modal-btn-confirm-primary'));
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(closed, 1);
    app.context.showGlobalConfirmModal({ message: 'Another action' });
    assert.equal(app.node('gModalCancelBtn').style.display, 'inline-flex');
});

test('Escape and clicking the backdrop dismiss without executing a destructive action', () => {
    const app = modalApp(); let confirmed = 0;
    for (const dismiss of [() => app.document.fire('keydown', { key: 'Escape' }),
        () => app.node('gModalBackdrop').fire('click')]) {
        app.context.showGlobalConfirmModal({ isDanger: true, onConfirm: () => confirmed++ });
        dismiss();
        assert.equal(app.node('gModalBackdrop').classList.contains('show'), false);
    }
    assert.equal(confirmed, 0);
});

test('form confirmations submit only after explicit confirmation', () => {
    const app = modalApp();
    const { form, submissions, request } = confirmationForm(app);
    assert.equal(request(), false);
    app.node('gModalCancelBtn').fire('click');
    assert.equal(submissions.length, 0);
    assert.equal(request(), false);
    app.node('gModalConfirmBtn').fire('click');
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(submissions.length, 1);
    assert.equal(submissions[0].get('_csrf'), 'test-only');
    assert.match(form.getAttribute('onsubmit'), /confirm\(/);
    assert.equal(app.node('gModalBackdrop').classList.contains('show'), false);
    // A later attempt must still require confirmation, even if navigation was prevented.
    assert.equal(request(), false);
    app.node('gModalCancelBtn').fire('click');
    assert.equal(submissions.length, 1);
});

for (const paid of ['false', 'true']) {
    test(`admin withdrawal confirmation preserves paid=${paid} and CSRF exactly once`, () => {
        const app = modalApp();
        const { submissions, request } = confirmationForm(app);
        const button = { name: 'paid', value: paid };
        request(button);
        app.node('gModalCancelBtn').fire('click');
        assert.equal(submissions.length, 0);
        request(button);
        app.node('gModalConfirmBtn').fire('click');
        app.node('gModalConfirmBtn').fire('click');
        assert.equal(submissions.length, 1);
        assert.deepEqual([...submissions[0]], [['_csrf', 'test-only'], ['paid', paid]]);
    });
}

test('confirmed form without a submitter does not invent a withdrawal decision', () => {
    const app = modalApp();
    const { submissions, request } = confirmationForm(app);
    request();
    app.node('gModalConfirmBtn').fire('click');
    assert.deepEqual([...submissions[0]], [['_csrf', 'test-only']]);
});

test('confirmation still validates the form and restores its handler when submission fails', () => {
    const app = modalApp();
    const { form, submissions, request } = confirmationForm(app);
    request({ name: 'paid', value: 'false' });
    form.valid = false;
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(submissions.length, 0);
    assert.match(form.getAttribute('onsubmit'), /confirm\(/);
    form.valid = true;
    assert.equal(request({ name: 'paid', value: 'true' }), false);
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(submissions[0].get('paid'), 'true');
});

test('confirmation restores onsubmit after a requestSubmit error', () => {
    const app = modalApp();
    const { form, request } = confirmationForm(app, { onRequest() { throw new Error('submitter removed'); } });
    request({ name: 'paid', value: 'false' });
    assert.throws(() => app.node('gModalConfirmBtn').fire('click'), /submitter removed/);
    assert.match(form.getAttribute('onsubmit'), /confirm\(/);
});

test('onclick confirmations also preserve the clicked submit button', () => {
    const app = modalApp();
    const { form, submissions } = confirmationForm(app);
    const button = { tagName: 'BUTTON', type: 'submit', name: 'paid', value: 'false', form,
        getAttribute: name => name === 'onclick' ? "return confirm('Reject withdrawal?')" : null,
        closest() { return this; } };
    app.document.fire('click', { target: button, preventDefault() {}, stopImmediatePropagation() {} });
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(submissions.length, 1);
    assert.equal(submissions[0].get('paid'), 'false');
    assert.match(form.getAttribute('onsubmit'), /confirm\(/);
});

test('wishlist logout uses the same styled dialog and preserves cancellation', () => {
    const app = modalApp(); let submitted = 0, prevented = 0;
    const html = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/customer/wishlist.html'), 'utf8');
    const start = html.indexOf('    function confirmWishlistLogout(');
    const end = html.indexOf('    // Client-side Category Filter', start);
    assert.ok(start >= 0 && end > start);
    vm.runInContext(html.slice(start, end), app.context);
    const event = { preventDefault() { prevented++; } }, form = { submit() { submitted++; } };
    app.context.confirmWishlistLogout(event, form);
    assert.equal(app.node('gModalCancelBtn').innerText, 'Ở lại');
    assert.equal(app.node('gModalConfirmBtn').innerText, 'Xác nhận');
    app.node('gModalCancelBtn').fire('click');
    assert.equal(submitted, 0);
    app.context.confirmWishlistLogout(event, form);
    app.node('gModalConfirmBtn').fire('click');
    assert.equal(submitted, 1);
    assert.equal(prevented, 2);
});
