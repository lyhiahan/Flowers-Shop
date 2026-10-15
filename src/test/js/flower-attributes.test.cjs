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
        addEventListener(k, fn) { this.listeners[k] = fn; }, focus() { this.focused = true; },
        setCustomValidity(v) { this.validity = v; }, reportValidity() { this.reported = true; }
    };
}
function setup(value = '') {
    const source = element('textarea'); source.id = 'flowerTypes'; source.name = 'flowerTypes'; source.value = value;
    source.dataset = { label: 'Loại hoa', suggestions: 'flowerTypes-suggestions' }; source.form = element('form');
    enhance(source, { createElement: element });
    const [tags, controls, error] = source.next.children;
    const [input, add] = controls.children;
    return { source, tags, input, add, error };
}
test('existing values hydrate and retain label, field name and suggestions', () => {
    const { source, tags, input } = setup('Hoa hồng, Baby');
    assert.equal(source.hidden, true); assert.equal(source.name, 'flowerTypes');
    assert.equal(input.id, 'flowerTypes'); assert.equal(tags.children.length, 2);
    assert.equal(input.attributes.list, 'flowerTypes-suggestions');
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
