'use strict';
// An ephemeral fixture verifies the real tunnel without touching a database.
const http = require('node:http');
const https = require('node:https');
const path = require('node:path');
const assert = require('node:assert/strict');
const {randomBytes} = require('node:crypto');
const {createAutoTunnel} = require('./demo-phone-tunnel.cjs');

function request(url, {method = 'GET', headers = {}, body} = {}) {
    return new Promise((resolve, reject) => {
        const req = https.request(url, {method, headers, timeout:10000}, res => {
            let content = '';
            res.setEncoding('utf8'); res.on('data', part => { content += part; });
            res.on('end', () => resolve({status:res.statusCode, headers:res.headers, body:content}));
        });
        req.on('timeout', () => req.destroy(new Error('NETWORK_TIMEOUT')));
        req.on('error', reject); req.end(body);
    });
}

async function main() {
    const token = randomBytes(32).toString('hex');
    const csrf = randomBytes(16).toString('hex');
    const cookie = randomBytes(16).toString('hex');
    let confirmations = 0;
    const app = http.createServer((req, res) => {
        if (req.method === 'GET' && req.url === `/demo-payment/${token}`) {
            res.setHeader('Set-Cookie', `probe=${cookie}; HttpOnly; SameSite=Lax; Path=/`);
            res.writeHead(200); res.end(csrf); return;
        }
        if (req.method === 'POST' && req.url === `/demo-payment/${token}/confirm`) {
            let body = ''; req.on('data', part => { body += part; });
            req.on('end', () => {
                if (req.headers.cookie !== `probe=${cookie}` || body !== `_csrf=${csrf}`) {
                    res.writeHead(403); res.end(); return;
                }
                confirmations++; res.writeHead(200); res.end('fixture-confirmed');
            });
            return;
        }
        res.writeHead(404); res.end();
    });
    let tunnel, deadline;
    try {
        await new Promise((resolve, reject) => { app.once('error', reject); app.listen(0, '127.0.0.1', resolve); });
        const ready = new Promise((resolve, reject) => {
            deadline = setTimeout(() => reject(new Error('TUNNEL_NOT_READY')), 90000);
            tunnel = createAutoTunnel({appPort:app.address().port,
                cloudflared:path.resolve(process.env.CLOUDFLARED_PATH || '.tools/cloudflared.exe'),
                emit:event => {
                    console.log(`Tunnel: ${event.state}${event.code ? ' ' + event.code : ''}`);
                    if (event.state === 'READY') resolve(event.url);
                }});
        });
        await tunnel.start();
        const origin = await ready;
        clearTimeout(deadline);
        assert.equal((await request(origin + '/')).status, 200);
        for (const route of ['/admin', '/api/auth/me', '/customer/orders']) {
            assert.equal((await request(origin + route)).status, 404);
        }
        const page = await request(`${origin}/demo-payment/${token}`);
        assert.equal(page.status, 200); assert.equal(page.body, csrf);
        assert.match(page.headers['set-cookie'][0], /HttpOnly/);
        const confirmation = await request(`${origin}/demo-payment/${token}/confirm`, {
            method:'POST', headers:{Cookie:`probe=${cookie}`, 'Content-Type':'application/x-www-form-urlencoded'}, body:`_csrf=${csrf}`});
        assert.equal(confirmation.status, 200); assert.equal(confirmation.body, 'fixture-confirmed');
        assert.equal(confirmations, 1);
        console.log('PASS: live Cloudflare HTTPS, gateway isolation, payment-page cookie and confirmation body');
        console.log('Fixture only: no database, order, wallet or real payment was changed. A physical phone was not used.');
    } finally {
        clearTimeout(deadline);
        if (tunnel) await tunnel.stop();
        app.closeAllConnections();
        if (app.listening) await new Promise(resolve => app.close(resolve));
    }
}

main().catch(error => {
    console.error(`FAIL: live phone connectivity check (${error.code || error.message}). No database was used.`);
    process.exitCode = 1;
});
