const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/home-cart.js'), 'utf8');
const settle = () => new Promise(resolve => setImmediate(resolve));

function fixture(fetcher, {count = 0, withBadge = true} = {}) {
    const badge = {textContent:String(count), style:{display:count ? '' : 'none'}, setAttribute() {}};
    const notification = {textContent:'7', style:{display:''}};
    const following = {children:[]};
    const cart = {badge:withBadge ? badge : null, attributes:{},
        querySelector() {return this.badge;},
        appendChild(node) {this.badge = node;},
        setAttribute(key,value) {this.attributes[key] = value;}};
    const button = {disabled:false, innerHTML:'Add to cart', style:{}};
    const error = {textContent:'', style:{display:'none'}};
    const form = {action:'/cart/add', dataset:{}, querySelector(selector) {
        return selector === 'button[type="submit"]' ? button : error;
    }};
    const requests = [], timers = [], window = {location:{href:'/'}};
    const document = {listeners:{},
        getElementById(id) {return id === 'headerCartLink' ? cart : null;},
        querySelector(selector) {
            if (selector === '.cart-badge-pill') return notification;
            if (selector === '.header-cart-link') return following;
            return {content:selector.includes('_csrf_header') ? 'X-CSRF-TOKEN' : 'test-csrf'};
        },
        createElement() {return {style:{}, setAttribute() {}};},
        addEventListener(name, fn) {this.listeners[name] = fn;}};
    vm.runInNewContext(source, {document,window,
        FormData:class {constructor(form) {this.form=form;}},
        fetch(url, options) {requests.push({url,options}); return fetcher(url,options);},
        setTimeout(fn) {timers.push(fn);}});
    const submit = () => document.listeners.submit({target:{closest:() => form}, preventDefault() {}});
    return {cart,notification,following,button,error,form,requests,timers,window,submit};
}
const response = data => Promise.resolve({status:200, json:async () => data});

test('first add shows the cart count without changing following or notifications', async () => {
    const ui = fixture(() => response({success:true,count:1}));
    ui.submit(); await settle();
    assert.equal(Number(ui.cart.badge.textContent),1);
    assert.equal(ui.cart.badge.style.display,'');
    assert.equal(ui.cart.attributes['aria-label'],'Giỏ hàng, 1 sản phẩm');
    assert.equal(ui.notification.textContent,'7');
    assert.equal(ui.notification.style.display,'');
    assert.deepEqual(ui.following.children,[]);
    assert.equal(ui.requests[0].options.headers['X-CSRF-TOKEN'],'test-csrf');
    assert.equal(ui.requests[0].options.credentials,'same-origin');
    assert.equal(ui.button.disabled,false);
});

test('uses the confirmed total from the server for subsequent additions', async () => {
    let count=3;
    const ui=fixture(() => response({success:true,count}), {count:2});
    ui.submit(); await settle();
    assert.equal(Number(ui.cart.badge.textContent),3);
    count=8;
    ui.submit(); await settle();
    assert.equal(Number(ui.cart.badge.textContent),8);
    assert.equal(ui.notification.textContent,'7');
});

test('creates a missing badge only inside the cart link', async () => {
    const ui=fixture(() => response({success:true,count:2}), {withBadge:false});
    ui.submit(); await settle();
    assert.equal(ui.cart.badge.id,'headerCartBadge');
    assert.equal(Number(ui.cart.badge.textContent),2);
    assert.equal(ui.notification.textContent,'7');
});

test('failed additions retain the confirmed count and show the error', async () => {
    for (const fetcher of [() => response({success:false,error:'Out of stock'}),
        () => Promise.reject(new Error('Offline'))]) {
        const ui=fixture(fetcher,{count:2});
        ui.submit(); await settle();
        assert.equal(ui.cart.badge.textContent,'2');
        assert.equal(ui.error.style.display,'block');
        assert.equal(ui.button.disabled,false);
        assert.equal(ui.form.dataset.pending,undefined);
    }
});

test('blocks duplicate submissions while a request is pending', async () => {
    let resolve;
    const ui=fixture(() => new Promise(done => {resolve=done;}));
    ui.submit(); ui.submit();
    assert.equal(ui.requests.length,1);
    assert.equal(ui.button.disabled,true);
    resolve({status:200,json:async () => ({success:true,count:1})});
    await settle();
    assert.equal(ui.button.disabled,false);
});

test('expired login redirects without displaying a fake cart count', async () => {
    const ui=fixture(async () => ({status:401}));
    ui.submit(); await settle();
    assert.equal(ui.window.location.href,'/login');
    assert.equal(ui.cart.badge.textContent,'0');
});

test('zero hides only the cart badge and malformed totals are ignored', async () => {
    for (const count of [0,-1,null,'3']) {
        const ui=fixture(() => response({success:true,count}),{count:2});
        ui.submit(); await settle();
        assert.equal(ui.cart.badge.style.display,count === 0 ? 'none' : '');
        assert.equal(ui.notification.textContent,'7');
    }
});
