'use strict';
const http = require('node:http');
const { Buffer } = require('node:buffer');

const assets = new Set(['/css/demo-payment.css', '/css/site-footer.css', '/js/demo-payment.js']);
const hopHeaders = new Set(['connection', 'keep-alive', 'proxy-authenticate', 'proxy-authorization', 'te', 'trailer', 'transfer-encoding', 'upgrade']);

function permitted(method, path) {
    if (method === 'GET' || method === 'HEAD')
        return assets.has(path) || /^\/demo-payment\/[0-9a-f]{64}(?:\/success)?$/.test(path);
    return method === 'POST' && /^\/demo-payment\/[0-9a-f]{64}\/confirm$/.test(path);
}

function headersWithoutHop(headers) {
    const excluded = new Set(hopHeaders);
    for (const header of (headers.connection || '').split(',')) excluded.add(header.trim().toLowerCase());
    return Object.fromEntries(Object.entries(headers).filter(([name]) => !excluded.has(name.toLowerCase())));
}

function createGateway({ appPort = 8080, healthId } = {}) {
    if (!Number.isInteger(appPort) || appPort < 1 || appPort > 65535) throw new Error('Invalid app port');
    const origin = `http://127.0.0.1:${appPort}`;
    const server = http.createServer((req, res) => {
        res.setHeader('Cache-Control', 'no-store');
        res.setHeader('Referrer-Policy', 'no-referrer');
        let path;
        try {
            const url = new URL(req.url, 'http://gateway.local');
            path = url.pathname;
            if (url.origin !== 'http://gateway.local' || (req.url !== path && !req.url.startsWith(path + '?')))
                throw new Error('Non-canonical path');
        } catch (_) { res.writeHead(404); res.end('Not found'); return; }
        if (req.method === 'GET' && path === '/') {
            if (healthId) res.setHeader('X-Demo-Gateway-Id', healthId);
            res.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'});
            res.end('<!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
                + '<title>Kết nối thanh toán</title><body style="margin:0;padding:36px 24px;font:18px/1.6 Arial,sans-serif;background:#fff;color:#26332d">'
                + '<p style="color:#a50064;font-weight:bold">MoMo giả lập</p><h1 style="font-size:28px;color:#16704d">Kết nối thành công</h1>'
                + '<p>Điện thoại đã kết nối được tới cổng thanh toán.</p><p>Quét mã QR mới trên trang đặt hàng để xác nhận giao dịch giả lập.</p>'
                + '<p style="font-size:14px;color:#647079">Không kết nối hoặc trừ tiền ví MoMo thật.</p></body></html>');
            return;
        }
        // Match the raw path so encoded separators/dot segments cannot expose other routes.
        if (!permitted(req.method, path)) { res.writeHead(404); res.end('Not found'); return; }
        let size = 0;
        const chunks = [];
        req.on('data', chunk => {
            size += chunk.length;
            if (size > 65536) {
                if (!res.headersSent) { res.writeHead(413); res.end('Request too large'); }
                return;
            }
            chunks.push(chunk);
        });
        req.on('end', () => {
            if (res.writableEnded) return;
            const body = Buffer.concat(chunks);
            const headers = headersWithoutHop(req.headers);
            headers.host = `127.0.0.1:${appPort}`;
            delete headers.forwarded;
            for (const name of Object.keys(headers)) if (name.startsWith('x-forwarded-')) delete headers[name];
            headers['content-length'] = String(body.length);
            // Tracking parameters are not part of a payment and never reach the application.
            const upstream = http.request({hostname:'127.0.0.1', port:appPort, path, method:req.method, headers}, response => {
                const resultHeaders = headersWithoutHop(response.headers);
                if (resultHeaders.location) {
                    try {
                        const location = new URL(resultHeaders.location, origin);
                        if (location.origin !== origin || !permitted('GET', location.pathname) || location.search || location.hash)
                            throw new Error('Unexpected redirect');
                        resultHeaders.location = location.pathname;
                    } catch (_) {
                        response.resume();
                        res.writeHead(502); res.end('Unexpected upstream redirect'); return;
                    }
                }
                res.writeHead(response.statusCode, resultHeaders);
                response.pipe(res);
                response.on('error', () => res.destroy());
            });
            upstream.setTimeout(15000, () => upstream.destroy());
            upstream.on('error', () => {
                if (!res.headersSent) { res.writeHead(502); res.end('Start the website before opening this payment page'); }
                else res.destroy();
            });
            res.on('close', () => upstream.destroy());
            upstream.end(body);
        });
        req.on('error', () => res.destroy());
    });
    server.requestTimeout = 15000;
    server.headersTimeout = 10000;
    server.on('upgrade', (_, socket) => socket.destroy());
    return server;
}

if (require.main === module) {
    const appPort = Number(process.env.DEMO_APP_PORT || 8080);
    const gatewayPort = Number(process.env.DEMO_GATEWAY_PORT || 8082);
    if (!Number.isInteger(gatewayPort) || gatewayPort < 1024 || gatewayPort > 65535) throw new Error('Invalid gateway port');
    const server = createGateway({appPort});
    server.on('error', error => { console.error(error.message); process.exitCode = 1; });
    server.listen(gatewayPort, '127.0.0.1', () => {
        console.log(`Demo phone gateway: http://127.0.0.1:${gatewayPort}`);
        console.log(`Website: http://127.0.0.1:${appPort}. Only phone payment pages are forwarded.`);
    });
}

module.exports = {createGateway};
