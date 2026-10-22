const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function drawer() {
    const values = new Set(), nodes = new Map(), keys = {}, windowEvents = {};
    function node(id) {
        if (!nodes.has(id)) nodes.set(id, {
            events: {}, attributes: {}, focused: false,
            addEventListener(name, callback) { this.events[name] = callback; },
            setAttribute(name, value) { this.attributes[name] = value; },
            focus() { this.focused = true; }
        });
        return nodes.get(id);
    }
    const context = {
        document: {
            readyState: 'complete',
            body: { classList: {
                add: name => values.add(name), remove: name => values.delete(name),
                contains: name => values.has(name)
            }, appendChild() {} },
            getElementById: node,
            addEventListener(name, callback) { keys[name] = callback; }
        },
        window: { innerWidth: 390, addEventListener(name, callback) { windowEvents[name] = callback; } }
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin-layout.js'), 'utf8'), context);
    return { node, keys, windowEvents, context, isOpen: () => values.has('admin-menu-open'),
        click: id => node(id).events.click({ stopPropagation() {} }) };
}

test('admin mobile menu opens, closes by Escape/backdrop and restores focus', () => {
    const app = drawer();
    app.click('adminMenuToggleBtn');
    assert.equal(app.isOpen(), true);
    assert.equal(app.node('adminMenuToggleBtn').attributes['aria-expanded'], 'true');
    assert.equal(app.node('adminMenuCloseBtn').focused, true);
    app.keys.keydown({ key: 'Escape' });
    assert.equal(app.isOpen(), false);
    assert.equal(app.node('adminMenuToggleBtn').focused, true);
    app.click('adminMenuToggleBtn');
    app.click('adminDrawerBackdrop');
    assert.equal(app.isOpen(), false);
    assert.equal(app.node('adminMenuToggleBtn').attributes['aria-expanded'], 'false');
});

test('resizing to desktop clears the mobile drawer state', () => {
    const app = drawer();
    app.click('adminMenuToggleBtn');
    app.context.window.innerWidth = 1024;
    app.windowEvents.resize();
    assert.equal(app.isOpen(), false);
    assert.equal(app.node('adminMenuToggleBtn').attributes['aria-expanded'], 'false');
});
