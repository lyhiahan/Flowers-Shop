'use strict';
const fs = require('node:fs');
const https = require('node:https');
const readline = require('node:readline');
const { spawn: spawnProcess } = require('node:child_process');
const { randomUUID } = require('node:crypto');
const { createGateway } = require('./demo-phone-gateway.cjs');

function tunnelUrl(line) {
    const match = line.match(/https:\/\/[a-z0-9]+(?:-[a-z0-9]+)*\.trycloudflare\.com(?=[\s|"']|$)/);
    return match ? match[0] : null;
}

function probeGateway(url, healthId) {
    return new Promise(resolve => {
        const request = https.get(url + '/', {timeout:5000}, response => {
            const healthy = response.statusCode === 200 && response.headers['x-demo-gateway-id'] === healthId;
            response.resume();
            resolve(healthy);
        });
        request.on('timeout', () => request.destroy());
        request.on('error', () => resolve(false));
    });
}

function createAutoTunnel({appPort, cloudflared, emit, spawn = spawnProcess, probe = probeGateway,
    retryDelayMs = 5000, healthIntervalMs = 10000, startupTimeoutMs = 60000,
    maxRetryDelayMs = 60000, rateLimitDelayMs = 60000}) {
    const healthId = randomUUID();
    const gateway = createGateway({appPort, healthId});
    let child, readers = [], retryTimer, healthTimer, startupTimer;
    let stopped = false, generation = 0, candidate, checking = false, failures = 0, lastState, wasReady = false, retries = 0;
    function report(state, code = '', url = null) {
        const value = JSON.stringify({state, code, url});
        if (value !== lastState) { lastState = value; emit(JSON.parse(value)); }
    }
    function clearTimers() {
        clearTimeout(retryTimer); clearTimeout(startupTimer); clearInterval(healthTimer);
    }
    function disposeChild() {
        for (const reader of readers) reader.close();
        readers = [];
        const owned = child;
        child = null;
        if (owned && owned.exitCode == null && owned.signalCode == null) owned.kill();
    }
    function restart(code) {
        if (stopped) return;
        generation++;
        clearTimers(); disposeChild(); candidate = null; checking = false;
        report('ERROR', code);
        const delay = Math.min(maxRetryDelayMs, retryDelayMs * 2 ** Math.min(retries++, 10));
        retryTimer = setTimeout(launch, code === 'RATE_LIMITED' ? Math.max(rateLimitDelayMs, delay) : delay);
    }
    async function check(current) {
        if (stopped || current !== generation || !candidate || checking) return;
        checking = true;
        const url = candidate;
        let healthy = false;
        try { healthy = await probe(url, healthId); } catch (_) {}
        if (stopped || current !== generation) return;
        checking = false;
        if (healthy) {
            wasReady = true; failures = 0; retries = 0; clearTimeout(startupTimer);
            report('READY', '', url);
        } else {
            failures++;
            report('STARTING', 'CHECKING_CONNECTION');
            // A new quick-tunnel hostname may take time to resolve. Keep it until
            // the startup deadline instead of continuously replacing its DNS name.
            if (wasReady && failures >= 3) restart('NETWORK_UNAVAILABLE');
        }
    }
    function launch() {
        if (stopped) return;
        clearTimers();
        const current = ++generation;
        candidate = null; checking = false; failures = 0; wasReady = false;
        report('STARTING', 'STARTING_TUNNEL');
        try {
            child = spawn(cloudflared, ['--no-autoupdate','tunnel','--protocol','http2','--url',
                `http://127.0.0.1:${gateway.address().port}`], {windowsHide:true, shell:false, stdio:['ignore','pipe','pipe']});
        } catch (_) { restart('CLOUDFLARED_START_FAILED'); return; }
        child.on('error', () => { if (current === generation) restart('CLOUDFLARED_START_FAILED'); });
        child.on('close', () => { if (current === generation) restart('CLOUDFLARED_EXITED'); });
        for (const stream of [child.stdout, child.stderr]) {
            const reader = readline.createInterface({input:stream});
            readers.push(reader);
            reader.on('line', line => {
                if (stopped || current !== generation) return;
                if (/quick tunnel provisioning failed with status 429|Too Many Requests/i.test(line)) {
                    restart('RATE_LIMITED'); return;
                }
                if (line.includes('Unauthorized: Tunnel not found')) { restart('TUNNEL_EXPIRED'); return; }
                const url = tunnelUrl(line);
                if (url) { candidate = url; check(current); }
                if (line.includes('Connection terminated')) report('STARTING', 'CHECKING_CONNECTION');
            });
        }
        startupTimer = setTimeout(() => { if (current === generation) restart('NETWORK_UNAVAILABLE'); }, startupTimeoutMs);
        healthTimer = setInterval(() => check(current), healthIntervalMs);
    }
    return {
        async start() {
            await new Promise((resolve, reject) => {
                gateway.once('error', reject);
                gateway.listen(0, '127.0.0.1', resolve);
            });
            launch();
        },
        async stop() {
            if (stopped) return;
            stopped = true; generation++; clearTimers(); disposeChild();
            gateway.closeAllConnections();
            if (gateway.listening) await new Promise(resolve => gateway.close(resolve));
        }
    };
}

if (require.main === module) {
    const {values} = require('node:util').parseArgs({options:{'app-port':{type:'string'},cloudflared:{type:'string'}}});
    const emit = value => process.stdout.write(JSON.stringify(value) + '\n');
    const appPort = Number(values['app-port']);
    if (!Number.isInteger(appPort) || appPort < 1 || appPort > 65535 || !values.cloudflared || !fs.existsSync(values.cloudflared)) {
        emit({state:'ERROR', code:'INVALID_CONFIGURATION', url:null});
        process.exitCode = 1;
    } else {
        const supervisor = createAutoTunnel({appPort, cloudflared:values.cloudflared, emit});
        let closing = false;
        const stop = async () => {
            if (closing) return;
            closing = true;
            await supervisor.stop();
            process.stdin.pause();
        };
        // Closing Java's pipe also cleans up the owned tunnel if the JVM is killed unexpectedly.
        process.stdin.on('end', stop);
        process.stdin.on('data', chunk => { if (chunk.toString().includes('stop')) stop(); });
        process.stdin.resume();
        process.on('SIGINT', stop); process.on('SIGTERM', stop);
        supervisor.start().catch(async () => {
            emit({state:'ERROR', code:'GATEWAY_START_FAILED', url:null});
            await stop(); process.exitCode = 1;
        });
    }
}

module.exports = {createAutoTunnel, tunnelUrl, probeGateway};
