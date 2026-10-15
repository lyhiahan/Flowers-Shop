const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { createGateway } = require('../../../scripts/demo-phone-gateway.cjs');
const token = 'a'.repeat(64);

test('gateway publishes only phone routes and preserves confirmation cookies and body', async t => {
    const requests = [];
    const app = http.createServer((req, res) => {
        const chunks = [];
        req.on('data', chunk => chunks.push(chunk));
        req.on('end', () => {
            requests.push({path:req.url, method:req.method, cookie:req.headers.cookie, body:Buffer.concat(chunks).toString()});
            if (req.method === 'POST') {
                res.writeHead(303, {Location:`http://127.0.0.1:${app.address().port}/demo-payment/${token}/success`});
                res.end(); return;
            }
            res.setHeader('Set-Cookie', 'JSESSIONID=test; Path=/; HttpOnly; SameSite=Lax');
            res.end('Payment page');
        });
    });
    await new Promise(resolve => app.listen(0, '127.0.0.1', resolve));
    const gateway = createGateway({appPort:app.address().port});
    await new Promise(resolve => gateway.listen(0, '127.0.0.1', resolve));
    t.after(() => { gateway.closeAllConnections(); gateway.close(); app.closeAllConnections(); app.close(); });
    const base = `http://127.0.0.1:${gateway.address().port}`;
    assert.match(await (await fetch(base + '/?utm_source=phone')).text(), /Kết nối thành công/);
    for (const path of ['/admin', '/login', '/customer/orders', `/customer/demo-payment/${token}`, '/api/auth/login', '/css/other.css', `/demo-payment/${token}/confirm`]) {
        assert.equal((await fetch(base + path)).status, 404, path);
    }
    assert.equal(requests.length, 0);
    const page = await fetch(`${base}/demo-payment/${token}`);
    assert.equal(page.status, 200);
    assert.match(page.headers.get('set-cookie'), /JSESSIONID=test/);
    const confirm = await fetch(`${base}/demo-payment/${token}/confirm`, {
        method:'POST', redirect:'manual', headers:{Cookie:'JSESSIONID=test', 'Content-Type':'application/x-www-form-urlencoded'}, body:'_csrf=test-token'
    });
    assert.equal(confirm.status, 303);
    assert.equal(confirm.headers.get('location'), `/demo-payment/${token}/success`);
    assert.deepEqual(requests[1], {path:`/demo-payment/${token}/confirm`,method:'POST',cookie:'JSESSIONID=test',body:'_csrf=test-token'});
    assert.equal((await fetch(`${base}/demo-payment/${token}/success`)).status, 200);
    assert.equal((await fetch(`${base}/css/demo-payment.css`)).status, 200);
    assert.equal((await fetch(`${base}/css/site-footer.css`)).status, 200);
    assert.equal((await fetch(`${base}/demo-payment/${token}`, {method:'POST'})).status, 404);
    assert.equal((await fetch(`${base}/demo-payment/${token}?redirect=/admin&amount=1`)).status, 200);
    assert.equal(requests.at(-1).path, `/demo-payment/${token}`);
    const malformedStatus = await new Promise((resolve, reject) => {
        const request = http.get({hostname:'127.0.0.1', port:gateway.address().port, path:`/admin/../demo-payment/${token}`}, response => {
            response.resume(); resolve(response.statusCode);
        });
        request.on('error', reject);
    });
    assert.equal(malformedStatus, 404);
});
