const { test } = require('node:test');
const assert = require('node:assert/strict');
const { parse, enhance } = require('../../main/resources/static/js/flower-attributes.js');

test('mixed bouquet supports arbitrary values, delimiters and Unicode normalization', () => {
    assert.deepEqual(parse(' Hoa  hồng, Cẩm tú cầu; Baby\nThanh liễu'), ['Hoa hồng', 'Cẩm tú cầu', 'Baby', 'Thanh liễu']);
    assert.deepEqual(parse('Hồng đào, hồng đào,' + 'Hồng đào'.normalize('NFD')), ['Hồng đào']);
});
test('attributes are optional and bounded without restricting the vocabulary', () => {
    assert.deepEqual(parse(' , ;\n'), []);
    assert.throws(() => parse('a'.repeat(33)), /32/);
    assert.throws(() => parse(Array.from({ length: 16 }, (_, i) => 'Hoa ' + i).join(',')), /15/);
    assert.throws(() => parse('a'.repeat(1501)), /quá dài/);
    assert.throws(() => parse('a\x00b'), /điều khiển/);
    assert.deepEqual(parse('Trắng kem; Phối bất đối xứng'), ['Trắng kem', 'Phối bất đối xứng']);
});

function element(tag) {
    return { tag, children: [], listeners: {}, attributes: {}, dataset: {}, value: '',
        append(...items) { this.children.push(...items); }, replaceChildren(...items) { this.children = items; },
        setAttribute(k, v) { this.attributes[k] = v; }, after(item) { this.next = item; },
        contains(item) { return this === item || this.children.some(child => child.contains(item)); },
        addEventListener(k, fn) { this.listeners[k] = fn; }, focus() { this.focused = true; },
        setCustomValidity(v) { this.validity = v; }, reportValidity() { this.reported = true; }
    };
}
function find(root, predicate) {
    if (predicate(root)) return root;
    for (const child of root.children) { const match = find(child, predicate); if (match) return match; }
}
function setup(value = '', suggestions = ['Hoa hồng', 'Baby', 'Cẩm tú cầu', 'Hồng đào']) {
    const source = element('textarea'); source.id = 'flowerTypes'; source.name = 'flowerTypes'; source.value = value;
    source.dataset = { label: 'Loại hoa', suggestions: 'flowerTypes-suggestions' }; source.form = element('form');
    const document = { createElement: element, listeners: {},
        addEventListener(k, fn) { this.listeners[k] = fn; },
        getElementById(id) { return id === 'flowerTypes-suggestions'
            ? {querySelectorAll: () => suggestions.map(value => ({value}))} : null; }
    };
    enhance(source, document);
    const byClass = name => find(source.next, item => item.className === name);
    return { source, document, tags: byClass('flower-tags'), input: byClass('flower-tag-input'),
        add: byClass('flower-tag-add'), error: byClass('flower-tag-error'),
        picker: byClass('flower-tag-picker'), choices: byClass('flower-tag-choices') };
}
test('existing values hydrate and retain label, field name and suggestions', () => {
    const { source, tags, input, picker, choices } = setup('Hoa hồng, Baby');
    assert.equal(source.hidden, true); assert.equal(source.name, 'flowerTypes');
    assert.equal(input.id, 'flowerTypes'); assert.equal(tags.children.length, 2);
    assert.equal(picker.attributes['aria-controls'], choices.id);
    picker.listeners.click();
    assert.deepEqual(choices.children.map(c => c.textContent), ['Hoa hồng', 'Baby', 'Cẩm tú cầu', 'Hồng đào']);
    assert.equal(input.attributes['aria-describedby'], 'flowerTypes-error');
});
test('Enter adds a tag rather than submitting the form, including unknown flower names', () => {
    const { source, input, tags } = setup(); let prevented = false;
    input.value = 'Thanh liễu'; input.listeners.keydown({ key: 'Enter', preventDefault() { prevented = true; } });
    assert.equal(prevented, true); assert.equal(source.value, 'Thanh liễu'); assert.equal(tags.children.length, 1);
});
test('IME confirmation does not prematurely add a tag', () => {
    const { source, input } = setup(); input.value = 'Hồng';
    input.listeners.keydown({ key: 'Enter', isComposing: true, preventDefault() { assert.fail('IME intercepted'); } });
    assert.equal(source.value, ''); assert.equal(input.value, 'Hồng');
});
test('removing a tag updates the submitted field and allows clearing all', () => {
    const { source, tags, input } = setup('Baby'); tags.children[0].children[1].listeners.click();
    assert.equal(source.value, ''); assert.equal(tags.children.length, 0); assert.equal(input.focused, true);
});
test('add button and blur commit pending input without duplicates', () => {
    const { source, input, add } = setup('Baby'); input.value = 'baby, Hoa hồng'; add.listeners.click();
    input.value = 'Thanh liễu'; input.listeners.blur();
    assert.equal(source.value, 'Baby\nHoa hồng\nThanh liễu');
});
test('submit includes the last unfinished value', () => {
    const { source, input } = setup(); input.value = 'Hồng đào';
    source.form.listeners.submit({ preventDefault() { assert.fail('valid input'); } });
    assert.equal(source.value, 'Hồng đào');
});
test('invalid pending value blocks submit without erasing previous tags', () => {
    const { source, input, error } = setup('Baby'); input.value = 'a'.repeat(33); let prevented = false;
    source.form.listeners.submit({ preventDefault() { prevented = true; }, stopImmediatePropagation() {} });
    assert.equal(prevented, true); assert.equal(source.value, 'Baby'); assert.match(error.textContent, /32/);
});
test('tag labels are text, not executable HTML', () => {
    const { tags } = setup('<img src=x onerror=alert(1)>');
    assert.equal(tags.children[0].children[0].textContent, '<img src=x onerror=alert(1)>');
    assert.equal(tags.children[0].children[0].innerHTML, undefined);
});

