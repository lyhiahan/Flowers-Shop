const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const template = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/admin/products-manage.html'), 'utf8');
const start = template.indexOf('    function openDeleteProductModal(');
const end = template.indexOf('    function openProductConfirmModal(', start);
const source = template.slice(start, end);

function setup() {
    const elements = new Map();
    const shown = [];
    const context = {
        document: {getElementById(id) {
            if (!elements.has(id)) elements.set(id, {id, textContent: '', innerText: '', action: '', value: ''});
            return elements.get(id);
        }},
        window: {scrollY: 0, location: {pathname: '/admin/products-manage', search: '?page=1'}},
        sessionStorage: {setItem() {}},
        bootstrap: {Modal: class {
            static getInstance() {return null;}
            constructor(element) {this.element = element;}
            show() {shown.push(this.element.id);}
        }}
    };
    vm.createContext(context);
    vm.runInContext(source, context);
    return {open: context.openDeleteProductModal, elements, shown};
}

test('open orders show an explanation dialog without preparing a delete submission', () => {
    const ui = setup();
    ui.open('32', '<b>Hoa Cúc Tana</b>', 'Chi nhánh A', '1');
    assert.deepEqual(ui.shown, ['deleteProductBlockedModal']);
    assert.equal(ui.elements.get('deleteProductBlockedName').textContent, '<b>Hoa Cúc Tana</b>');
    assert.match(ui.elements.get('deleteProductBlockedMessage').textContent, /còn 1 đơn hàng đang xử lý/);
    assert.equal(ui.elements.has('deleteProductForm'), false);
});

test('unknown order count cannot enter the delete confirmation', () => {
    for (const count of ['-1', 'invalid', null]) {
        const ui = setup();
        ui.open('32', 'Hoa', 'A', count);
        assert.deepEqual(ui.shown, ['deleteProductBlockedModal']);
        assert.equal(ui.elements.has('deleteProductForm'), false);
    }
});

test('zero open orders keeps normal confirmation and return filters', () => {
    const ui = setup();
    ui.open('32', 'Hoa', 'A', '0');
    assert.deepEqual(ui.shown, ['deleteProductConfirmModal']);
    assert.equal(ui.elements.get('deleteProductForm').action, '/admin/products-manage/delete/32');
    assert.equal(ui.elements.get('deleteProductReturnUrl').value, '/admin/products-manage?page=1');
});
