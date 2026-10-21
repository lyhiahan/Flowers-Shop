const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function picture({ url = '/images/product.jpg', complete = false, naturalWidth = 0 } = {}) {
    const listeners = new Map(), classes = new Set();
    return {
        url, complete, naturalWidth, alt: 'Rose bouquet', writes: 0,
        dataset: { fallbackSrc: '/images/flower-logo.png' },
        classList: { add: value => classes.add(value), contains: value => classes.has(value) },
        get src() { return this.url; },
        set src(value) { this.url = value; this.writes++; },
        getAttribute(name) { return name === 'src' ? this.url : null; },
        addEventListener(name, callback) { listeners.set(name, callback); },
        fail() { listeners.get('error')(); }
    };
}

function init(images, readyState = 'complete') {
    let ready;
    const document = {
        readyState,
        querySelectorAll(selector) { assert.equal(selector, '[data-cart-product-image]'); return images; },
        addEventListener(name, callback) { assert.equal(name, 'DOMContentLoaded'); ready = callback; }
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/cart-product-images.js'), 'utf8'), { document });
    return () => ready();
}

test('valid product photos are preserved', () => {
    const image = picture({ complete: true, naturalWidth: 800 });
    init([image]);
    assert.equal(image.src, '/images/product.jpg');
    assert.equal(image.alt, 'Rose bouquet');
    assert.equal(image.writes, 0);
});

test('failed remote images switch to a local fallback without retry loops', () => {
    const image = picture({ url: 'https://example.test/missing.jpg' });
    init([image]); image.fail(); image.fail();
    assert.equal(image.src, '/images/flower-logo.png');
    assert.equal(image.writes, 1);
    assert.ok(image.classList.contains('is-image-fallback'));
    assert.match(image.alt, /Rose bouquet/);
});

test('images that failed before script initialization also receive a fallback', () => {
    const image = picture({ complete: true, naturalWidth: 0 });
    init([image]);
    assert.equal(image.src, '/images/flower-logo.png');
});

test('missing-image placeholders are labelled without downloading themselves again', () => {
    const image = picture({ url: '/images/flower-logo.png' });
    init([image]); image.fail();
    assert.ok(image.classList.contains('is-image-fallback'));
    assert.equal(image.writes, 0);
});

test('each cart line handles its image independently after DOM ready', () => {
    const first = picture(), second = picture();
    const ready = init([first, second], 'loading'); ready();
    first.fail();
    assert.equal(first.src, '/images/flower-logo.png');
    assert.equal(second.src, '/images/product.jpg');
});

test('pages with no cart thumbnails are unaffected', () => {
    assert.doesNotThrow(() => init([]));
});