test('existing suggestions toggle without duplicating hydrated values', () => {
    const {source, picker, choices} = setup('Baby');
    picker.listeners.click();
    assert.equal(choices.children[1].attributes['aria-pressed'], 'true');
    choices.children[1].listeners.click();
    assert.equal(source.value, '');
    choices.children[1].listeners.click();
    assert.equal(source.value, 'Baby');
});
test('suggestions filter without accents and allow an unknown custom value', () => {
    const {source, picker, choices, input, add} = setup();
    picker.listeners.click(); input.value = 'hong dao'; input.listeners.input();
    assert.deepEqual(choices.children.map(c => c.textContent), ['Hồng đào']);
    input.value = 'Hoa thanh anh'; input.listeners.input();
    assert.equal(choices.children[0].textContent, 'Không có kết quả phù hợp.');
    add.listeners.click(); assert.equal(source.value, 'Hoa thanh anh');
});
test('clicking picker does not commit the search query as an attribute', () => {
    const {source, input, picker} = setup('Baby'); input.value = 'hong';
    input.listeners.blur({relatedTarget: picker}); picker.listeners.click();
    assert.equal(source.value, 'Baby'); assert.equal(input.value, 'hong');
});
test('Escape and outside click close suggestions without losing chosen values', () => {
    const {source, picker, choices, document} = setup('Baby');
    picker.listeners.click(); let prevented = false;
    source.next.listeners.keydown({key: 'Escape', preventDefault() {prevented = true;}});
    assert.equal(prevented, true); assert.equal(choices.hidden, true); assert.equal(picker.focused, true);
    picker.listeners.click(); document.listeners.click({target: element('div')});
    assert.equal(choices.hidden, true); assert.equal(source.value, 'Baby');
});
test('legacy text outside current bounds remains editable instead of being discarded', () => {
    const value = 'a'.repeat(40); const source = element('textarea'); source.value = value;
    enhance(source, {createElement: element});
    assert.equal(source.hidden, undefined); assert.equal(source.value, value); assert.equal(source.next, undefined);
});
test('empty suggestions still allow free entry and saved values rehydrate', () => {
    const first = setup('Hồng', []); first.picker.listeners.click();
    assert.equal(first.choices.children[0].textContent, 'Chưa có giá trị có sẵn.');
    first.input.value = 'Xanh ngọc'; first.add.listeners.click();
    const reopened = setup(first.source.value, []);
    assert.deepEqual(reopened.tags.children.map(tag => tag.children[0].textContent), ['Hồng', 'Xanh ngọc']);
});
